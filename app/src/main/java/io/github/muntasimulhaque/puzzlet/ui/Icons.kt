package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's own icon set, drawn as geometry: a chevron for back, a circular
 * arrow for replay, a two-by-two grid for the picture menu. No icon fonts,
 * no third-party packs: same hand, same weights, everywhere.
 */
@Composable
fun BackIcon(modifier: Modifier = Modifier, color: Color) {
    GeoIcon(modifier, color) { w, h ->
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.20f)
            lineTo(w * 0.35f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.80f)
        }
        drawPath(path, color, style = stroke(w))
    }
}

@Composable
fun ReplayIcon(modifier: Modifier = Modifier, color: Color) {
    GeoIcon(modifier, color) { w, h ->
        val c = Offset(w / 2f, h / 2f)
        val r = w * 0.30f
        drawArc(
            color,
            startAngle = -45f,
            sweepAngle = 315f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = stroke(w),
        )
        // Arrowhead at the arc's start, pointing the way the sweep travels.
        val a0 = Math.toRadians(-45.0)
        val start = Offset(c.x + r * cos(a0).toFloat(), c.y + r * sin(a0).toFloat())
        val dirAngle = a0 + Math.PI / 2
        val dir = Offset(cos(dirAngle).toFloat(), sin(dirAngle).toFloat())
        val perp = Offset(-dir.y, dir.x)
        val tip = start + dir * (w * 0.15f)
        val half = w * 0.085f
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(start.x + perp.x * half, start.y + perp.y * half)
            lineTo(start.x - perp.x * half, start.y - perp.y * half)
            close()
        }
        drawPath(head, color)
    }
}

@Composable
fun MenuIcon(modifier: Modifier = Modifier, color: Color) {
    GeoIcon(modifier, color) { w, _ ->
        val cell = w * 0.30f
        val gap = w * 0.38f
        for ((dx, dy) in listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)) {
            drawRoundRect(
                color,
                topLeft = Offset(w * 0.16f + dx * gap, w * 0.16f + dy * gap),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.3f),
                style = stroke(w),
            )
        }
    }
}

/**
 * The sound switch's own mark: a speaker drawn in the app's house hand,
 * the same round-capped stroke as the chevron and the grid cells. Its two
 * waves hang on the mouth of the cone when sound is on; a balanced cross
 * takes their place when it is off. Drawn here like every other icon, no
 * pack and no font.
 */
@Composable
fun SpeakerIcon(modifier: Modifier = Modifier, on: Boolean, color: Color) {
    GeoIcon(modifier, color, size = 26.dp) { w, h ->
        val line = Stroke(w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cone = Path().apply {
            moveTo(w * 0.15f, h * 0.40f)
            lineTo(w * 0.30f, h * 0.40f)
            lineTo(w * 0.49f, h * 0.19f)
            lineTo(w * 0.49f, h * 0.81f)
            lineTo(w * 0.30f, h * 0.60f)
            lineTo(w * 0.15f, h * 0.60f)
            close()
        }
        drawPath(cone, color, style = line)
        if (on) {
            // Two waves, concentric on the mouth, each a little wider than
            // the last: nearer the cone they hug it, farther out they open.
            wave(color, line.width, w, h, radius = 0.175f, halfAngleDeg = 52f)
            wave(color, line.width, w, h, radius = 0.315f, halfAngleDeg = 58f)
        } else {
            val arm = w * 0.105f
            val cx = w * 0.755f
            val cy = h * 0.5f
            drawLine(color, Offset(cx - arm, cy - arm), Offset(cx + arm, cy + arm), line.width, line.cap)
            drawLine(color, Offset(cx + arm, cy - arm), Offset(cx - arm, cy + arm), line.width, line.cap)
        }
    }
}

/** One sound wave: an arc open to the right, centred on the cone's mouth. */
private fun DrawScope.wave(color: Color, strokeW: Float, w: Float, h: Float, radius: Float, halfAngleDeg: Float) {
    val r = w * radius
    drawArc(
        color,
        startAngle = -halfAngleDeg,
        sweepAngle = 2f * halfAngleDeg,
        useCenter = false,
        topLeft = Offset(w * 0.49f - r, h * 0.5f - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
    )
}

private fun stroke(w: Float) = Stroke(
    width = w * 0.11f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

@Composable
private fun GeoIcon(
    modifier: Modifier,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    content: DrawScope.(Float, Float) -> Unit,
) {
    Canvas(modifier = modifier.size(size)) {
        content(this.size.width, this.size.height)
    }
}
