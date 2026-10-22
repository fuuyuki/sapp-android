package com.example.sapp.verification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sapp.AppRepository
import com.example.sapp.data.network.RetrofitClient
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Uploads one already-captured JPEG. The work is idempotent (the endpoint is a
 * PUT), keyed by pillname + scheduled_time, and only runs with connectivity.
 */
class EvidenceUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val photoPath = inputData.getString(KEY_PHOTO_PATH) ?: return Result.failure()
        val medlogId = inputData.getString(KEY_MEDLOG_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return Result.failure()
        val confidence = inputData.getFloat(KEY_CONFIDENCE, 0f)
        val capturedAt = inputData.getString(KEY_CAPTURED_AT) ?: ""
        val method = inputData.getString(KEY_METHOD) ?: MouthMetrics.EVIDENCE_METHOD
        val file = File(photoPath)
        if (!file.exists()) return Result.failure()

        val uploader = EvidenceUploader(
            AppRepository(RetrofitClient.getApiService(applicationContext))
        )

        var outcome = uploader.upload(medlogId, file, confidence, capturedAt, method)
        if (outcome == UploadOutcome.TooLarge && shrinkOnce(file)) {
            outcome = uploader.upload(medlogId, file, confidence, capturedAt, method)
        }

        return when (outcome) {
            is UploadOutcome.Success -> {
                deleteCapture(file)
                Result.success()
            }
            UploadOutcome.Permanent, UploadOutcome.TooLarge -> {
                deleteCapture(file)
                Result.failure()
            }
            UploadOutcome.Retryable -> {
                if (runAttemptCount >= MAX_RUNS - 1) {
                    deleteCapture(file)
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }

    private fun shrinkOnce(file: File): Boolean {
        val marker = File("${file.absolutePath}.shrunk")
        if (marker.exists()) return false
        val shrunk = EvidenceImage.shrinkForRetry(file)
        if (shrunk) runCatching { marker.createNewFile() }
        return shrunk
    }

    private fun deleteCapture(file: File) {
        runCatching { file.delete() }
        runCatching { File("${file.absolutePath}.shrunk").delete() }
    }

    companion object {
        const val MAX_RUNS = 8

        const val KEY_MEDLOG_ID = "medlog_id"
        const val KEY_PHOTO_PATH = "photo_path"
        const val KEY_CONFIDENCE = "confidence"
        const val KEY_CAPTURED_AT = "captured_at"
        const val KEY_METHOD = "method"
    }
}

object EvidenceUploadScheduler {

    /**
     * Unique per (pillname, scheduled_time) so repeated taps while the same
     * dose is pending cannot enqueue duplicate uploads. The PUT is idempotent,
     * so an occasional re-run is harmless anyway.
     */
    fun uniqueWorkName(pillname: String, scheduledTime: String): String {
        val raw = "$pillname|$scheduledTime"
        val readable = raw.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(80)
            .ifEmpty { "dose" }
        return "evidence_${readable}_${Integer.toHexString(raw.hashCode())}"
    }

    fun enqueue(
        context: Context,
        medlogId: UUID,
        photo: File,
        confidence: Float,
        capturedAt: String,
        method: String,
        pillname: String,
        scheduledTime: String
    ): String {
        val workName = uniqueWorkName(pillname, scheduledTime)

        val data = Data.Builder()
            .putString(EvidenceUploadWorker.KEY_MEDLOG_ID, medlogId.toString())
            .putString(EvidenceUploadWorker.KEY_PHOTO_PATH, photo.absolutePath)
            .putFloat(EvidenceUploadWorker.KEY_CONFIDENCE, confidence)
            .putString(EvidenceUploadWorker.KEY_CAPTURED_AT, capturedAt)
            .putString(EvidenceUploadWorker.KEY_METHOD, method.take(30))
            .build()

        val request = OneTimeWorkRequestBuilder<EvidenceUploadWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,
            request
        )
        return workName
    }
}
