package com.ml.android.scanner.models

import org.opencv.core.Point

/**
 * Represents a detected document in a camera frame.
 *
 * @property points The 4 corner points of the detected document polygon (ordered: top-left, top-right, bottom-right, bottom-left)
 * @property frameWidth Width of the source frame in pixels
 * @property frameHeight Height of the source frame in pixels
 * @property confidence Confidence score of the detection (0.0 to 1.0)
 * @property timestamp Timestamp when the document was detected
 */
data class DetectedDocument(
    val points: List<Point>,
    val frameWidth: Int,
    val frameHeight: Int,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this is a valid 4-point polygon
     */
    val isValid: Boolean
        get() = points.size == 4
}
