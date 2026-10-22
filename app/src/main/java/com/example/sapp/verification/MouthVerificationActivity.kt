package com.example.sapp.verification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.sapp.AppRepository
import com.example.sapp.R
import com.example.sapp.data.model.MedlogOut
import com.example.sapp.data.network.RetrofitClient
import com.example.sapp.ui.theme.MedicalAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera-based proof for a dose the ESP32 already reported.
 *
 * This is additive: it never creates a medlog and never changes `status`. It
 * only attaches one JPEG + confidence to an existing medlog via the existing
 * repository/Retrofit stack.
 */
class MouthVerificationActivity : AppCompatActivity() {

    private var uiState by mutableStateOf<VerificationUiState>(VerificationUiState.Idle)

    private lateinit var previewView: PreviewView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val repository by lazy { AppRepository(RetrofitClient.getApiService(applicationContext)) }
    private val cachedLogs: List<MedlogOut> by lazy {
        parseCachedLogs(intent.getStringExtra(EXTRA_MEDLOGS_JSON))
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startScanning()
        } else {
            uiState = VerificationUiState.Unavailable(
                getString(R.string.camera_permission_required),
                showAppSettings = true
            )
        }
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeAnalysis: ImageAnalysis? = null

    @Volatile
    private var scanning = false

    @Volatile
    private var faceLandmarker: FaceLandmarker? = null

    private val tracker = MouthOpenTracker()
    private val trackerLock = Any()
    private val triggered = AtomicBoolean(false)
    private val inFlightFrames = ConcurrentHashMap<Long, Bitmap>()
    private var lastTimestampMs = 0L

    private var pendingCaptureFile: File? = null
    private var pendingConfidence = 0f
    private var pendingCapturedAt: String? = null
    private var workObservationJob: Job? = null
    private var waitingJob: Job? = null

    private val pillName: String by lazy {
        intent.getStringExtra(EXTRA_PILLNAME).orEmpty()
    }
    private val userId: UUID? by lazy {
        intent.getStringExtra(EXTRA_USER_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        EvidenceImage.deleteStaleCaptures(this, STALE_CAPTURE_MILLIS)

        setContent {
            MedicalAppTheme {
                MouthVerificationScreen(
                    state = uiState,
                    previewView = previewView,
                    onStart = { ensurePermissionAndStart() },
                    onScanAgain = {
                        pendingCaptureFile?.delete()
                        pendingCaptureFile = null
                        pendingConfidence = 0f
                        pendingCapturedAt = null
                        triggered.set(false)
                        VerificationStore(this).resetCooldown()
                        ensurePermissionAndStart()
                    },
                    onCheckAgain = { checkAgain() },
                    onOpenSettings = { openAppSettings() },
                    onClose = { finish() }
                )
            }
        }
    }

    // ---------------------------------------------------------------- camera

    private fun ensurePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanning() {
        if (scanning) return
        triggered.set(false)
        synchronized(trackerLock) { tracker.reset() }

        val landmarker = try {
            createFaceLandmarker()
        } catch (gpuError: Exception) {
            Log.e(TAG, "CPU fallback also failed to create FaceLandmarker", gpuError)
            uiState = VerificationUiState.Unavailable(
                getString(R.string.mouth_detector_unavailable),
                showAppSettings = false
            )
            return
        }
        faceLandmarker = landmarker

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    releaseFaceLandmarker()
                    uiState = VerificationUiState.Unavailable(
                        getString(R.string.no_front_camera),
                        showAppSettings = false
                    )
                    return@addListener
                }
                bindCamera(provider)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start front camera", e)
                releaseFaceLandmarker()
                uiState = VerificationUiState.Unavailable(
                    getString(R.string.camera_start_failed),
                    showAppSettings = true
                )
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createFaceLandmarker(): FaceLandmarker {
        fun options(delegate: Delegate) = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .setDelegate(delegate)
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ -> onFaceLandmarkerResult(result) }
            .setErrorListener { error ->
                Log.e(TAG, "FaceLandmarker error", error)
            }
            .build()

        return try {
            FaceLandmarker.createFromOptions(this, options(Delegate.GPU))
        } catch (_: Exception) {
            // GPU delegate is not available on every device / emulator.
            FaceLandmarker.createFromOptions(this, options(Delegate.CPU))
        }
    }

    private fun bindCamera(provider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(cameraExecutor, ::analyzeFrame) }

