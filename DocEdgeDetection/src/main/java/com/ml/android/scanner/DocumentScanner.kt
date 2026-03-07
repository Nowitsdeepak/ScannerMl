package com.ml.android.scanner

import android.graphics.Bitmap
import com.ml.android.processor.ImageProcessor
import com.ml.android.scanner.models.DetectedDocument
import com.ml.android.scanner.utils.GeometryUtils
import org.opencv.core.Mat
import org.opencv.core.Point

/**
 * Main API class for document scanning functionality.
 *
 * This class provides a simple, camera-agnostic API for document detection and processing.
 * It can be used with any image source (CameraX, Camera2, static images, etc.).
 *
 * Usage:
 * ```
 * val scanner = DocumentScanner(config)
 * val detected = scanner.processFrame(imageMat)
 * val cropped = scanner.cropDocument(imageMat, detected.points)
 * ```
 *
 * @property config Configuration for scanner behavior and appearance
 */
class DocumentScanner(
    private var config: DocumentScannerConfig = DocumentScannerConfig()
) {
    private val imageProcessor = ImageProcessor(config)
    private var smoothedPolygon: List<Point>? = null
    
    fun updateConfig(newConfig: DocumentScannerConfig) {
        this.config = newConfig
        imageProcessor.updateConfig(newConfig)
    }
    
    /**
     * Process a single frame and detect document edges.
     *
     * @param image Input image as OpenCV Mat
     * @return DetectedDocument if a valid document is found, null otherwise
     */
    fun processFrame(image: Mat): DetectedDocument? {
        val points = imageProcessor.processImage(image)
        
        return if (points != null && points.size == 4) {
            DetectedDocument(
                points = points,
                frameWidth = image.cols(),
                frameHeight = image.rows()
            )
        } else {
            null
        }
    }
    
    /**
     * Process frame with smoothing for animation.
     * This applies exponential moving average to create smooth polygon transitions.
     *
     * @param image Input image as OpenCV Mat
     * @return DetectedDocument with smoothed points if found, null otherwise
     */
    fun processFrameSmooth(image: Mat): DetectedDocument? {
        val points = imageProcessor.processImage(image)
        
        return if (points != null && points.size == 4) {
            // Apply smoothing
            val smoothed = GeometryUtils.smoothPolygon(
                newPoints = points,
                previousSmoothed = smoothedPolygon,
                alpha = config.smoothingAlpha
            )
            smoothedPolygon = smoothed
            
            DetectedDocument(
                points = smoothed,
                frameWidth = image.cols(),
                frameHeight = image.rows()
            )
        } else {
            smoothedPolygon = null
            null
        }
    }
    
    /**
     * Crop document from image using detected polygon points.
     *
     * @param image Source image as OpenCV Mat
     * @param points 4 corner points of the document
     * @return Cropped and perspective-corrected bitmap, or null if crop fails
     */
    fun cropDocument(image: Mat, points: List<Point>): Bitmap? {
        return imageProcessor.cropDocument(image, points)
    }
    
    /**
     * Draw polygon overlay on bitmap (GraphicsOverlay pattern).
     * This is useful for rendering the detected polygon directly on a bitmap
     * for display purposes, similar to Android's View system GraphicsOverlay.
     *
     * @param bitmap Source bitmap to draw on
     * @param points Polygon points to draw
     * @param strokeColor Color for polygon stroke (default: from config)
     * @param fillColor Color for polygon fill (default: from config)
     * @return New bitmap with polygon overlay drawn
     */
    fun drawPolygonOverlay(
        bitmap: Bitmap,
        points: List<Point>,
        strokeColor: Int = config.strokeColor,
        fillColor: Int = config.fillColor
    ): Bitmap {
        // Create a mutable copy of the bitmap
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)
        
        // Create path from points
        val path = android.graphics.Path()
        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x.toFloat(), point.y.toFloat())
            } else {
                path.lineTo(point.x.toFloat(), point.y.toFloat())
            }
        }
        path.close()
        
        // Draw fill
        val fillPaint = android.graphics.Paint().apply {
            color = fillColor
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(path, fillPaint)
        
        // Draw stroke
        val strokePaint = android.graphics.Paint().apply {
            color = strokeColor
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawPath(path, strokePaint)
        
        return mutableBitmap
    }
    
    /**
     * Reset internal state (e.g., smoothed polygon)
     */
    fun reset() {
        smoothedPolygon = null
    }
}
