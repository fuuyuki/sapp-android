package com.example.sapp.verification

import com.example.sapp.AppRepository
import com.example.sapp.data.model.MedlogOut
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.util.UUID

sealed interface UploadOutcome {
    data class Success(val medlog: MedlogOut?) : UploadOutcome

    /** 404 (log not visible yet), 401, 408, 429, 5xx, or a transport failure. */
    data object Retryable : UploadOutcome

    /** 413: shrink once and try again. */
    data object TooLarge : UploadOutcome

    /** 400 / 403 / 415 / 422 and other non-retryable client errors. */
    data object Permanent : UploadOutcome
}

/**
 * One PUT to the existing evidence endpoint through the existing repository.
 * Retry/backoff policy lives in [EvidenceUploadWorker]; this class only maps
 * the HTTP result to an outcome.
 */
class EvidenceUploader(private val repository: AppRepository) {

    suspend fun upload(
        medlogId: UUID,
        photo: File,
        confidence: Float,
        capturedAt: String,
        method: String = MouthMetrics.EVIDENCE_METHOD
    ): UploadOutcome {
        if (!photo.exists() || photo.length() == 0L) return UploadOutcome.Permanent

        val textType = "text/plain".toMediaType()
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            photo.name,
            photo.asRequestBody("image/jpeg".toMediaType())
        )
        val confidenceBody = confidence.coerceIn(0f, 1f).toString().toRequestBody(textType)
        val capturedAtBody = capturedAt.toRequestBody(textType)
        val methodBody = method.take(30).toRequestBody(textType)

        return try {
            val response = repository.uploadMedlogEvidence(
                medlogId = medlogId,
                photo = photoPart,
                confidence = confidenceBody,
                capturedAt = capturedAtBody,
                method = methodBody
            )
            when {
                response.isSuccessful -> UploadOutcome.Success(response.body())
                response.code() == 413 -> UploadOutcome.TooLarge
                response.code() == 404 ||
                        response.code() == 401 ||
                        response.code() == 408 ||
                        response.code() == 429 ||
                        response.code() >= 500 -> UploadOutcome.Retryable

                else -> UploadOutcome.Permanent
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            UploadOutcome.Retryable
        } catch (_: Exception) {
            UploadOutcome.Retryable
        }
    }
}
