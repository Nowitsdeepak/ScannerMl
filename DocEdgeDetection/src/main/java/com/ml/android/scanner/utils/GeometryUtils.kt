package com.ml.android.scanner.utils

import org.opencv.core.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utility functions for geometric calculations on polygons and points.
 */
object GeometryUtils {

    /**
     * Order points in clockwise order: top-left, top-right, bottom-right, bottom-left
     */
    fun orderPoints(points: List<Point>): List<Point> {
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
     * Calculate Euclidean distance between two points
     */
    fun distance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculate area of polygon using Shoelace formula
     */
    fun calculatePolygonArea(points: List<Point>): Double {
        var area = 0.0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return abs(area / 2.0)
    }

    /**
     * Calculate angle at point 'b' formed by points a-b-c
     */
    fun calculateAngle(a: Point, b: Point, c: Point): Double {
        val ba = Point(a.x - b.x, a.y - b.y)
        val bc = Point(c.x - b.x, c.y - b.y)

        val dotProduct = ba.x * bc.x + ba.y * bc.y
        val magnitudeBA = sqrt(ba.x * ba.x + ba.y * ba.y)
        val magnitudeBC = sqrt(bc.x * bc.x + bc.y * bc.y)

        val cosAngle = dotProduct / (magnitudeBA * magnitudeBC)
        val angleRad = acos(cosAngle.coerceIn(-1.0, 1.0))
        return Math.toDegrees(angleRad)
    }

    /**
     * Check if two polygons are similar (for stability tracking)
     */
    fun isPolygonSimilar(p1: List<Point>, p2: List<Point>, threshold: Double = 25.0): Boolean {
        if (p1.size != p2.size) return false

        for (i in p1.indices) {
            val dist = sqrt(
                (p1[i].x - p2[i].x).pow(2) + (p1[i].y - p2[i].y).pow(2)
            )
            if (dist > threshold) return false
        }
        return true
    }

    /**
     * Smooth polygon points using exponential moving average (lerp)
     */
    fun smoothPolygon(
        newPoints: List<Point>,
        previousSmoothed: List<Point>?,
        alpha: Float
    ): List<Point> {
        // If no previous smoothed polygon, use new points directly
        if (previousSmoothed == null || previousSmoothed.size != newPoints.size) {
            return newPoints
        }

        // Apply exponential moving average to each point
        val smoothed = mutableListOf<Point>()
        for (i in newPoints.indices) {
            val oldPoint = previousSmoothed[i]
            val newPoint = newPoints[i]

            // Lerp formula: smoothed = old + alpha * (new - old)
            val smoothedX = oldPoint.x + alpha * (newPoint.x - oldPoint.x)
            val smoothedY = oldPoint.y + alpha * (newPoint.y - oldPoint.y)

            smoothed.add(Point(smoothedX, smoothedY))
        }

        return smoothed
    }

    /**
     * Calculate center point of polygon
     */
    fun calculatePolygonCenter(points: List<Point>): Point {
        val avgX = points.map { it.x }.average()
        val avgY = points.map { it.y }.average()
        return Point(avgX, avgY)
    }

    /**
     * Calculate destination points and dimensions for perspective transform
     */
    fun calculateDestinationPoints(srcPoints: List<Point>): Triple<List<Point>, Int, Int> {
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
}
