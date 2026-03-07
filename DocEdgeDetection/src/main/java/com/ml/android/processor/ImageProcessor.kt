package com.ml.android.processor

import android.graphics.Bitmap
import com.ml.android.scanner.DocumentScannerConfig
import com.ml.android.scanner.utils.HoughLineDetector
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap

class ImageProcessor(
    private var config: DocumentScannerConfig = DocumentScannerConfig()
) {
    
    fun updateConfig(newConfig: DocumentScannerConfig) {
        this.config = newConfig
    }



    /**
     * Process image to detect document edges
     * Returns list of 4 corner points if document detected, null otherwise
     */
    fun processImage(inputMat: Mat): List<Point>? {
        var gray = Mat()
        val blurred = Mat()
        val edges = Mat()

        try {
            // Convert to grayscale if needed
            if (inputMat.channels() > 1) {
                Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_RGBA2GRAY)
            } else {
                gray = inputMat.clone()
            }

            // Apply bilateral filter to reduce noise while keeping edges sharp
            Imgproc.bilateralFilter(gray, blurred, 9, 75.0, 75.0)

            // Apply Gaussian blur
            Imgproc.GaussianBlur(blurred, blurred, Size(5.0, 5.0), 0.0)

            // Apply Canny edge detection
            Imgproc.Canny(blurred, edges, 30.0, 100.0)

            // Dilate to connect fragmented edges
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
            Imgproc.dilate(edges, edges, kernel)

            // Mode 1: Hough-only detection
            if (config.detectionMode == 1) {
                val houghPoints = HoughLineDetector.detectDocument(edges, inputMat.cols(), inputMat.rows())
                if (houghPoints != null && isValidDocumentShape(houghPoints, inputMat.cols(), inputMat.rows())) {
                    return houghPoints
                }
                return null
            }

            // Mode 0 or 2: Canny-based detection (with or without fallbacks)
            // Find contours
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                edges,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            // Sort contours by area (largest first)
            contours.sortByDescending { Imgproc.contourArea(it) }

            // Try to find 4-point contour
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                
                // Skip very small contours
                if (area < 3000) continue

                val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(
                    MatOfPoint2f(*contour.toArray()),
                    approx,
                    0.02 * peri,
                    true
                )

                val points = approx.toArray()

                // Found perfect 4-point contour
                if (points.size == 4) {
                    // Validate shape before returning
                    if (isValidDocumentShape(points.toList(), inputMat.cols(), inputMat.rows())) {
                        return points.toList()
                    }
                }
            }

            // Mode 0: Canny-only, stop here if no contour found
            if (config.detectionMode == 0) {
                return null
            }

            // Mode 2: Hybrid - continue with fallbacks
            // Fallback: Use minAreaRect if no perfect 4-point contour found
            if (contours.isNotEmpty()) {
                val largestContour = contours[0]
                val area = Imgproc.contourArea(largestContour)
                
                if (area > 3000) {
                    val rect = Imgproc.minAreaRect(MatOfPoint2f(*largestContour.toArray()))
                    
                    // Get the 4 corners of the rotated rectangle
                    val vertices = Mat()
                    Imgproc.boxPoints(rect, vertices)
                    
                    val boxPoints = mutableListOf<Point>()
                    for (i in 0 until 4) {
                        val x = vertices.get(i, 0)[0]
                        val y = vertices.get(i, 1)[0]
                        boxPoints.add(Point(x, y))
                    }
                    
                    vertices.release()
                    
                    // Validate minAreaRect result
                    if (isValidDocumentShape(boxPoints, inputMat.cols(), inputMat.rows())) {
                        return boxPoints
                    }
                }
            }

            // Final fallback: Use Hough Line Transform for partial/broken edges
            val houghPoints = HoughLineDetector.detectDocument(edges, inputMat.cols(), inputMat.rows())
            if (houghPoints != null && isValidDocumentShape(houghPoints, inputMat.cols(), inputMat.rows())) {
                return houghPoints
            }

            return null

        } finally {
            gray.release()
            blurred.release()
            edges.release()
        }
    }

    /**
     * Validate if detected polygon is a valid document shape
     */
    private fun isValidDocumentShape(points: List<Point>, frameWidth: Int, frameHeight: Int): Boolean {
        if (points.size != 4) return false
        
        // 1. Calculate aspect ratio
        val orderedPts = orderPoints(points)
        val width = maxOf(
            distance(orderedPts[0], orderedPts[1]),
            distance(orderedPts[2], orderedPts[3])
        )
        val height = maxOf(
            distance(orderedPts[1], orderedPts[2]),
            distance(orderedPts[0], orderedPts[3])
        )
        
        val aspectRatio = (width / height).toFloat()
        if (aspectRatio < 0.25f || aspectRatio > 4.0f) {  // Relaxed from 0.3-3.0
            return false // Too thin or too tall
        }
        
        // 2. Check minimum area (must be at least 12% of frame)
        val frameArea = frameWidth * frameHeight
        val polygonArea = calculatePolygonArea(points)
        if (polygonArea < frameArea * 0.12) {
            return false // Too small
        }
        
        // 3. Validate corner angles (should be approximately 90°)
        for (i in 0..3) {
            val prev = orderedPts[(i + 3) % 4]
            val curr = orderedPts[i]
            val next = orderedPts[(i + 1) % 4]
            
            val angle = calculateAngle(prev, curr, next)
            if (angle < 50.0 || angle > 130.0) {  // Relaxed from 55-125
                return false // Not a rectangular shape
            }
        }
        
        return true
    }

    /**
     * Calculate area of polygon using Shoelace formula
     */
    private fun calculatePolygonArea(points: List<Point>): Double {
        var area = 0.0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return kotlin.math.abs(area / 2.0)
    }

    /**
     * Calculate angle at point 'b' formed by points a-b-c
     */
    private fun calculateAngle(a: Point, b: Point, c: Point): Double {
        val ba = Point(a.x - b.x, a.y - b.y)
        val bc = Point(c.x - b.x, c.y - b.y)
        
        val dotProduct = ba.x * bc.x + ba.y * bc.y
        val magnitudeBA = kotlin.math.sqrt(ba.x * ba.x + ba.y * ba.y)
        val magnitudeBC = kotlin.math.sqrt(bc.x * bc.x + bc.y * bc.y)
        
        val cosAngle = dotProduct / (magnitudeBA * magnitudeBC)
        val angleRad = kotlin.math.acos(cosAngle.coerceIn(-1.0, 1.0))
        return Math.toDegrees(angleRad)
    }

    /**
     * Crop document from image using perspective transform
     */
    fun cropDocument(inputMat: Mat, points: List<Point>): Bitmap? {
        if (points.size != 4) return null

        try {
            // Order points: top-left, top-right, bottom-right, bottom-left
            val orderedPoints = orderPoints(points)
            
            // Calculate destination size
            val (dstPoints, width, height) = calculateDestinationPoints(orderedPoints)

            // Create transformation matrices
            val srcMat = MatOfPoint2f(*orderedPoints.toTypedArray())
            val dstMat = MatOfPoint2f(*dstPoints.toTypedArray())

            // Get perspective transform
            val transform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            
            // Apply warp perspective
            val warped = Mat()
            Imgproc.warpPerspective(
                inputMat,
                warped,
                transform,
                Size(width.toDouble(), height.toDouble()),
                Imgproc.INTER_LINEAR
            )

            // Flip horizontally to correct left-to-right mirror
            val flipped = Mat()
            Core.flip(warped, flipped, 1)  // 1 = horizontal flip

            // Convert to bitmap
            val bitmap = createBitmap(flipped.cols(), flipped.rows())
            Utils.matToBitmap(flipped, bitmap)

            warped.release()
            flipped.release()
            srcMat.release()
            dstMat.release()
            transform.release()

            return bitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Order points in clockwise order: top-left, top-right, bottom-right, bottom-left
     */
    private fun orderPoints(points: List<Point>): List<Point> {
        val ordered = Array(4) { Point() }

        val sum = points.map { it.x + it.y }
        val diff = points.map { it.x - it.y }

        // Top-left has smallest sum
        ordered[0] = points[sum.indexOf(sum.minOrNull()!!)]
        
        // Bottom-right has largest sum
        ordered[2] = points[sum.indexOf(sum.maxOrNull()!!)]
        
        // Top-right has smallest difference
        ordered[1] = points[diff.indexOf(diff.minOrNull()!!)]
        
        // Bottom-left has largest difference
        ordered[3] = points[diff.indexOf(diff.maxOrNull()!!)]

        return ordered.toList()
    }

    /**
     * Calculate destination points and dimensions for perspective transform
     */
    private fun calculateDestinationPoints(srcPoints: List<Point>): Triple<List<Point>, Int, Int> {
        // Calculate width
        val widthA = distance(srcPoints[0], srcPoints[1])
        val widthB = distance(srcPoints[2], srcPoints[3])
        val maxWidth = maxOf(widthA, widthB).toInt()

        // Calculate height
        val heightA = distance(srcPoints[1], srcPoints[2])
        val heightB = distance(srcPoints[0], srcPoints[3])
        val maxHeight = maxOf(heightA, heightB).toInt()

        // Create destination points
        val dstPoints = listOf(
            Point(0.0, 0.0),
            Point(maxWidth.toDouble(), 0.0),
            Point(maxWidth.toDouble(), maxHeight.toDouble()),
            Point(0.0, maxHeight.toDouble())
        )

        return Triple(dstPoints, maxWidth, maxHeight)
    }

    /**
     * Calculate Euclidean distance between two points
     */
    private fun distance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
