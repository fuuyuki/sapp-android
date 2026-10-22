package com.example.sapp.verification

import kotlin.math.abs

/** One analysed frame reduced to the values the trigger needs. */
data class FaceFrame(
    val faceCount: Int,
    val faceWidthFraction: Float,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val mouthAspectRatio: Float
)

data class MouthDetection(val confidence: Float, val meanMar: Float)

/**
 * Requires a sustained open mouth: [requiredConsecutiveFrames] qualifying
 * analysed frames in a row. Any frame that fails a gate resets the run, so the
 * trigger cannot be produced by a single frame or by an erratic face.
 *
 * This class is not thread-safe; feed it from a single analysis callback.
 */
class MouthOpenTracker(
    private val requiredConsecutiveFrames: Int = MouthMetrics.REQUIRED_CONSECUTIVE_FRAMES,
    private val marThreshold: Float = MouthMetrics.MAR_THRESHOLD,
    private val maxPoseDegrees: Float = MouthMetrics.MAX_POSE_DEGREES,
    private val minFaceWidthFraction: Float = MouthMetrics.MIN_FACE_WIDTH_FRACTION
) {
    private val recentMars = ArrayDeque<Float>()
    private var consecutive = 0

    val consecutiveFrames: Int get() = consecutive

    fun onFrame(frame: FaceFrame): MouthDetection? {
        val qualifies = frame.faceCount == 1 &&
            frame.faceWidthFraction >= minFaceWidthFraction &&
            abs(frame.yawDegrees) <= maxPoseDegrees &&
            abs(frame.pitchDegrees) <= maxPoseDegrees &&
            frame.mouthAspectRatio >= marThreshold

        if (!qualifies) {
            reset()
            return null
        }

        consecutive++
        recentMars.addLast(frame.mouthAspectRatio)
        while (recentMars.size > requiredConsecutiveFrames) {
            recentMars.removeFirst()
        }

        if (consecutive < requiredConsecutiveFrames) return null

        val meanMar = recentMars.average().toFloat()
        val detection = MouthDetection(
            confidence = MouthMetrics.confidence(meanMar, marThreshold),
            meanMar = meanMar
        )
        // Reset so one open mouth only produces one trigger.
        reset()
        return detection
    }

    fun reset() {
        consecutive = 0
        recentMars.clear()
    }
}
