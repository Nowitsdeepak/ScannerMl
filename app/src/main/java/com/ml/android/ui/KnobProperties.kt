package com.ml.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration properties for the KnobController.
 *
 * Provides customizable properties for line spacing, width, alpha, colors,
 * marker appearance, and sound behavior.
 *
 * Usage:
 * - Use the data class constructor with default values: `KnobProperties()` for default behavior
 * - Use `KnobProperties.Custom(...)` to provide all parameters explicitly (no defaults)
 * - Use preset styles: `KnobProperties.Compact`, `KnobProperties.Wide`, etc.
 */
data class KnobProperties(
    // Line spacing and geometry
    val lineSpacing: Dp,

    // Line width range (0.0 to 1.0)
    val minWidthPercent: Float,
    val maxWidthPercent: Float,

    // Line alpha/opacity range (0.0 to 1.0)
    val minAlpha: Float,
    val maxAlpha: Float,

    // Line colors
    val lineColor: Color,
    val referenceLineColor: Color,

    // Stroke width
    val strokeWidth: Dp,

    // Triangle marker properties
    val showTriangleMarker: Boolean,
    val triangleMarkerHeight: Dp,
    val triangleMarkerColor: Color,

    // Sound properties
    val enableSound: Boolean,
    val alignmentThresholdFraction: Float,

    // Circular loop properties
    val useCircularLoop: Boolean,
) {
    companion object {
        /**
         * Creates a custom KnobProperties instance where all parameters must be explicitly provided.
         * No default values are used - you must specify every property.
         *
         * Use this when you want full control over all properties without relying on defaults.
         */
        fun Default(
            // Line spacing and geometry
            lineSpacing: Dp = 5.dp,

            // Line width range (0.0 to 1.0)
            minWidthPercent: Float = 0.75f,
            maxWidthPercent: Float = 1.0f,

            // Line alpha/opacity range (0.0 to 1.0)
            minAlpha: Float = 0.1f,
            maxAlpha: Float = 1.0f,

            // Line colors
            lineColor: Color = Color.Black,
            referenceLineColor: Color = Color.Red,

            // Stroke width
            strokeWidth: Dp = 1.dp,

            // Triangle marker properties
            showTriangleMarker: Boolean = true,
            triangleMarkerHeight: Dp = 3.dp,
            triangleMarkerColor: Color = Color.Red,

            // Sound properties
            enableSound: Boolean = true,
            alignmentThresholdFraction: Float = 0.15f, // Fraction of line spacing for alignment detection

            // Circular loop properties
            useCircularLoop: Boolean = true, // Whether to show reference line based on circular loop
        ) = KnobProperties(
            lineSpacing = lineSpacing,
            minWidthPercent = minWidthPercent,
            maxWidthPercent = maxWidthPercent,
            minAlpha = minAlpha,
            maxAlpha = maxAlpha,
            lineColor = lineColor,
            referenceLineColor = referenceLineColor,
            strokeWidth = strokeWidth,
            showTriangleMarker = showTriangleMarker,
            triangleMarkerHeight = triangleMarkerHeight,
            triangleMarkerColor = triangleMarkerColor,
            enableSound = enableSound,
            alignmentThresholdFraction = alignmentThresholdFraction,
            useCircularLoop = useCircularLoop,
        )
    }
}
