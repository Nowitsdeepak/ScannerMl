package com.ml.android.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ml.android.R
import com.ml.android.ui.theme.DocEdgeDetectionTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A vertical-scrolling wheel/gear indicator controller with drag gesture support.
 *
 * Features:
 * - Vertical lines arranged like a gear
 * - Infinite scrolling with vertical drag gestures
 * - All lines move together as a unified gear
 * - Red reference line to track rotation
 * - Rotation tracking based on drag amount
 *
 * @param range The allowed range of line indices for scrolling (default: unlimited)
 * @param properties Configuration properties for the knob appearance and behavior.
 *                   Defaults to [KnobProperties] with default parameter values.
 *                   Use [KnobProperties.Custom] to provide all parameters explicitly.
 * @param modifier Modifier for the composable
 * @param onRotationChange Callback when rotation amount changes (in units of line spacing)
 */
@Composable
fun KnobController(
    modifier: Modifier = Modifier,
    range: IntRange = IntRange(-Int.MAX_VALUE, Int.MAX_VALUE),
    properties: KnobProperties,
    onRotationChange: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current

    val soundPool = remember {
        if (properties.enableSound) SoundPoolHelper(context) else null
    }

    // State to track cumulative vertical offset for scrolling
    var offset by remember { mutableFloatStateOf(0f) }

    // Get density to convert dp to px (line spacing and marker height)
    val density = LocalDensity.current
    val spacePx = with(density) { properties.lineSpacing.toPx() }
    val markerHeightPx = with(density) { properties.triangleMarkerHeight.toPx() }

    // Handle tick sound when a line aligns with the triangle center
    if (properties.enableSound && soundPool != null) {
        HandleCenterAlignmentTick(
            offset = offset,
            spacePx = spacePx,
            soundPool = soundPool,
            alignmentThresholdFraction = properties.alignmentThresholdFraction,
        )
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                // Proposed new offset from drag
                val newOffset = offset - dragAmount.y

                // Convert allowed index range into offset range in pixels
                val minOffset = range.first * spacePx
                val maxOffset = range.last * spacePx

                // Clamp offset so the center line index stays inside the provided range
                offset = newOffset.coerceIn(minOffset, maxOffset)

                // Calculate rotation amount (in units of line spacing)
                val rotation = offset / spacePx
                onRotationChange?.invoke(rotation)
            }
        }) {

        drawKnobLines(
            offset = offset,
            spacePx = spacePx,
            properties = properties,
        )

        if (properties.showTriangleMarker) {
            drawTriangleMarker(
                canvasHeight = size.height,
                markerHeight = markerHeightPx,
                markerColor = properties.triangleMarkerColor,
            )
        }
    }


    DisposableEffect(soundPool) {
        onDispose {
            soundPool?.releasePool()
        }
    }
}

@Composable
private fun HandleCenterAlignmentTick(
    offset: Float,
    spacePx: Float,
    soundPool: SoundPoolHelper,
    alignmentThresholdFraction: Float,
) {
    var lastTickLineIndex by remember { mutableFloatStateOf(Float.NaN) }

    // Line index currently closest to the center triangle
    val nearestLineToCenter = (offset / spacePx).roundToInt()
    val centerDeltaPx = abs(offset - nearestLineToCenter * spacePx)
    val alignmentThreshold = spacePx * alignmentThresholdFraction

    LaunchedEffect(nearestLineToCenter, centerDeltaPx) {
        if (centerDeltaPx <= alignmentThreshold &&
            lastTickLineIndex != nearestLineToCenter.toFloat()
        ) {
            soundPool.playSound()
            lastTickLineIndex = nearestLineToCenter.toFloat()
        } else if (centerDeltaPx > alignmentThreshold * 2f) {
            // Reset when far away so the next pass can trigger again
            lastTickLineIndex = Float.NaN
        }
    }
}

