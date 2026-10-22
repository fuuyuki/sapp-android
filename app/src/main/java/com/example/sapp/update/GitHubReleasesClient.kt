package com.example.sapp.update

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Metadata published by .github/workflows/release.yml as `latest.json`.
 * Nullable fields make a malformed release fail validation instead of
 * crashing Gson.
 */
data class LatestRelease(
    val versionName: String? = null,
    val versionCode: Int? = null,
    val tag: String? = null,
    val apk: String? = null,
    val sha256: String? = null,
    val publishedAt: String? = null
)

sealed interface ReleaseFetchResult {
    data class Success(val release: LatestRelease) : ReleaseFetchResult
    data class Error(val message: String, val httpCode: Int? = null) : ReleaseFetchResult
}

/**
 * Reads the stable `latest.json` asset from GitHub Releases:
 *
 * https://github.com/<owner>/<repo>/releases/latest/download/latest.json
 *
 * This avoids the GitHub REST API rate limit and never sends the patient JWT
 * to GitHub. The downloaded APK is checked against the SHA-256 in this file.
 */
class GitHubReleasesClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val owner: String = OWNER,
    private val repo: String = REPO
) {

    fun latestJsonUrl(): String =
        "https://github.com/$owner/$repo/releases/latest/download/latest.json"

    fun apkUrl(apkName: String): String =
        "https://github.com/$owner/$repo/releases/latest/download/$apkName"

    suspend fun fetchLatest(): ReleaseFetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestJsonUrl())
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ReleaseFetchResult.Error(
                        message = "GitHub returned HTTP ${response.code}.",
                        httpCode = response.code
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext ReleaseFetchResult.Error("Empty release metadata.")
                val release = parseReleaseMetadata(body)
                    ?: return@withContext ReleaseFetchResult.Error("Invalid release metadata.")

                val apkName = release.apk
                if (release.versionName.isNullOrBlank() ||
                    release.versionCode == null ||
                    !isSafeAssetName(apkName) ||
                    release.sha256.isNullOrBlank()
                ) {
                    return@withContext ReleaseFetchResult.Error(
                        "The release metadata is incomplete."
                    )
                }
                ReleaseFetchResult.Success(release)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReleaseFetchResult.Error(e.message ?: "Could not reach GitHub.")
        }
    }

    /** Downloads the APK to [destination], reporting progress periodically. */
    suspend fun downloadApk(
        release: LatestRelease,
        destination: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val apkName = release.apk
        if (!isSafeAssetName(apkName)) throw IOException("Invalid APK asset name.")
        val request = Request.Builder()
            .url(apkUrl(apkName!!))
            .header("User-Agent", USER_AGENT)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed (HTTP ${response.code}).")
            }
            val body = response.body ?: throw IOException("Empty download response.")
            val total = body.contentLength()
            destination.parentFile?.mkdirs()

            try {
                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        var lastReported = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReported >= PROGRESS_STEP_BYTES ||
                                (total > 0 && downloaded == total)
                            ) {
                                onProgress(downloaded, total)
                                lastReported = downloaded
                            }
                        }
                        output.flush()
                        onProgress(downloaded, total)
                    }
                }
            } catch (e: Exception) {
                destination.delete()
                throw e
            }
        }
        destination
    }

    companion object {
        const val OWNER = "fuuyuki"
        const val REPO = "sapp-android"
        private const val USER_AGENT = "SApp-Android-Updater"
        private const val PROGRESS_STEP_BYTES = 256L * 1024L
        private val SAFE_ASSET_NAME = Regex("[A-Za-z0-9._-]+")

        fun parseReleaseMetadata(body: String): LatestRelease? =
            runCatching { Gson().fromJson(body, LatestRelease::class.java) }.getOrNull()

        fun isSafeAssetName(name: String?): Boolean =
            !name.isNullOrBlank() && SAFE_ASSET_NAME.matches(name)
    }
}

object UpdateVersion {
    fun isNewer(remoteVersionCode: Int?, currentVersionCode: Int): Boolean =
        remoteVersionCode != null && remoteVersionCode > currentVersionCode
}

object Sha256 {
    fun of(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val bytes = digest.digest()
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            if (value < 0x10) result.append('0')
            result.append(value.toString(16))
        }
        return result.toString()
    }
}
