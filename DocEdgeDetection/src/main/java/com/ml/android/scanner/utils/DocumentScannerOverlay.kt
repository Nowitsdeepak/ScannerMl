package com.ml.android.scanner.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ml.android.scanner.DocumentScannerConfig
import com.ml.android.scanner.models.DetectedDocument
import com.ml.android.utils.pxToDp

/**
 * Smart wrapper composable that handles camera preview and polygon overlay with automatic sizing.
 *
 * This composable uses BoxWithConstraints internally to handle all size calculations,
 * so developers don't need to worry about coordinate transformations or sizing logic.
 *
 * @param detectedDocument The detected document with polygon points, or null if no document detected
 * @param modifier Modifier for the container
 * @param config Scanner configuration for colors and behavior
 * @param camera Composable function for the camera preview (will be layered below the overlay)
 * @param customOverlay Optional custom overlay composable for advanced users who want custom rendering
 *
 * Usage:
 * ```
 * DocumentScannerOverlay(
 *     detectedDocument = detectedDoc,
 *     camera = {
 *         AndroidView(factory = { PreviewView(it) })
 *     }
 * )
 * ```
 */

@Composable
fun DocumentScannerOverlay(
    detectedDocument: DetectedDocument?,
    modifier: Modifier = Modifier,
    config: DocumentScannerConfig = DocumentScannerConfig(),
    camera: @Composable () -> Unit,
    customOverlay: (@Composable (DetectedDocument?, Float, Float) -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val previewWidth = constraints.maxWidth
        val previewHeight = constraints.maxHeight


        // Render camera preview first (bottom layer)
        camera()

        // Render overlay on top
        if (customOverlay != null) {
            // User-provided custom overlay
            customOverlay(detectedDocument, previewWidth.toFloat(), previewHeight.toFloat())
        } else {
            // Default polygon canvas overlay
            DefaultPolygonCanvas(
                detectedDocument = detectedDocument,
                config = config,
                modifier = Modifier.size(previewWidth.pxToDp(), previewHeight.pxToDp())
            )
        }
    }
}

/**
 * Default polygon canvas overlay implementation
 */
@Composable
private fun DefaultPolygonCanvas(
    detectedDocument: DetectedDocument?,
    config: DocumentScannerConfig,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {

        val previewWidth: Float = size.width
        val previewHeight: Float = size.height

        detectedDocument?.let { doc ->
            if (doc.isValid) {
                val points = doc.points

                // Check if rotation is needed (analyzer is landscape, preview is portrait)
                val isAnalyzerLandscape = doc.frameWidth > doc.frameHeight
                val isPreviewPortrait = previewHeight > previewWidth
                val needsRotation = isAnalyzerLandscape && isPreviewPortrait

                // Determine dimensions after rotation
                val contentWidth =
                    if (needsRotation) doc.frameHeight.toFloat() else doc.frameWidth.toFloat()
                val contentHeight =
                    if (needsRotation) doc.frameWidth.toFloat() else doc.frameHeight.toFloat()

                // Calculate scaling from analyzer resolution to preview size (FIT_CENTER)
                val scaleX = previewWidth / contentWidth
                val scaleY = previewHeight / contentHeight
                val scale = minOf(scaleX, scaleY)

                // Calculate scaled dimensions and centering offsets
                val scaledW = contentWidth * scale
                val scaledH = contentHeight * scale
                val offsetX = (previewWidth - scaledW) / 2
                val offsetY = (previewHeight - scaledH) / 2

                val path = Path()

                points.forEachIndexed { index, point ->
                    var x = point.x.toFloat()
                    var y = point.y.toFloat()

                    // Apply rotation if needed (270 degrees CW / 90 degrees CCW: x,y -> height-y, x)
                    if (needsRotation) {
                        val rotatedX = doc.frameHeight - y
                        val rotatedY = x
                        x = rotatedX
                        y = rotatedY
                    }

                    // Scale and offset the coordinates
                    val screenX = x * scale + offsetX
                    val screenY = y * scale + offsetY

                    if (index == 0) {
                        path.moveTo(screenX, screenY)
                    } else {
                        path.lineTo(screenX, screenY)
                    }
                }
                path.close()

                // Draw filled polygon with transparency
                drawPath(
                    path = path,
                    color = androidx.compose.ui.graphics.Color(config.fillColor),
                    style = Fill
                )

                // Draw polygon stroke
                drawPath(
                    path = path,
                    color = androidx.compose.ui.graphics.Color(config.strokeColor),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
