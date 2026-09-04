package app.puzzlet.ui

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

/** The sound switch on the shelf: speaker with two waves when on. */
@Composable
fun SoundOnIcon(modifier: Modifier = Modifier, color: Color) {
    GeoIcon(modifier, color) { w, h ->
        val speaker = Path().apply {
            moveTo(w * 0.10f, h * 0.38f)
            lineTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.48f, h * 0.20f)
            lineTo(w * 0.48f, h * 0.80f)
            lineTo(w * 0.28f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.62f)
            close()
        }
        drawPath(speaker, color)
        val cy = h * 0.5f
        drawArc(
            color,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.44f - w * 0.10f, cy - w * 0.10f),
            size = Size(w * 0.20f, w * 0.20f),
            style = stroke(w),
        )
        drawArc(
            color,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.44f - w * 0.19f, cy - w * 0.19f),
            size = Size(w * 0.38f, w * 0.38f),
            style = stroke(w),
        )
    }
}

/** The sound switch, off: the same speaker under a calm slash. */
@Composable
fun SoundOffIcon(modifier: Modifier = Modifier, color: Color) {
    GeoIcon(modifier, color) { w, h ->
        val speaker = Path().apply {
            moveTo(w * 0.10f, h * 0.38f)
            lineTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.48f, h * 0.20f)
            lineTo(w * 0.48f, h * 0.80f)
            lineTo(w * 0.28f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.62f)
            close()
        }
        drawPath(speaker, color)
        val line = Path().apply {
            moveTo(w * 0.58f, h * 0.32f)
            lineTo(w * 0.90f, h * 0.68f)
        }
        drawPath(line, color, style = stroke(w))
    }
}

private fun stroke(w: Float) = Stroke(
    width = w * 0.11f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

@Composable
private fun GeoIcon(modifier: Modifier, color: Color, content: DrawScope.(Float, Float) -> Unit) {
    Canvas(modifier = modifier.size(24.dp)) {
        content(size.width, size.height)
    }
}