private fun DrawScope.drawKnobLines(
    offset: Float,
    spacePx: Float,
    properties: KnobProperties,
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    val strokeWidth = properties.strokeWidth.toPx()

    // Center Y position of the canvas
    val centerY = canvasHeight / 2f

    // Treat canvas height as circle diameter
    val diameter = canvasHeight
    val circumference = PI.toFloat() * diameter

    // How many lines fit in one full rotation (circumference)
    val linesPerRotation = circumference / spacePx

    // Which line index is currently at the center based on offset
    val centerLineIndex = (offset / spacePx).toInt()

    // How many lines we need to draw (visible + some extra for smooth scrolling)
    val visibleLineCount = (canvasHeight / spacePx).toInt() + 3
    val linesToDraw = visibleLineCount * 2 + 1

    // Starting line index (center - half of lines to draw)
    val startLineIndex = centerLineIndex - (linesToDraw / 2)

    repeat(linesToDraw) { i ->
        // Logical line index
        val lineIndex = startLineIndex + i

        // Position relative to the center
        val lineYRelativeToCenter = (lineIndex * spacePx) - offset
        val y = centerY + lineYRelativeToCenter

        // Skip lines that are completely outside the visible area
        if (y < -strokeWidth || y > canvasHeight + strokeWidth) return@repeat

        // Distance from center in pixels
        val distanceFromCenterY = abs(y - centerY)

        // Normalize distance (0 at center, 1 at farthest visible point)
        val maxDistance = canvasHeight / 2f
        val fraction = (1f - (distanceFromCenterY / maxDistance)).coerceIn(0f, 1f)

        // Lerp width percentage - center line gets maximum width
        val widthPercent = lerp(
            properties.minWidthPercent,
            properties.maxWidthPercent,
            fraction
        )

        // Lerp stroke alpha - center line is most visible
        val strokeAlpha = lerp(
            properties.minAlpha,
            properties.maxAlpha,
            fraction
        )

        val lineWidth = canvasWidth * widthPercent
        val startX = (canvasWidth - lineWidth) / 2f
        val endX = startX + lineWidth

        // Draw reference line based on circular loop (if enabled)
        // Reference line appears every time we complete a full rotation
        val isReferenceLine = if (properties.useCircularLoop) {
            val normalizedPhase = (lineIndex % linesPerRotation).let {
                if (it < 0) it + linesPerRotation else it
            }
            normalizedPhase < 0.5f || normalizedPhase > (linesPerRotation - 0.5f)
        } else {
            false
        }
        val lineColor = if (isReferenceLine) properties.referenceLineColor else properties.lineColor

        drawLine(
            color = lineColor,
            strokeWidth = strokeWidth,
            alpha = strokeAlpha,
            start = Offset(startX, y),
            end = Offset(endX, y)
        )
    }
}

private fun DrawScope.drawTriangleMarker(
    canvasHeight: Float,
    markerHeight: Float,
    markerColor: Color,
) {
    val centerY = canvasHeight / 2f
    val path = Path()

    path.apply {
        moveTo(0f, centerY)
        lineTo(0f, centerY + markerHeight)
        lineTo(markerHeight, centerY)
        lineTo(0f, centerY - markerHeight)
        close()
    }

    drawPath(
        path = path,
        color = markerColor,
    )
}


class SoundPoolHelper(
    context: Context,
) {

    private var soundPool: SoundPool? = null
    private var soundId: Int? = null

    init {

        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
//        TODO:Uncomment when you run it, commented because to view preview
        soundId = soundPool?.load(context, R.raw.tick_sound_effect_336779, 1)
    }


    fun playSound() {
        soundId?.let { sId ->
            soundPool?.play(sId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun releasePool() {
        soundPool?.release()
        soundId = null
        soundPool = null
    }

}

@Preview
@Composable
private fun KnobControllerPreview() {
    DocEdgeDetectionTheme {

        var onRotationChange by remember { mutableFloatStateOf(0f) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            KnobController(
                properties = KnobProperties.Default()
                    .copy(lineColor = Color.Black, enableSound = false),
                modifier = Modifier
                    .size(20.dp, 100.dp),
                onRotationChange = {
                    onRotationChange = it
                })
            Text(
                text = onRotationChange.toString(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
