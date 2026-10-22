package com.example.sapp.verification

import com.example.sapp.data.model.MedlogOut
import com.example.sapp.data.model.ScheduleOut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

class DueDoseResolverTest {

    private val now = OffsetDateTime.parse("2026-10-22T08:00:00+07:00")
    private val localNow = LocalTime.of(8, 0)

    private fun schedule(pill: String, doseTime: String) = ScheduleOut(
        id = UUID.randomUUID(),
        pillname = pill,
        dose_time = doseTime,
        repeat_days = 127,
        patient_id = UUID.randomUUID(),
        device_id = "chip-1"
    )

    private fun log(pill: String, scheduled: String, evidence: String? = null) = MedlogOut(
        id = UUID.randomUUID(),
        pillname = pill,
        scheduled_time = scheduled,
        status = "taken",
        patient_id = UUID.randomUUID(),
        device_id = "chip-1",
        evidence_key = evidence
    )

    @Test
    fun parsesTheScheduleTimeFormatsUsedByTheApp() {
        assertEquals(LocalTime.of(8, 30), DueDoseResolver.parseDoseTime("08:30:00.000Z"))
        assertEquals(LocalTime.of(8, 30), DueDoseResolver.parseDoseTime("2026-10-22T08:30:00"))
        assertEquals(LocalTime.of(8, 30), DueDoseResolver.parseDoseTime("08:30"))
        assertNull(DueDoseResolver.parseDoseTime("not-a-time"))
        assertNull(DueDoseResolver.parseDoseTime(null))
    }

    @Test
    fun prefersARecentPillboxLogWithoutEvidence() {
        val result = DueDoseResolver.resolve(
            schedules = listOf(schedule("SchedulePill", "07:30:00.000Z")),
            logs = listOf(log("LogPill", "2026-10-22T07:55:00")),
            now = now,
            nowLocalTime = localNow
        )

        assertEquals("LogPill", result)
    }

    @Test
    fun ignoresVerifiedAndOutOfWindowLogs() {
        val result = DueDoseResolver.resolve(
            schedules = listOf(schedule("SchedulePill", "07:45:00.000Z")),
            logs = listOf(
                log("VerifiedPill", "2026-10-22T07:55:00", evidence = "evidence/x.jpg"),
                log("OldPill", "2026-10-22T05:00:00")
            ),
            now = now,
            nowLocalTime = localNow
        )

        assertEquals("SchedulePill", result)
    }

    @Test
    fun fallsBackToTheNearestScheduleAcrossMidnight() {
        val result = DueDoseResolver.resolve(
            schedules = listOf(
                schedule("LatePill", "23:55:00.000Z"),
                schedule("FarPill", "12:00:00.000Z")
            ),
            logs = emptyList(),
            now = now,
            nowLocalTime = LocalTime.of(0, 10)
        )

        assertEquals("LatePill", result)
    }

    @Test
    fun returnsNullWhenThereIsNothingToVerify() {
        assertNull(DueDoseResolver.resolve(emptyList(), emptyList(), now, localNow))
    }
}
