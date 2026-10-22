package com.example.sapp.verification

import android.content.Context
import java.util.UUID

/**
 * Small local ledger for the camera-proof debounce:
 * - the timestamp of the last accepted detection (5 minute cooldown)
 * - the set of medlog IDs that already received a verified upload
 */
class VerificationStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastDetectionAtMillis(): Long = prefs.getLong(KEY_LAST_DETECTION, 0L)

    fun recordDetection(atMillis: Long) {
        prefs.edit().putLong(KEY_LAST_DETECTION, atMillis).apply()
    }

    fun resetCooldown() {
        prefs.edit().remove(KEY_LAST_DETECTION).apply()
    }

    fun isInCooldown(nowMillis: Long): Boolean =
        nowMillis - lastDetectionAtMillis() < MouthMetrics.CONFIRMATION_COOLDOWN_MILLIS

    fun cooldownRemainingMillis(nowMillis: Long): Long =
        (MouthMetrics.CONFIRMATION_COOLDOWN_MILLIS - (nowMillis - lastDetectionAtMillis()))
            .coerceAtLeast(0L)

    fun isVerified(medlogId: UUID): Boolean = verifiedIds().contains(medlogId.toString())

    fun markVerified(medlogId: UUID) {
        val updated = HashSet(verifiedIds())
        updated.add(medlogId.toString())
        prefs.edit().putStringSet(KEY_VERIFIED, updated).apply()
    }

    private fun verifiedIds(): Set<String> =
        prefs.getStringSet(KEY_VERIFIED, emptySet()) ?: emptySet()

    private companion object {
        const val PREFS_NAME = "mouth_verification"
        const val KEY_LAST_DETECTION = "last_detection_at"
        const val KEY_VERIFIED = "verified_medlogs"
    }
}