        provider.unbindAll()
        provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)

        cameraProvider = provider
        activeAnalysis = analysis
        scanning = true
        uiState = VerificationUiState.Scanning
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (!scanning || triggered.get()) {
            imageProxy.close()
            return
        }

        var frameBitmap: Bitmap? = null
        try {
            val raw = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rotated = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                    .also { bitmap -> if (bitmap !== raw) raw.recycle() }
            } else {
                raw
            }
            frameBitmap = rotated

            val landmarker = faceLandmarker
            if (landmarker == null) {
                rotated.recycle()
                frameBitmap = null
                return
            }

            val timestampMs = nextTimestampMs(imageProxy.imageInfo.timestamp)
            inFlightFrames[timestampMs] = rotated
            trimInFlightFrames()

            try {
                landmarker.detectAsync(BitmapImageBuilder(rotated).build(), timestampMs)
            } catch (e: Exception) {
                // The outer catch recycles the bitmap; drop the map entry so it
                // is not retained until the camera is stopped.
                inFlightFrames.remove(timestampMs)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame analysis failed", e)
            frameBitmap?.recycle()
        } finally {
            imageProxy.close()
        }
    }

    /** Results are delivered in order, so the oldest in-flight frame is this one. */
    private fun takeOldestFrame(): Bitmap? {
        val oldestKey = inFlightFrames.keys.minOrNull() ?: return null
        return inFlightFrames.remove(oldestKey)
    }

    private fun trimInFlightFrames() {
        // Drop the entry, but do not recycle: MediaPipe may still be reading the
        // bitmap for a submitted frame. The GC reclaims it after the callback.
        while (inFlightFrames.size > MAX_IN_FLIGHT_FRAMES) {
            val oldestKey = inFlightFrames.keys.minOrNull() ?: return
            inFlightFrames.remove(oldestKey)
        }
    }

    private fun nextTimestampMs(nanos: Long): Long {
        var timestamp = if (nanos > 0) nanos / 1_000_000 else SystemClock.uptimeMillis()
        synchronized(this) {
            if (timestamp <= lastTimestampMs) timestamp = lastTimestampMs + 1
            lastTimestampMs = timestamp
        }
        return timestamp
    }

    private fun onFaceLandmarkerResult(result: FaceLandmarkerResult) {
        val frameBitmap = takeOldestFrame()
        try {
            if (!scanning || triggered.get()) return
            val faces: List<List<NormalizedLandmark>> = result.faceLandmarks()
            val landmarks = faces.firstOrNull()
            if (landmarks == null || landmarks.size <= MouthLandmarkIndex.MOUTH_RIGHT_CORNER) {
                synchronized(trackerLock) { tracker.reset() }
                return
            }
            val bitmap = frameBitmap ?: return
            val points = landmarks.map { LandmarkPoint(it.x(), it.y()) }
            val mar = MouthMetrics.mouthAspectRatio(points, bitmap.width, bitmap.height) ?: 0f

            val frame = FaceFrame(
                faceCount = faces.size,
                faceWidthFraction = MouthMetrics.faceWidthFraction(points),
                yawDegrees = MouthMetrics.yawDegrees(points),
                pitchDegrees = MouthMetrics.pitchDegrees(points),
                mouthAspectRatio = mar
            )

            val detection = synchronized(trackerLock) { tracker.onFrame(frame) } ?: return
            val capture = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
            handleDetection(capture, detection.confidence)
        } finally {
            frameBitmap?.recycle()
        }
    }

    private fun handleDetection(bitmap: Bitmap, confidence: Float) {
        if (!triggered.compareAndSet(false, true)) {
            bitmap.recycle()
            return
        }

        val now = System.currentTimeMillis()
        val store = VerificationStore(this)
        if (store.isInCooldown(now)) {
            bitmap.recycle()
            val minutes = (store.cooldownRemainingMillis(now) / 60_000L) + 1
            runOnUiThread {
                stopScanning()
                uiState = VerificationUiState.Failed(
                    getString(R.string.mouth_cooldown, minutes),
                    canRetry = true
                )
            }
            return
        }
        store.recordDetection(now)

        val capturedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val file = try {
            EvidenceImage.saveCapture(this, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Could not save capture", e)
            null
        } finally {
            bitmap.recycle()
        }

        runOnUiThread {
            stopScanning()
            if (file == null) {
                uiState = VerificationUiState.Failed(
                    getString(R.string.mouth_capture_failed),
                    canRetry = true
                )
                return@runOnUiThread
            }
            pendingCaptureFile = file
            pendingConfidence = confidence
            pendingCapturedAt = capturedAt
            uiState = VerificationUiState.Detected(confidence)
            resolveMedlogAndUpload(file, confidence, capturedAt)
        }
    }

    // ------------------------------------------------------------- matching

    private suspend fun fetchLogs(): List<MedlogOut> = withContext(Dispatchers.IO) {
        val id = userId ?: return@withContext cachedLogs
        try {
            repository.getMedlogsOrEmpty(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cachedLogs
        }
    }

    private fun resolveMedlogAndUpload(file: File, confidence: Float, capturedAt: String) {
        lifecycleScope.launch {
            uiState = VerificationUiState.Uploading
            val capturedAtTime = runCatching {
                OffsetDateTime.parse(capturedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }.getOrElse { OffsetDateTime.now() }
            val medlog = MedlogMatcher.selectMedlog(
                logs = fetchLogs(),
                pillname = pillName,
                now = capturedAtTime
            )

            if (medlog == null) {
                startWaitingForPillbox(file, confidence, capturedAt)
                return@launch
            }

            val store = VerificationStore(this@MouthVerificationActivity)
            if (store.isVerified(medlog.id)) {
                file.delete()
                pendingCaptureFile = null
                uiState = VerificationUiState.Success(confidence, alreadyVerified = true)
                return@launch
            }

            val workName = EvidenceUploadScheduler.enqueue(
                context = this@MouthVerificationActivity,
                medlogId = medlog.id,
                photo = file,
                confidence = confidence,
                capturedAt = capturedAt,
                method = MouthMetrics.EVIDENCE_METHOD,
                pillname = medlog.pillname,
                scheduledTime = medlog.scheduled_time
            )
            observeUpload(workName, medlog.id, confidence)
        }
    }

    private fun startWaitingForPillbox(file: File, confidence: Float, capturedAt: String) {
        uiState = VerificationUiState.WaitingForPillbox
        waitingJob?.cancel()
        waitingJob = lifecycleScope.launch {
            // The ESP32 usually reports within a few seconds. Poll briefly so
            // the user does not have to re-scan; the "Check again" button
            // remains available afterwards.
            repeat(WAITING_POLL_COUNT) {
                delay(WAITING_POLL_MILLIS)
                val capturedAtTime = runCatching {
                    OffsetDateTime.parse(capturedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                }.getOrElse { OffsetDateTime.now() }
                val medlog = MedlogMatcher.selectMedlog(
                    logs = fetchLogs(),
                    pillname = pillName,
                    now = capturedAtTime
                )
                if (medlog != null) {
                    resolveMatchedMedlog(medlog, file, confidence, capturedAt)
                    return@launch
                }
            }
        }
    }

    private fun resolveMatchedMedlog(
        medlog: MedlogOut,
        file: File,
        confidence: Float,
        capturedAt: String
    ) {
        val store = VerificationStore(this)
        if (store.isVerified(medlog.id)) {
            file.delete()
            pendingCaptureFile = null
            uiState = VerificationUiState.Success(confidence, alreadyVerified = true)
            return
        }
        val workName = EvidenceUploadScheduler.enqueue(
            context = this,
            medlogId = medlog.id,
            photo = file,
            confidence = confidence,
            capturedAt = capturedAt,
            method = MouthMetrics.EVIDENCE_METHOD,
            pillname = medlog.pillname,
            scheduledTime = medlog.scheduled_time
        )
        observeUpload(workName, medlog.id, confidence)
    }

    private fun checkAgain() {
        val file = pendingCaptureFile
        if (file == null || !file.exists()) {
            ensurePermissionAndStart()
            return
        }
        waitingJob?.cancel()
        resolveMedlogAndUpload(file, pendingConfidence, pendingCapturedAt ?: return)
    }

    private fun observeUpload(workName: String, medlogId: UUID, confidence: Float) {
        workObservationJob?.cancel()
        uiState = VerificationUiState.Uploading
        workObservationJob = lifecycleScope.launch {
            WorkManager.getInstance(this@MouthVerificationActivity)
                .getWorkInfosForUniqueWorkFlow(workName)
                .collect { infos ->
                    val info = infos.lastOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            VerificationStore(this@MouthVerificationActivity).markVerified(medlogId)
                            pendingCaptureFile?.delete()
                            pendingCaptureFile = null
                            uiState = VerificationUiState.Success(confidence)
                        }

                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            pendingCaptureFile = null
                            uiState = VerificationUiState.Failed(
                                getString(R.string.mouth_upload_failed),
                                canRetry = true
                            )
                        }

                        WorkInfo.State.ENQUEUED -> {
                            uiState = VerificationUiState.Queued
                        }

                        else -> {
                            uiState = VerificationUiState.Uploading
                        }
                    }
                }
        }
    }

    // ------------------------------------------------------------- lifecycle

    private fun stopScanning() {
        scanning = false
        runCatching { cameraProvider?.unbindAll() }
        runCatching { activeAnalysis?.clearAnalyzer() }
        activeAnalysis = null
        releaseFaceLandmarker()

        // Do not recycle here: a NativeMPImage/detectAsync call may still hold
        // the bitmap until the FaceLandmarker is fully released. Dropping the
        // references lets the GC reclaim them safely.
        inFlightFrames.clear()
        synchronized(trackerLock) { tracker.reset() }
    }

    private fun releaseFaceLandmarker() {
        runCatching { faceLandmarker?.close() }
        faceLandmarker = null
    }

    override fun onStop() {
        super.onStop()
        val wasScanning = scanning
        stopScanning()
        if (wasScanning) uiState = VerificationUiState.Idle
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        cameraExecutor.shutdown()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    private fun parseCachedLogs(json: String?): List<MedlogOut> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            Gson().fromJson<List<MedlogOut>>(
                json,
                object : TypeToken<List<MedlogOut>>() {}.type
            )
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "MouthVerification"
        private const val MODEL_ASSET = "face_landmarker.task"
        private const val MAX_IN_FLIGHT_FRAMES = 4
        private const val STALE_CAPTURE_MILLIS = 24 * 60 * 60 * 1000L
        private const val WAITING_POLL_COUNT = 12
        private const val WAITING_POLL_MILLIS = 10_000L

        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_PILLNAME = "extra_pillname"
        const val EXTRA_MEDLOGS_JSON = "extra_medlogs_json"

        fun intent(
            context: Context,
            userId: UUID,
            pillname: String,
            logs: List<MedlogOut>
        ): Intent = Intent(context, MouthVerificationActivity::class.java)
            .putExtra(EXTRA_USER_ID, userId.toString())
            .putExtra(EXTRA_PILLNAME, pillname)
            .putExtra(EXTRA_MEDLOGS_JSON, Gson().toJson(logs))
    }
}

