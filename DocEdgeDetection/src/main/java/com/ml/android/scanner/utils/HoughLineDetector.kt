package com.ml.android.scanner.utils

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

/**
 * Hough Line Transform utilities for document detection.
 * Provides robust detection for documents with partial or broken edges.
 */
object HoughLineDetector {
    
    /**
     * Detect document using Hough Line Transform.
     * This method is more robust for documents with partial or broken edges.
     *
     * @param edges Edge-detected image (output from Canny)
     * @param width Image width
     * @param height Image height
     * @return List of 4 corner points if document detected, null otherwise
     */
    fun detectDocument(edges: Mat, width: Int, height: Int): List<Point>? {
        val lines = Mat()
        
        try {
            // Detect lines using Hough Line Transform
            Imgproc.HoughLinesP(
                edges,
                lines,
                1.0,                    // rho: distance resolution in pixels
                Math.PI / 180.0,        // theta: angle resolution in radians
                80,                     // threshold: minimum votes
                 100.0,  // minimum line length
                 10.0       // maximum gap between line segments
            )
            
            if (lines.rows() < 4) {
                lines.release()
                return null
            }
            
            // Group lines into horizontal and vertical
            val horizontalLines = mutableListOf<DoubleArray>()
            val verticalLines = mutableListOf<DoubleArray>()
            
            for (i in 0 until lines.rows()) {
                val line = lines.get(i, 0)
                val x1 = line[0]
                val y1 = line[1]
                val x2 = line[2]
                val y2 = line[3]
                
                val angle = Math.atan2(y2 - y1, x2 - x1) * 180.0 / Math.PI
                
                // Classify as horizontal or vertical based on angle
                if (Math.abs(angle) < 30 || Math.abs(angle) > 150) {
                    horizontalLines.add(line)
                } else if (Math.abs(angle - 90) < 30 || Math.abs(angle + 90) < 30) {
                    verticalLines.add(line)
                }
            }
            
            lines.release()
            
            // Need at least 2 horizontal and 2 vertical lines
            if (horizontalLines.size < 2 || verticalLines.size < 2) {
                return null
            }
            
            // Sort lines and pick the most prominent ones
            horizontalLines.sortBy { it[1] } // Sort by y-coordinate
            verticalLines.sortBy { it[0] }   // Sort by x-coordinate
            
            // Get top and bottom horizontal lines
            val topLine = horizontalLines.first()
            val bottomLine = horizontalLines.last()
            
            // Get left and right vertical lines
            val leftLine = verticalLines.first()
            val rightLine = verticalLines.last()
            
            // Calculate intersections to form quadrilateral
            val topLeft = lineIntersection(topLine, leftLine)
            val topRight = lineIntersection(topLine, rightLine)
            val bottomRight = lineIntersection(bottomLine, rightLine)
            val bottomLeft = lineIntersection(bottomLine, leftLine)
            
            // Validate all intersections exist
            val points = listOfNotNull(topLeft, topRight, bottomRight, bottomLeft)
            if (points.size != 4) {
                return null
            }
            
            // Check if points are within image bounds
            if (points.any { it.x < 0 || it.x > width || it.y < 0 || it.y > height }) {
                return null
            }
            
            return points
            
        } catch (e: Exception) {
            lines.release()
            return null
        }
    }
    
    /**
     * Calculate intersection point of two lines.
     *
     * @param line1 First line as [x1, y1, x2, y2]
     * @param line2 Second line as [x1, y1, x2, y2]
     * @return Intersection point, or null if lines are parallel
     */
    private fun lineIntersection(line1: DoubleArray, line2: DoubleArray): Point? {
        val x1 = line1[0]
        val y1 = line1[1]
        val x2 = line1[2]
        val y2 = line1[3]
        
        val x3 = line2[0]
        val y3 = line2[1]
        val x4 = line2[2]
        val y4 = line2[3]
        
        val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        
        if (Math.abs(denom) < 1e-10) {
            return null // Lines are parallel
        }
        
        val intersectX = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom
        val intersectY = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom
        
        return Point(intersectX, intersectY)
    }
}
