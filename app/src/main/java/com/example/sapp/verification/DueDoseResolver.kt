package com.example.sapp.verification

import com.example.sapp.data.model.MedlogOut
import com.example.sapp.data.model.ScheduleOut
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Picks the medication to verify from the existing due-dose screen.
 *
 * Preference order:
 * 1. The newest ESP32-created medlog without evidence inside the WIB window.
 * 2. The nearest scheduled medication by local time-of-day.
 *
 * Schedules store a wall-clock time (the app creates "HH:mm:00.000Z" without a
 * zone conversion), so schedules are compared against local time while medlogs
 * keep using [MedlogMatcher]'s explicit Asia/Jakarta handling.
 */
object DueDoseResolver {

    fun resolve(
        schedules: List<ScheduleOut>,
        logs: List<MedlogOut>,
        now: OffsetDateTime = OffsetDateTime.now(),
        nowLocalTime: LocalTime = LocalTime.now(),
        windowMinutes: Long = MedlogMatcher.WINDOW_MINUTES
    ): String? {
        val recentLog = logs.asSequence()
            .filter { it.evidence_key.isNullOrEmpty() }
            .filter { MedlogMatcher.isWithinWindow(it.scheduled_time, now, windowMinutes) }
            .maxByOrNull {
                MedlogMatcher.scheduledLocalDateTime(it.scheduled_time) ?: LocalDateTime.MIN
            }

        recentLog?.pillname?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        return schedules.asSequence()
            .mapNotNull { schedule ->
                val pill = schedule.pillname.trim()
                val time = parseDoseTime(schedule.dose_time) ?: return@mapNotNull null
                if (pill.isEmpty()) null else pill to minutesApart(time, nowLocalTime)
            }
            .minByOrNull { it.second }
            ?.first
    }

    /** Parses "HH:mm:00.000Z", "yyyy-MM-ddTHH:mm:ss" or "HH:mm". */
    fun parseDoseTime(raw: String?): LocalTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val timePart = if (value.contains('T')) value.substringAfter('T') else value
        val hhmm = timePart.split(':').take(2).joinToString(":")
        return runCatching {
            LocalTime.parse(hhmm, DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }

    private fun minutesApart(time: LocalTime, now: LocalTime): Long {
        val direct = Math.abs(Duration.between(time, now).toMinutes())
        val aroundMidnight = 24 * 60 - direct
        return minOf(direct, aroundMidnight)
    }
}
