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
    drawTray(game.tray, game.shelfAbove)
    drawBoard(game.board)
    drawPulse(game, pulseId, pulseT)
}

private val TRAY_RADIUS = 26.dp
private val TRAY_LIP = 2.5.dp

/**
 * The shelf: a warm tray that hangs from under the top bar when it stands
 * above the picture, or from the screen's left edge when it stands beside
 * it (D-087). Its corners turn off square on the side that faces the
 * picture, like a real felt tray, and its soft edge falls that same way,
 * so the shelf reads as one kind of object whichever way the field is
 * shaped. No hairline: the shadow is the seam now.
 */
private fun DrawScope.drawTray(tray: Area, above: Boolean) {
    val path = trayPath(tray, above, TRAY_RADIUS.toPx())
    val lip = TRAY_LIP.toPx()
    withTransform({ if (above) translate(0f, lip) else translate(lip, 0f) }) {
        drawPath(path, PuzzletColors.Ink.copy(alpha = 0.05f))
    }
    drawPath(path, PuzzletColors.Tray)
}

/** The tray's outline: flush on the two edges that meet the screen's own frame. */
private fun trayPath(tray: Area, above: Boolean, r: Float): Path {
    val x0 = tray.x.toFloat()
    val x1 = tray.maxX.toFloat()
    val y0 = tray.y.toFloat()
    val y1 = tray.maxY.toFloat()
    return Path().apply {
        if (above) {
            moveTo(x0, y0)
            lineTo(x1, y0)
            lineTo(x1, y1 - r)
            arcTo(Rect(x1 - 2f * r, y1 - 2f * r, x1, y1), 0f, 90f, forceMoveTo = false)
            lineTo(x0 + r, y1)
            arcTo(Rect(x0, y1 - 2f * r, x0 + 2f * r, y1), 90f, 90f, forceMoveTo = false)
            close()
        } else {
            moveTo(x0, y0)
            lineTo(x1 - r, y0)
            arcTo(Rect(x1 - 2f * r, y0, x1, y0 + 2f * r), 270f, 90f, forceMoveTo = false)
            lineTo(x1, y1 - r)
            arcTo(Rect(x1 - 2f * r, y1 - 2f * r, x1, y1), 0f, 90f, forceMoveTo = false)
            lineTo(x0, y1)
            close()
        }
    }
}

/**
 * The table the picture assembles on: one quiet linen square, exactly the
 * board the pieces snap to, wearing the tray's own soft lip so the two
 * surfaces are the same kind of thing. It used to be a hairline outline
 * drawn eight dp outside the board, which showed a frame a piece could not
 * reach and left the work with no place to happen (D-085). Still no
 * picture, no slot glow, nothing to copy: a surface, not a hint.
 */
private fun DrawScope.drawBoard(board: Area) {
    val r = BOARD_RADIUS.toPx()
    val lip = BOARD_LIP.toPx()
    val topLeft = Offset(board.x.toFloat(), board.y.toFloat())
    val size = Size(board.w.toFloat(), board.h.toFloat())
    withTransform({ translate(0f, lip) }) {
        drawRoundRect(PuzzletColors.Ink.copy(alpha = 0.05f), topLeft, size, CornerRadius(r))
    }
    drawRoundRect(PuzzletColors.Board, topLeft, size, CornerRadius(r))
}

private val BOARD_RADIUS = 24.dp
private val BOARD_LIP = 2.5.dp

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
