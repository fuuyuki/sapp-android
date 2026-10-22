package com.example.sapp.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MouthMetricsTest {

    @Test
    fun marScalesNormalizedLandmarksToImageSize() {
        // y span: 0.10 * 480 = 48 px; x span: 0.20 * 640 = 128 px
        val mar = MouthMetrics.mouthAspectRatio(
            innerLipTopY = 0.40f,
            innerLipBottomY = 0.50f,
            leftCornerX = 0.35f,
            rightCornerX = 0.55f,
            imageWidth = 640,
            imageHeight = 480
        )

        assertEquals(48f / 128f, mar, 1e-5f)
        assertEquals(0.375f, mar, 1e-5f)
    }

    @Test
    fun marUsesEpsilonWhenMouthCornersCoincide() {
        val mar = MouthMetrics.mouthAspectRatio(
            innerLipTopY = 0.40f,
            innerLipBottomY = 0.50f,
            leftCornerX = 0.50f,
            rightCornerX = 0.50f,
            imageWidth = 640,
            imageHeight = 480
        )

        assertTrue(mar.isFinite())
        assertTrue(mar > 1_000f)
    }

    @Test
    fun marReadsTheRequiredLandmarkIndices() {
        val points = MutableList(292) { LandmarkPoint(0f, 0f) }
        points[MouthLandmarkIndex.INNER_LIP_TOP] = LandmarkPoint(0.5f, 0.40f)
        points[MouthLandmarkIndex.INNER_LIP_BOTTOM] = LandmarkPoint(0.5f, 0.50f)
        points[MouthLandmarkIndex.MOUTH_LEFT_CORNER] = LandmarkPoint(0.35f, 0.45f)
        points[MouthLandmarkIndex.MOUTH_RIGHT_CORNER] = LandmarkPoint(0.55f, 0.45f)

        val mar = MouthMetrics.mouthAspectRatio(points, imageWidth = 640, imageHeight = 480)

        assertEquals(0.375f, mar ?: 0f, 1e-5f)
    }

    @Test
    fun confidenceHasDocumentedBoundsAndNeutralCentre() {
        assertEquals(0.50f, MouthMetrics.confidence(0.35f), 1e-6f)
        assertEquals(0.55f, MouthMetrics.confidence(0.40f), 1e-6f)
        assertEquals(0f, MouthMetrics.confidence(-100f), 1e-6f)
        assertEquals(1f, MouthMetrics.confidence(100f), 1e-6f)

        var mean = 0f
        while (mean <= 2f) {
            val confidence = MouthMetrics.confidence(mean)
            assertTrue(confidence in 0f..1f)
            mean += 0.01f
        }
    }

    @Test
    fun frontalFaceHasNeutralPoseAndFullFaceWidth() {
        val points = MutableList(478) { LandmarkPoint(0.5f, 0.5f) }
        points[MouthLandmarkIndex.FACE_LEFT] = LandmarkPoint(0.2f, 0.5f)
        points[MouthLandmarkIndex.FACE_RIGHT] = LandmarkPoint(0.8f, 0.5f)
        points[MouthLandmarkIndex.NOSE_TIP] = LandmarkPoint(0.5f, 0.45f)
        points[MouthLandmarkIndex.LEFT_EYE_OUTER] = LandmarkPoint(0.35f, 0.3f)
        points[MouthLandmarkIndex.RIGHT_EYE_OUTER] = LandmarkPoint(0.65f, 0.3f)
        points[MouthLandmarkIndex.MOUTH_LEFT_CORNER] = LandmarkPoint(0.4f, 0.6f)
        points[MouthLandmarkIndex.MOUTH_RIGHT_CORNER] = LandmarkPoint(0.6f, 0.6f)

        assertEquals(0f, MouthMetrics.yawDegrees(points), 1e-4f)
        assertEquals(0f, MouthMetrics.pitchDegrees(points), 1e-4f)
        assertEquals(0.6f, MouthMetrics.faceWidthFraction(points), 1e-5f)
    }
}
