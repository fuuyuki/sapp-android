package com.example.sapp.verification

import com.example.sapp.data.model.MedlogOut
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Selects the ESP32-created medlog that a camera proof belongs to.
 *
 * `scheduled_time` has no offset, so it is parsed as a LocalDateTime and
 * compared against "now" converted to Asia/Jakarta (WIB). The device default
 * zone is never consulted.
 */
object MedlogMatcher {
    val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
    const val WINDOW_MINUTES = 60L

    fun selectMedlog(
        logs: List<MedlogOut>,
        pillname: String,
        now: OffsetDateTime,
        windowMinutes: Long = WINDOW_MINUTES
    ): MedlogOut? {
        val wantedPill = pillname.trim()
        if (wantedPill.isEmpty()) return null

        val nowWib = now.atZoneSameInstant(WIB).toLocalDateTime()
        val maxDelta = Duration.ofMinutes(windowMinutes)

        return logs.asSequence()
            .filter { it.evidence_key.isNullOrEmpty() }
            .filter { it.pillname.trim().equals(wantedPill, ignoreCase = true) }
            .mapNotNull { log ->
                scheduledLocalDateTime(log.scheduled_time)?.let { scheduled -> scheduled to log }
            }
            .filter { (scheduled, _) ->
                absolute(Duration.between(scheduled, nowWib)) <= maxDelta
            }
            .maxByOrNull { it.first }
            ?.second
    }

    /** True when a raw scheduled_time falls in the +/- window around [now] in WIB. */
    fun isWithinWindow(
        raw: String?,
        now: OffsetDateTime,
        windowMinutes: Long = WINDOW_MINUTES
    ): Boolean {
        val scheduled = scheduledLocalDateTime(raw) ?: return false
        val nowWib = now.atZoneSameInstant(WIB).toLocalDateTime()
        return absolute(Duration.between(scheduled, nowWib)) <= Duration.ofMinutes(windowMinutes)
    }

    fun scheduledLocalDateTime(raw: String?): LocalDateTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: DateTimeParseException) {
            try {
                // Tolerate an offset if the server ever starts sending one.
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun absolute(duration: Duration): Duration =
        if (duration.isNegative) duration.negated() else duration
}
