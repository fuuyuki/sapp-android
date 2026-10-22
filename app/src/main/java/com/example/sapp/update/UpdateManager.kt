package com.example.sapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.sapp.BuildConfig
import com.example.sapp.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val versionName: String) : UpdateState
    data class Available(val release: LatestRelease, val currentVersionName: String) : UpdateState
    data class Downloading(val downloaded: Long, val total: Long) : UpdateState {
        val progress: Float
            get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    data class ReadyToInstall(val release: LatestRelease, val apkFile: File) : UpdateState
    data class Failed(val message: String, val canRetry: Boolean = true) : UpdateState
}

/**
 * GitHub-only self-updater.
 *
 * Android does not allow silent installation for normal apps, so the final step
 * always opens the system package installer and the user confirms. Everything
 * before that (check, download, SHA-256 verification) is automatic.
 */
class UpdateManager(
    context: Context,
    private val scope: CoroutineScope,
    private val client: GitHubReleasesClient = GitHubReleasesClient()
) {
    private val appContext = context.applicationContext
    private val activityContext = context

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    fun checkForUpdates() {
        job?.cancel()
        _state.value = UpdateState.Checking
        job = scope.launch {
            when (val result = client.fetchLatest()) {
                is ReleaseFetchResult.Success -> {
                    val release = result.release
                    if (UpdateVersion.isNewer(release.versionCode, BuildConfig.VERSION_CODE)) {
                        _state.value = UpdateState.Available(release, BuildConfig.VERSION_NAME)
                    } else {
                        _state.value = UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                    }
                }

                is ReleaseFetchResult.Error -> {
                    val messageRes = if (result.httpCode == 404) {
                        R.string.update_error_no_release
                    } else {
                        R.string.update_error_check
                    }
                    _state.value = UpdateState.Failed(appContext.getString(messageRes))
                }
            }
        }
    }

    fun download() {
        val available = _state.value as? UpdateState.Available ?: return
        job?.cancel()
        // Set this before launching so a rapid double-tap cannot start two
        // downloads writing to the same file.
        _state.value = UpdateState.Downloading(0, 0)
        job = scope.launch {
            val release = available.release
            val apkName = release.apk ?: return@launch
            val updatesDir = File(appContext.filesDir, UPDATES_DIR).apply { mkdirs() }
            val target = File(updatesDir, apkName)
            updatesDir.listFiles()?.forEach { if (it != target) it.delete() }

            try {
                client.downloadApk(release, target) { downloaded, total ->
                    _state.value = UpdateState.Downloading(downloaded, total)
                }
                val expected = release.sha256
                if (expected.isNullOrBlank() ||
                    !expected.equals(Sha256.of(target), ignoreCase = true)
                ) {
                    target.delete()
                    _state.value = UpdateState.Failed(
                        appContext.getString(R.string.update_error_checksum)
                    )
                    return@launch
                }
                _state.value = UpdateState.ReadyToInstall(release, target)
            } catch (e: CancellationException) {
                target.delete()
                throw e
            } catch (_: Exception) {
                target.delete()
                _state.value = UpdateState.Failed(
                    appContext.getString(R.string.update_error_download)
                )
            }
        }
    }

    fun install() {
        val ready = _state.value as? UpdateState.ReadyToInstall ?: return
        val apk = ready.apkFile
        if (!apk.exists()) {
            _state.value = UpdateState.Failed(
                appContext.getString(R.string.update_error_missing_file)
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = runCatching {
                activityContext.packageManager.canRequestPackageInstalls()
            }.getOrDefault(true)
            if (!canInstall) {
                Toast.makeText(
                    activityContext,
                    activityContext.getString(R.string.update_error_unknown_sources),
                    Toast.LENGTH_LONG
                ).show()
                openUnknownSourcesSettings()
                return
            }
        }

        val uri = FileProvider.getUriForFile(
            activityContext,
            "${activityContext.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { activityContext.startActivity(intent) }
            .onFailure {
                _state.value = UpdateState.Failed(
                    appContext.getString(R.string.update_error_install)
                )
            }
    }

    fun dismiss() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    fun cancelDownload() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    private fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${activityContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { activityContext.startActivity(intent) }
            .onFailure {
                Toast.makeText(
                    activityContext,
                    activityContext.getString(R.string.update_error_unknown_sources),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private companion object {
        const val UPDATES_DIR = "updates"
    }
}
