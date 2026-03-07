package com.ml.android.scanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Configuration for document scanner behavior and appearance.
 *
 * @property minContourArea Minimum contour area in pixels to consider as a document
 * @property minFrameAreaPercent Minimum percentage of frame area the document must occupy (0.0 to 1.0)
 * @property smoothingAlpha Smoothing factor for polygon animation (0.0 to 1.0, lower = smoother but slower)
 * @property requiredStableFrames Number of consecutive stable frames before triggering auto-capture
 * @property postCaptureCooldownMs Cooldown period in milliseconds after capture to prevent duplicates
 * @property minPolygonDistance Minimum distance in pixels between polygon centers to consider as different document
 * @property autoCapture Enable/disable automatic capture when document is stable
 * @property strokeColor Color for polygon stroke outline
 * @property fillAlpha Alpha value for polygon fill (applied to stroke color)
 * @property detectionMode Detection algorithm mode: 0 = Canny only, 1 = Hough only, 2 = Hybrid (Canny + minAreaRect + Hough fallback)
 */
data class DocumentScannerConfig(
    val minContourArea: Double = 3000.0,
    val minFrameAreaPercent: Double = 0.12,
    val smoothingAlpha: Float = 0.15f,
    val requiredStableFrames: Int = 20,
    val postCaptureCooldownMs: Long = 2500L,
    val minPolygonDistance: Float = 50f,
    val autoCapture: Boolean = true,
    val strokeColor: Int = Color.Blue.toArgb(),
    val fillAlpha: Float = 0.3f,
    val detectionMode: Int = 2  // 0 = Canny only, 1 = Hough only, 2 = Hybrid (default)
) {
    /**
     * Fill color auto-generated from stroke color with reduced alpha
     */
    val fillColor: Int
        get() {
            val alpha = (fillAlpha * 255).toInt()
            val rgb = strokeColor and 0x00FFFFFF
            return (alpha shl 24) or rgb
        }
}
