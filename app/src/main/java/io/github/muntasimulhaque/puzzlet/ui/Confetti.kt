package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.starPoints
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

internal fun DrawScope.drawConfetti(pieces: List<ConfettiPiece>, t: Float) {
    for (p in pieces) {
        val local = ((t - p.delay) / p.fall).coerceIn(0.0, 1.0)
        if (local <= 0.0) continue
        val y = ((-0.08 + local * 1.25) * size.height).toFloat()
        val x = ((p.x0 + p.sway * sin(local * p.freq * 2 * PI + p.phase)) * size.width).toFloat()
        val alpha = if (local > 0.82) ((1.0 - local) / 0.18).toFloat() else 1f
        val angle = (p.rot0 + t * p.spin).toFloat()
        withTransform({
            translate(x, y)
            rotate(angle)
        }) {
            when (p.kind) {
                0 -> drawCircle(p.color.copy(alpha = alpha), radius = p.size.toFloat())
                1 -> drawRoundRect(
                    p.color.copy(alpha = alpha),
                    topLeft = androidx.compose.ui.geometry.Offset(-p.size.toFloat(), -p.size.toFloat()),
                    size = androidx.compose.ui.geometry.Size(p.size.toFloat() * 2, p.size.toFloat() * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                )
                else -> p.shape?.let { drawPath(it, p.color.copy(alpha = alpha)) }
            }
        }
    }
}

private fun triangle(s: Float): Path = Path().apply {
    moveTo(0f, -s)
    lineTo(s * 0.9f, s * 0.7f)
    lineTo(-s * 0.9f, s * 0.7f)
    close()
}

private fun star(s: Float): Path = Path().apply {
    val pts = starPoints(Vec2(0.0, 0.0), s.toDouble(), (s * 0.45).toDouble(), 5)
    moveTo(pts[0].x.toFloat(), pts[0].y.toFloat())
    for (i in 1 until pts.size) lineTo(pts[i].x.toFloat(), pts[i].y.toFloat())
    close()
}

internal class ConfettiPiece(
    val x0: Double,
    val delay: Double,
    val fall: Double,
    val sway: Double,
    val freq: Double,
    val phase: Double,
    val rot0: Double,
    val spin: Double,
    val size: Double,
    val color: Color,
    val kind: Int,
) {
    /** Kinds 2 and 3 are a triangle or a star, built once, not per frame. */
    val shape: Path? = when (kind) {
        2 -> triangle(size.toFloat())
        3 -> star(size.toFloat())
        else -> null
    }
}

internal fun buildConfetti(seed: Long): List<ConfettiPiece> {
    val rnd = Random(seed + 31)
    val colors = listOf(PuzzletColors.Teal, PuzzletColors.Honey, PuzzletColors.Coral, PuzzletColors.Sky)
    return List(64) {
        ConfettiPiece(
            x0 = 0.05 + rnd.nextDouble() * 0.9,
            delay = rnd.nextDouble() * 0.25,
            fall = 0.85 + rnd.nextDouble() * 0.5,
            sway = 0.02 + rnd.nextDouble() * 0.05,
            freq = 1.0 + rnd.nextDouble() * 2.0,
            phase = rnd.nextDouble() * 2 * PI,
            rot0 = rnd.nextDouble() * 360,
            spin = (rnd.nextDouble() - 0.5) * 720,
            size = 5.0 + rnd.nextDouble() * 7.0,
            color = colors[rnd.nextInt(colors.size)],
            kind = rnd.nextInt(4),
        )
    }
}
