package com.example.sapp.verification

import com.example.sapp.data.model.MedlogOut
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.util.TimeZone
import java.util.UUID

class MedlogMatcherTest {

    private val originalZone: TimeZone = TimeZone.getDefault()

    private val now = OffsetDateTime.parse("2026-10-22T08:01:12+07:00")

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun log(
        pillname: String,
        scheduledTime: String,
        evidenceKey: String? = null,
        id: UUID = UUID.randomUUID()
    ) = MedlogOut(
        id = id,
        pillname = pillname,
        scheduled_time = scheduledTime,
        status = "taken",
        patient_id = UUID.randomUUID(),
        device_id = "chip-1",
        evidence_key = evidenceKey
    )

    @Test
    fun matchesPillNameTrimmedAndCaseInsensitively() {
        val match = log("  Amlodipine  ", "2026-10-22T08:00:00")

        val selected = MedlogMatcher.selectMedlog(listOf(match), "amlodipine", now)

        assertEquals(match.id, selected?.id)
    }

    @Test
    fun picksTheMostRecentMatchInsideTheWindow() {
        val older = log("Amlodipine", "2026-10-22T07:30:00")
        val newer = log("Amlodipine", "2026-10-22T08:00:00")

        val selected = MedlogMatcher.selectMedlog(listOf(older, newer), "Amlodipine", now)

        assertEquals(newer.id, selected?.id)
    }

    @Test
    fun excludesLogsThatAlreadyHaveEvidence() {
        val verified = log("Amlodipine", "2026-10-22T08:00:00", evidenceKey = "evidence/abc.jpg")

        assertNull(MedlogMatcher.selectMedlog(listOf(verified), "Amlodipine", now))
    }

    @Test
    fun excludesLogsOutsideSixtyMinuteWindow() {
        val tooOld = log("Amlodipine", "2026-10-22T06:59:59")
        val tooNew = log("Amlodipine", "2026-10-22T09:01:13")

        assertNull(MedlogMatcher.selectMedlog(listOf(tooOld), "Amlodipine", now))
        assertNull(MedlogMatcher.selectMedlog(listOf(tooNew), "Amlodipine", now))
    }

    @Test
    fun windowBoundaryIsInclusive() {
        val exactlyOneHourAgo = log("Amlodipine", "2026-10-22T07:01:12")
        val exactlyOneHourAhead = log("Amlodipine", "2026-10-22T09:01:12")

        assertEquals(
            exactlyOneHourAgo.id,
            MedlogMatcher.selectMedlog(listOf(exactlyOneHourAgo), "Amlodipine", now)?.id
        )
        assertEquals(
            exactlyOneHourAhead.id,
            MedlogMatcher.selectMedlog(listOf(exactlyOneHourAhead), "Amlodipine", now)?.id
        )
    }

    @Test
    fun usesWibInsteadOfTheDeviceDefaultZone() {
        // 01:01:12 UTC is 08:01:12 in Asia/Jakarta.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val utcNow = OffsetDateTime.parse("2026-10-22T01:01:12Z")

        val wibMatch = log("Amlodipine", "2026-10-22T08:00:00")
        val timestampThatWouldMatchIfTreatedAsUtc = log("Amlodipine", "2026-10-22T01:00:00")

        assertEquals(wibMatch.id, MedlogMatcher.selectMedlog(listOf(wibMatch), "Amlodipine", utcNow)?.id)
        assertNull(
            MedlogMatcher.selectMedlog(
                listOf(timestampThatWouldMatchIfTreatedAsUtc),
                "Amlodipine",
                utcNow
            )
        )
    }

    @Test
    fun ignoresUnparseableScheduledTimes() {
        val broken = log("Amlodipine", "not-a-date")

        assertNull(MedlogMatcher.selectMedlog(listOf(broken), "Amlodipine", now))
    }

    @Test
    fun blankPillNameNeverMatches() {
        val candidate = log("Amlodipine", "2026-10-22T08:00:00")

        assertNull(MedlogMatcher.selectMedlog(listOf(candidate), "   ", now))
    }

    @Test
    fun toleratesAnEmptyEvidenceKey() {
        val emptyKey = log("Amlodipine", "2026-10-22T08:00:00", evidenceKey = "")

        assertEquals(
            emptyKey.id,
            MedlogMatcher.selectMedlog(listOf(emptyKey), "Amlodipine", now)?.id
        )
    }
}
