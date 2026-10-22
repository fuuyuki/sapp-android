package com.example.sapp.verification

import com.example.sapp.ApiService
import com.example.sapp.AppRepository
import com.example.sapp.data.network.AuthInterceptor
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.UUID

class EvidenceUploadApiTest {

    private val medlogId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private val medlogJson = """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "pillname": "Amlodipine",
          "scheduled_time": "2026-10-22T08:00:00",
          "status": "taken",
          "patient_id": "22222222-2222-2222-2222-222222222222",
          "device_id": "chip-1",
          "verification_method": "camera_mouth_open",
          "verification_confidence": 0.91,
          "verified_at": "2026-10-22T08:01:12+07:00",
          "evidence_key": "evidence/abc.jpg"
        }
    """.trimIndent()

    private fun api(server: MockWebServer, token: String? = "test-token"): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
            .build()
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun temporaryJpeg(): File =
        File.createTempFile("mouth-evidence", ".jpg").apply {
            writeBytes(
                byteArrayOf(
                    0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                    0xFF.toByte(), 0xD9.toByte()
                )
            )
        }

    @Test
    fun uploadSendsPhotoConfidenceCapturedAtMethodAndAuth_thenRetries404To200() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(200).setBody(medlogJson))
        server.start()
        val file = temporaryJpeg()
        try {
            val uploader = EvidenceUploader(AppRepository(api(server)))

            // First attempt: the medlog is not visible yet -> retryable.
            val first = uploader.upload(
                medlogId = medlogId,
                photo = file,
                confidence = 0.91f,
                capturedAt = "2026-10-22T08:01:12+07:00"
            )
            assertTrue(first is UploadOutcome.Retryable)

            // WorkManager's retry eventually succeeds.
            val second = uploader.upload(
                medlogId = medlogId,
                photo = file,
                confidence = 0.91f,
                capturedAt = "2026-10-22T08:01:12+07:00"
            )
            assertTrue(second is UploadOutcome.Success)
            val uploaded = (second as UploadOutcome.Success).medlog
            assertEquals("taken", uploaded?.status)
            assertEquals("evidence/abc.jpg", uploaded?.evidence_key)
            assertEquals(0.91, uploaded?.verification_confidence ?: 0.0, 1e-9)

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/medlogs/$medlogId/evidence", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))

            val body = request.body.readUtf8()
            assertTrue("photo part is required", body.contains("name=\"photo\""))
            assertTrue("photo must carry a filename", body.contains("filename=\""))
            assertTrue(body.contains("image/jpeg"))
            assertTrue("confidence field is required", body.contains("name=\"confidence\""))
            assertTrue(body.contains("0.91"))
            assertTrue(body.contains("name=\"captured_at\""))
            assertTrue(body.contains("2026-10-22T08:01:12+07:00"))
            assertTrue(body.contains("name=\"method\""))
            assertTrue(body.contains("camera_mouth_open"))

            val retry = server.takeRequest()
            assertEquals("Bearer test-token", retry.getHeader("Authorization"))
        } finally {
            file.delete()
            server.shutdown()
        }
    }

    @Test
    fun medlogList404MeansNoLogsInsteadOfAFailure() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(404))
        server.start()
        try {
            val repository = AppRepository(api(server))
            val logs = repository.getMedlogsOrEmpty(UUID.randomUUID())
            assertTrue(logs.isEmpty())

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertTrue(request.path?.startsWith("/medlogs/") == true)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun errorCodesMapToUploadOutcomes() = runBlocking {
        val server = MockWebServer()
        server.start()
        val file = temporaryJpeg()
        try {
            val uploader = EvidenceUploader(AppRepository(api(server)))
            val cases = listOf(
                403 to UploadOutcome.Permanent,
                413 to UploadOutcome.TooLarge,
                415 to UploadOutcome.Permanent,
                422 to UploadOutcome.Permanent,
                500 to UploadOutcome.Retryable,
                404 to UploadOutcome.Retryable
            )
            cases.forEach { (code, expected) ->
                server.enqueue(MockResponse().setResponseCode(code))
                val outcome = uploader.upload(
                    medlogId = medlogId,
                    photo = file,
                    confidence = 0.8f,
                    capturedAt = "2026-10-22T08:01:12+07:00"
                )
                assertEquals("HTTP $code", expected, outcome)
            }
        } finally {
            file.delete()
            server.shutdown()
        }
    }
}
