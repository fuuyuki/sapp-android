package com.example.sapp.verification

import kotlin.math.abs
import kotlin.math.asin

/**
 * Landmark indices used by the mouth-open heuristic. The indices follow the
 * MediaPipe Face Mesh / FaceLandmarker topology.
 */
object MouthLandmarkIndex {
    const val INNER_LIP_TOP = 13
    const val INNER_LIP_BOTTOM = 14
    const val MOUTH_LEFT_CORNER = 61
    const val MOUTH_RIGHT_CORNER = 291

    // Pose helper landmarks (not part of the MAR formula).
    const val NOSE_TIP = 1
    const val FACE_LEFT = 234
    const val FACE_RIGHT = 454
    const val LEFT_EYE_OUTER = 33
    const val RIGHT_EYE_OUTER = 263
}

/** A normalized landmark: x and y are both in 0..1 image coordinates. */
data class LandmarkPoint(val x: Float, val y: Float)

/**
 * Mouth Aspect Ratio and pose helpers.
 *
 * MAR_THRESHOLD = 0.35 was chosen for a frontal face: with a typical mouth
 * width of ~0.20 of the frame (128 px at 640x480) a relaxed closed mouth stays
 * below ~0.10, a normal talking mouth is ~0.15-0.25, and a deliberately open
 * mouth is ~0.35-0.65. 0.35 keeps false positives from talking low while still
 * triggering on an ordinary "open your mouth" gesture. It is a named constant
 * so it can be tuned from field data.
 */
object MouthMetrics {
    const val MAR_THRESHOLD = 0.35f
    const val REQUIRED_CONSECUTIVE_FRAMES = 8
    const val MIN_FACE_WIDTH_FRACTION = 0.25f
    const val MAX_POSE_DEGREES = 20f

    /** ~250-400 ms at 20-32 analysed frames per second. */
    const val CONSECUTIVE_WINDOW_MIN_MILLIS = 250L
    const val CONSECUTIVE_WINDOW_MAX_MILLIS = 400L

    const val CONFIRMATION_COOLDOWN_MILLIS = 5 * 60 * 1000L
    const val EVIDENCE_METHOD = "camera_mouth_open"
    const val EPSILON = 1e-4f

    /**
     * mar = |y13 - y14| / max(epsilon, |x61 - x291|), with the normalized
     * landmarks scaled to the actual image size first. Scaling matters because
     * x and y are normalized independently, so a non-square frame otherwise
     * distorts the ratio.
     */
    fun mouthAspectRatio(
        innerLipTopY: Float,
        innerLipBottomY: Float,
        leftCornerX: Float,
        rightCornerX: Float,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val verticalPixels = abs(innerLipTopY - innerLipBottomY) * imageHeight
        val horizontalPixels = abs(leftCornerX - rightCornerX) * imageWidth
        return verticalPixels / horizontalPixels.coerceAtLeast(EPSILON)
    }

    fun mouthAspectRatio(
        landmarks: List<LandmarkPoint>,
        imageWidth: Int,
        imageHeight: Int
    ): Float? {
        if (landmarks.size <= MouthLandmarkIndex.MOUTH_RIGHT_CORNER) return null
        val top = landmarks[MouthLandmarkIndex.INNER_LIP_TOP]
        val bottom = landmarks[MouthLandmarkIndex.INNER_LIP_BOTTOM]
        val left = landmarks[MouthLandmarkIndex.MOUTH_LEFT_CORNER]
        val right = landmarks[MouthLandmarkIndex.MOUTH_RIGHT_CORNER]
        return mouthAspectRatio(top.y, bottom.y, left.x, right.x, imageWidth, imageHeight)
    }

    /**
     * confidence = clamp01(0.5 + (meanMarDuringWindow - MAR_THRESHOLD)).
     *
     * The window must contain [REQUIRED_CONSECUTIVE_FRAMES] sustained frames, so
     * a single frame can never produce a value on its own. A window exactly at
     * the threshold yields the neutral 0.5; more convincing openings yield up to
     * 1.0, and weak-but-qualifying windows are clamped at 0.0.
     */
    fun confidence(meanMarDuringWindow: Float, threshold: Float = MAR_THRESHOLD): Float =
        (0.5f + (meanMarDuringWindow - threshold)).coerceIn(0f, 1f)

    /**
     * Approximate head yaw in degrees from the nose position between the face
     * edges. This is a deliberately coarse gate used to reject profile faces,
     * not a calibrated pose estimator.
     */
    fun yawDegrees(landmarks: List<LandmarkPoint>): Float {
        if (landmarks.size <= MouthLandmarkIndex.FACE_RIGHT) return 0f
        val left = landmarks[MouthLandmarkIndex.FACE_LEFT]
        val right = landmarks[MouthLandmarkIndex.FACE_RIGHT]
        val nose = landmarks[MouthLandmarkIndex.NOSE_TIP]
        val width = abs(right.x - left.x)
        if (width < EPSILON) return 0f
        val centre = (left.x + right.x) / 2f
        val offset = (nose.x - centre) / width
        val ratio = (2f * offset).coerceIn(-1f, 1f)
        return Math.toDegrees(asin(ratio.toDouble())).toFloat()
    }

    /**
     * Approximate head pitch in degrees from the nose tip's position between the
     * eye line and the mouth line. A frontal face sits near the midpoint.
     */
    fun pitchDegrees(landmarks: List<LandmarkPoint>): Float {
        if (landmarks.size <= MouthLandmarkIndex.MOUTH_RIGHT_CORNER) return 0f
        val leftEye = landmarks[MouthLandmarkIndex.LEFT_EYE_OUTER]
        val rightEye = landmarks[MouthLandmarkIndex.RIGHT_EYE_OUTER]
        val leftMouth = landmarks[MouthLandmarkIndex.MOUTH_LEFT_CORNER]
        val rightMouth = landmarks[MouthLandmarkIndex.MOUTH_RIGHT_CORNER]
        val nose = landmarks[MouthLandmarkIndex.NOSE_TIP]
        val eyeMid = (leftEye.y + rightEye.y) / 2f
        val mouthMid = (leftMouth.y + rightMouth.y) / 2f
        val height = abs(mouthMid - eyeMid)
        if (height < EPSILON) return 0f
        val noseRelative = (nose.y - eyeMid) / height
        // +/-20 degrees maps to roughly +/-0.22 of the eye-to-mouth span.
        return (noseRelative - 0.5f) * 90f
    }

    /** Face bounding-box width in normalized image coordinates (0..1). */
    fun faceWidthFraction(landmarks: List<LandmarkPoint>): Float {
        if (landmarks.isEmpty()) return 0f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        for (point in landmarks) {
            if (point.x < minX) minX = point.x
            if (point.x > maxX) maxX = point.x
        }
        return maxX - minX
    }
}