sealed interface VerificationUiState {
    data object Idle : VerificationUiState
    data object Scanning : VerificationUiState
    data class Detected(val confidence: Float) : VerificationUiState
    data object Uploading : VerificationUiState
    data object Queued : VerificationUiState
    data class Success(
        val confidence: Float,
        val alreadyVerified: Boolean = false
    ) : VerificationUiState

    data object WaitingForPillbox : VerificationUiState
    data class Failed(val message: String, val canRetry: Boolean = true) : VerificationUiState
    data class Unavailable(
        val message: String,
        val showAppSettings: Boolean = false
    ) : VerificationUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MouthVerificationScreen(
    state: VerificationUiState,
    previewView: PreviewView,
    onStart: () -> Unit,
    onScanAgain: () -> Unit,
    onCheckAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.mouth_verification_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (state is VerificationUiState.Scanning) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mouth_scanning_hint),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                StatePanel(
                    state = state,
                    onStart = onStart,
                    onScanAgain = onScanAgain,
                    onCheckAgain = onCheckAgain,
                    onOpenSettings = onOpenSettings,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun StatePanel(
    state: VerificationUiState,
    onStart: () -> Unit,
    onScanAgain: () -> Unit,
    onCheckAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            VerificationUiState.Idle -> {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.mouth_verification_idle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onStart) {
                    Text(stringResource(R.string.start_scanning))
                }
            }

            is VerificationUiState.Detected, VerificationUiState.Uploading -> {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.mouth_uploading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            VerificationUiState.Queued -> {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.mouth_queued),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close), color = Color.White)
                }
            }

            is VerificationUiState.Success -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (state.alreadyVerified) {
                        stringResource(R.string.mouth_already_verified)
                    } else {
                        stringResource(R.string.mouth_uploaded)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (!state.alreadyVerified) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.mouth_confidence,
                            (state.confidence * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onClose) {
                    Text(stringResource(R.string.close))
                }
            }

            VerificationUiState.WaitingForPillbox -> {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.mouth_waiting_pillbox),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onCheckAgain) {
                    Text(stringResource(R.string.check_again))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close), color = Color.White)
                }
            }

            is VerificationUiState.Failed -> {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                if (state.canRetry) {
                    Button(onClick = onScanAgain) {
                        Text(stringResource(R.string.scan_again))
                    }
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close), color = Color.White)
                }
            }

            is VerificationUiState.Unavailable -> {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                if (state.showAppSettings) {
                    Button(onClick = onOpenSettings) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close), color = Color.White)
                }
            }

            VerificationUiState.Scanning -> {
                // Rendered as the live preview branch above.
            }
        }
    }
}
