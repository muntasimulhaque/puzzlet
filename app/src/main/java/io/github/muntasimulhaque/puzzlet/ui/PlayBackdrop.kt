package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle

/**
 * Everything behind the pieces: the tray, the board's frame, and the ring
 * that answers a click home. No picture and no slot glow: the table is
 * blank, and the only thing that lights up is what the child just did.
 */
@Composable
internal fun BoardBackdrop(
    game: Puzzle,
    pulseId: Int,
    pulse: State<Float>,
) {
    // The pulse value is read inside the draw lambda, so a landing pulse
    // redraws this one canvas and never recomposes the pieces above it.
    Canvas(Modifier.fillMaxSize()) {
        drawBackdrop(game, pulseId, pulse.value)
    }
}

internal fun DrawScope.drawBackdrop(game: Puzzle, pulseId: Int, pulseT: Float) {
    drawTray(game.tray)
    drawMat(game.board)
    drawPulse(game, pulseId, pulseT)
}

private fun DrawScope.drawTray(tray: Area) {
    // The shelf: a warm tray that hangs from under the top bar, its bottom
    // corners turned off square like a real felt tray, with one soft edge
    // beneath so it sits a hair above the table. No hairline: the shadow
    // is the seam now.
    val r = TRAY_RADIUS.toPx()
    val x0 = tray.x.toFloat()
    val x1 = tray.maxX.toFloat()
    val y0 = tray.y.toFloat()
    val y1 = tray.maxY.toFloat()
    val path = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y0)
        lineTo(x1, y1 - r)
        arcTo(Rect(x1 - 2f * r, y1 - 2f * r, x1, y1), 0f, 90f, forceMoveTo = false)
        lineTo(x0 + r, y1)
        arcTo(Rect(x0, y1 - 2f * r, x0 + 2f * r, y1), 90f, 90f, forceMoveTo = false)
        close()
    }
    val shelfShadow = 2.5.dp.toPx()
    withTransform({ translate(0f, shelfShadow) }) {
        drawPath(path, PuzzletColors.Ink.copy(alpha = 0.05f))
    }
    drawPath(path, PuzzletColors.Tray)
}

private val TRAY_RADIUS = 26.dp

private fun DrawScope.drawMat(board: Area) {
    val mat = 8.dp.toPx()
    drawRoundRect(
        PuzzletColors.Ink.copy(alpha = 0.10f),
        topLeft = Offset((board.x - mat).toFloat(), (board.y - mat).toFloat()),
        size = Size((board.w + 2 * mat).toFloat(), (board.h + 2 * mat).toFloat()),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun DrawScope.drawPulse(game: Puzzle, pulseId: Int, pulseT: Float) {
    if (pulseId < 0 || pulseT >= 1f) return
    val landed = game.piece(pulseId) ?: return
    val c = landed.homeCenter
    val r = (landed.halfDiagonal * (0.5f + pulseT)).toFloat()
    drawCircle(
        PuzzletColors.Honey.copy(alpha = (1f - pulseT) * 0.7f),
        radius = r,
        center = Offset(c.x.toFloat(), c.y.toFloat()),
        style = Stroke(width = 4f * (1f - pulseT) + 1f),
    )
}
