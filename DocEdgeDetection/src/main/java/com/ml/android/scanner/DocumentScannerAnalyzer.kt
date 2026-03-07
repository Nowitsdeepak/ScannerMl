package com.ml.android.scanner

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.ml.android.scanner.models.DetectedDocument
import com.ml.android.scanner.utils.GeometryUtils
import com.ml.android.scanner.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opencv.core.Point

/**
 * Reusable CameraX ImageAnalysis.Analyzer for document scanning.
 *
 * This analyzer can be integrated into any CameraX-based camera implementation.
 * It handles document detection, smoothing, and optional auto-capture.
 *
 * Usage:
 * ```
 * val analyzer = DocumentScannerAnalyzer(
 *     config = DocumentScannerConfig(autoCapture = true),
 *     onDocumentDetected = { android, preview -> /* update UI */ },
 *     onDocumentCaptured = { bitmap -> /* save image */ }
 * )
 * imageAnalysis.setAnalyzer(executor, analyzer)
 * ```
 *
 * @property config Scanner configuration
 * @property onDocumentDetected Callback for each frame with detected document and preview bitmap
 * @property onDocumentCaptured Optional callback when auto-capture triggers (only if autoCapture enabled)
 */
class DocumentScannerAnalyzer(
    private var config: DocumentScannerConfig = DocumentScannerConfig(),
    private val onDocumentDetected: (DetectedDocument?, Bitmap) -> Unit,
    private val onDocumentCaptured: ((Bitmap) -> Unit)? = null
) : ImageAnalysis.Analyzer {
    
    private val scanner = DocumentScanner(config)
    
    fun updateConfig(newConfig: DocumentScannerConfig) {
        this.config = newConfig
        scanner.updateConfig(newConfig)
    }
    private var stableFrameCount = 0
    private var lastStablePolygon: List<Point>? = null
    private var shouldStartCapturing = true
    private var lastCaptureTime = 0L
    private var lastCapturedCenter: Point? = null
    private var triggerManualCapture = false
    
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.image?.let { mediaImage ->
            if (mediaImage.format == ImageFormat.YUV_420_888 && mediaImage.planes.size == 3) {
                // Convert YUV to RGBA
                val frame = ImageUtils.yuvToRgba(mediaImage)
                
                // Process frame with smoothing
                val detectedDocument = scanner.processFrameSmooth(frame)
                
                // Convert frame to bitmap for preview
                val previewBitmap = ImageUtils.matToBitmap(frame)
                
                // Notify UI with detected document
                onDocumentDetected(detectedDocument, previewBitmap)
                
                // Handle manual capture request
                if (triggerManualCapture) {
                    triggerManualCapture = false
                    if (detectedDocument != null && detectedDocument.isValid) {
                        val center = GeometryUtils.calculatePolygonCenter(detectedDocument.points)
                        captureDocument(frame, detectedDocument.points, center)
                    }
                }
                
                // Handle auto-capture if enabled
                if (config.autoCapture && onDocumentCaptured != null) {
                    handleAutoCapture(detectedDocument, frame)
                }
                
                frame.release()
            }
        }
        image.close()
    }
    
    fun triggerManualCapture() {
        triggerManualCapture = true
    }
    
    private fun handleAutoCapture(detectedDocument: DetectedDocument?, frame: org.opencv.core.Mat) {
        if (detectedDocument == null || !detectedDocument.isValid) {
            // No valid polygon detected, reset stability
            stableFrameCount = 0
            lastStablePolygon = null
            return
        }
        
        val points = detectedDocument.points
        
        // Check if polygon is stable (similar to last frame)
        val isStable = lastStablePolygon?.let { last ->
            GeometryUtils.isPolygonSimilar(last, points)
        } ?: false
        
        if (isStable) {
            stableFrameCount++
            lastStablePolygon = points
        } else {
            // Polygon changed significantly, reset counter
            stableFrameCount = 1
            lastStablePolygon = points
        }
        
        // Check if we should capture
        val currentTime = System.currentTimeMillis()
        val cooldownExpired = (currentTime - lastCaptureTime) > config.postCaptureCooldownMs
        
        // Ensure smoothed polygon has settled (is close to actual detected points)
        val smoothedHasSettled = GeometryUtils.isPolygonSimilar(
            points,
            points, // Compare with itself to ensure it's stable
            threshold = 10.0
        )
        
        if (stableFrameCount >= config.requiredStableFrames && 
            shouldStartCapturing && 
            cooldownExpired && 
            smoothedHasSettled) {
            
            // Capture the document
            val polygonCenter = GeometryUtils.calculatePolygonCenter(points)
            captureDocument(frame, points, polygonCenter)
        }
    }
    
    private fun captureDocument(frame: org.opencv.core.Mat, points: List<Point>, center: Point) {
        try {
            shouldStartCapturing = false
            
            // Crop document
            val croppedBitmap = scanner.cropDocument(frame, points)
            
            if (croppedBitmap != null) {
                // Update capture tracking
                lastCaptureTime = System.currentTimeMillis()
                lastCapturedCenter = center
                stableFrameCount = 0
                
                // Notify callback
                CoroutineScope(Dispatchers.Main).launch {
                    onDocumentCaptured?.invoke(croppedBitmap)
                    delay(config.postCaptureCooldownMs)
                    shouldStartCapturing = true
                }
            } else {
                shouldStartCapturing = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shouldStartCapturing = true
        }
    }
    

}
