package com.example.sapp.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MouthOpenTrackerTest {

    private val tracker = MouthOpenTracker()

    private fun openFrame(mar: Float = 0.50f) = FaceFrame(
        faceCount = 1,
        faceWidthFraction = 0.60f,
        yawDegrees = 0f,
        pitchDegrees = 0f,
        mouthAspectRatio = mar
    )

    @Test
    fun requiresEightConsecutiveQualifyingFrames() {
        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES - 1) {
            assertNull(tracker.onFrame(openFrame()))
        }

        val detection = tracker.onFrame(openFrame())
        assertNotNull(detection)
        // mean MAR = 0.50 -> 0.5 + (0.50 - 0.35) = 0.65
        assertEquals(0.65f, detection!!.confidence, 1e-5f)
        assertEquals(0.50f, detection.meanMar, 1e-5f)
    }

    @Test
    fun aClosedFrameResetsTheRun() {
        repeat(4) { assertNull(tracker.onFrame(openFrame())) }
        assertNull(tracker.onFrame(openFrame(mar = 0.10f)))

        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES - 1) {
            assertNull(tracker.onFrame(openFrame()))
        }
        assertNotNull(tracker.onFrame(openFrame()))
    }

    @Test
    fun oneTriggerConsumesTheRun() {
        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES) { tracker.onFrame(openFrame()) }
        assertNull(tracker.onFrame(openFrame()))
    }

    @Test
    fun confidenceIsAveragedAcrossTheWholeWindow() {
        repeat(3) { tracker.onFrame(openFrame(mar = 0.35f)) }
        repeat(4) { tracker.onFrame(openFrame(mar = 0.55f)) }

        val detection = tracker.onFrame(openFrame(mar = 0.45f))
        assertNotNull(detection)
        // The window is 3x0.35, 4x0.55, 0.45 -> mean 0.4625
        assertEquals(0.4625f, detection!!.meanMar, 1e-5f)
        assertEquals(0.6125f, detection.confidence, 1e-5f)
    }

    @Test
    fun smallFaceIsRejected() {
        val small = openFrame().copy(faceWidthFraction = MouthMetrics.MIN_FACE_WIDTH_FRACTION - 0.01f)
        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES + 2) {
            assertNull(tracker.onFrame(small))
        }
    }

    @Test
    fun offAxisFaceIsRejected() {
        val yawed = openFrame().copy(yawDegrees = MouthMetrics.MAX_POSE_DEGREES + 1f)
        val pitched = openFrame().copy(pitchDegrees = -MouthMetrics.MAX_POSE_DEGREES - 1f)
        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES + 2) {
            assertNull(tracker.onFrame(yawed))
            assertNull(tracker.onFrame(pitched))
        }
    }

    @Test
    fun moreThanOneFaceIsRejected() {
        val twoFaces = openFrame().copy(faceCount = 2)
        repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES + 2) {
            assertNull(tracker.onFrame(twoFaces))
        }
    }

    @Test
    fun sustainedDetectionConfidenceNeverExceedsItsBounds() {
        val detection = MouthOpenTracker().let { fresh ->
            repeat(MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES - 1) { fresh.onFrame(openFrame(mar = 2f)) }
            fresh.onFrame(openFrame(mar = 2f))
        }
        assertNotNull(detection)
        assertTrue(detection!!.confidence in 0f..1f)
    }
}
