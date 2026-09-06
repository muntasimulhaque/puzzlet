package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Cubic
import io.github.muntasimulhaque.puzzlet.core.Piece
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Vec2

internal fun outlinePath(segments: List<Cubic>): Path {
    val path = Path()
    var first = true
    for (seg in segments) {
        if (first) {
            path.moveTo(seg.p0.x.toFloat(), seg.p0.y.toFloat())
            first = false
        }
        path.cubicTo(
            seg.c1.x.toFloat(), seg.c1.y.toFloat(),
            seg.c2.x.toFloat(), seg.c2.y.toFloat(),
            seg.p1.x.toFloat(), seg.p1.y.toFloat(),
        )
    }
    path.close()
    return path
}

/**
 * Everything behind the pieces: the tray, the board's frame, and the ring
 * that answers a click home. No picture and no slot glow: the table is
 * blank, and the only thing that lights up is what the child just did.
 */
@Composable
internal fun BoardBackdrop(
    game: Puzzle,
    pulseId: Int,
    pulseT: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        drawBackdrop(game, pulseId, pulseT)
    }
}

internal fun DrawScope.drawBackdrop(game: Puzzle, pulseId: Int, pulseT: Float) {
    drawTray(game.tray)
    drawMat(game.board)
    drawPulse(game, pulseId, pulseT)
}

private fun DrawScope.drawTray(tray: Area) {
    drawRect(
        PuzzletColors.Tray,
        topLeft = Offset(tray.x.toFloat(), tray.y.toFloat()),
        size = Size(tray.w.toFloat(), tray.h.toFloat()),
    )
    drawLine(
        PuzzletColors.Ink.copy(alpha = 0.08f),
        start = Offset(0f, tray.maxY.toFloat()),
        end = Offset(tray.w.toFloat(), tray.maxY.toFloat()),
        strokeWidth = 1.dp.toPx(),
    )
}

private fun DrawScope.drawMat(board: Area) {
    val mat = 8.dp.toPx()
    drawRoundRect(
        PuzzletColors.Card,
        topLeft = Offset((board.x - mat).toFloat(), (board.y - mat + 1.dp.toPx()).toFloat()),
        size = Size((board.w + 2 * mat).toFloat(), (board.h + 2 * mat).toFloat()),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )
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

/**
 * One piece slice, drawn in piece local coordinates. The caller scales the
 * scope (tray scale in the shelf, full size in hand), so this stays exact
 * at any size with no shared cache to go stale.
 */
internal fun DrawScope.drawSlice(
    piece: Piece,
    path: Path,
    scene: SceneSpec,
    board: Area,
    shadowAlpha: Float,
    outlineAlpha: Float,
) {
    if (shadowAlpha > 0f) {
        withTransform({ translate(0f, 3f) }) {
            drawPath(path, PuzzletColors.Ink.copy(alpha = shadowAlpha))
        }
    }
    clipPath(path) {
        withTransform({
            translate(
                (board.x - piece.home.x).toFloat(),
                (board.y - piece.home.y).toFloat(),
            )
        }) {
            drawScene(scene, board.w)
        }
    }
    drawPath(path, PuzzletColors.Card, style = Stroke(3.2.dp.toPx()))
    drawPath(path, PuzzletColors.Ink.copy(alpha = outlineAlpha), style = Stroke(1.6.dp.toPx()))
}

internal fun pieceTargetTopLeft(piece: Piece, scale: Float): Vec2 {
    val c = piece.currentCenter
    return Vec2(c.x - piece.size.x * scale / 2.0, c.y - piece.size.y * scale / 2.0)
}
