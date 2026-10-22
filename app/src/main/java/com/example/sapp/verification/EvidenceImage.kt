package com.example.sapp.verification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/** Capture downscaling / JPEG encoding shared by the activity and the worker. */
object EvidenceImage {
    const val MAX_CAPTURE_DIMENSION = 1280
    const val CAPTURE_JPEG_QUALITY = 80
    const val SHRUNK_DIMENSION = 720
    const val SHRUNK_JPEG_QUALITY = 60

    fun saveCapture(context: Context, source: Bitmap): File {
        val directory = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val file = File(directory, "mouth-${UUID.randomUUID()}.jpg")
        val scaled = scaleToMaxDimension(source, MAX_CAPTURE_DIMENSION)
        try {
            FileOutputStream(file).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, CAPTURE_JPEG_QUALITY, output)
            }
        } finally {
            if (scaled !== source) scaled.recycle()
        }
        return file
    }

    /** Used once on HTTP 413 before giving up. Returns false if it cannot shrink. */
    fun shrinkForRetry(file: File): Boolean {
        if (!file.exists()) return false
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        return try {
            val scaled = scaleToMaxDimension(bitmap, SHRUNK_DIMENSION)
            try {
                FileOutputStream(file).use { output ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, SHRUNK_JPEG_QUALITY, output)
                }
                true
            } finally {
                if (scaled !== bitmap) scaled.recycle()
            }
        } catch (_: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    fun scaleToMaxDimension(source: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxDimension) return source
        val scale = maxDimension.toFloat() / longest
        val width = maxOf(1, (source.width * scale).roundToInt())
        val height = maxOf(1, (source.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    /** Best-effort cleanup of captures left behind by an abandoned scan. */
    fun deleteStaleCaptures(context: Context, olderThanMillis: Long) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        File(context.cacheDir, CACHE_DIR).listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    private const val CACHE_DIR = "evidence"
}
