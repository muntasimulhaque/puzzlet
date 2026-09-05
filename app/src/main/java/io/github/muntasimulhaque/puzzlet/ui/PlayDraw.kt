package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

internal fun DrawScope.drawBoard(
    game: Puzzle,
    scene: SceneSpec,
    paths: Map<Int, Path>,
    ghostAlpha: Float,
    dragId: Int?,
    ringAlpha: Float,
    pulseId: Int,
    pulseT: Float,
    liftV: Float,
    returning: Set<Int>,
) {
    val board = game.board
    val tray = game.tray
    val boardR = Rect(
        board.x.toFloat(), board.y.toFloat(),
        (board.x + board.w).toFloat(), (board.y + board.h).toFloat(),
    )

    // The tray: a deeper paper, so the shelf reads as a surface of its own.
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

    // The mat: a clear frame, so the board reads as a place to fill.
    val mat = 8.dp.toPx()
    drawRoundRect(
        PuzzletColors.Ink.copy(alpha = 0.18f),
        topLeft = Offset((board.x - mat).toFloat(), (board.y - mat).toFloat()),
        size = Size((board.w + 2 * mat).toFloat(), (board.h + 2 * mat).toFloat()),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )

    // The faint whole picture: where every piece is going, always visible.
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.alpha = ghostAlpha
        canvas.saveLayer(boardR, paint)
    }
    withTransform({ translate(board.x.toFloat(), board.y.toFloat()) }) {
        drawScene(scene, board.w)
    }
    drawIntoCanvas { it.restore() }

    for (piece in game.pieces) {
        if (!piece.placed) continue
        val path = paths[piece.id] ?: continue
        drawPiece(
            piece = piece,
            path = path,
            at = piece.current,
            scene = scene,
            board = board,
            scale = 1f,
            shadowAlpha = 0f,
            outlineAlpha = 0.16f,
        )
    }

    for (piece in game.pieces) {
        if (piece.placed || piece.id == dragId || piece.id in returning) continue
        val path = paths[piece.id] ?: continue
        drawPiece(
            piece = piece,
            path = path,
            at = piece.current,
            scene = scene,
            board = board,
            scale = game.trayScale.toFloat(),
            shadowAlpha = 0.16f,
            outlineAlpha = 0.28f,
        )
    }

    // Missed pieces glide home at full scale, then settle into the tray.
    for (piece in game.pieces) {
        if (piece.placed || piece.id == dragId || piece.id !in returning) continue
        val path = paths[piece.id] ?: continue
        drawPiece(
            piece = piece,
            path = path,
            at = piece.current,
            scene = scene,
            board = board,
            scale = 1f,
            shadowAlpha = 0.14f,
            outlineAlpha = 0.20f,
        )
    }

    // While a piece is held, its slot glows softly: the piece shows you
    // where it wants to go. A helper for the youngest hands only; on the
    // higher ladders the child answers that question themselves (AGENTS.md,
    // the original goal: the puzzle must make the child think).
    val dragged = dragId?.let { game.piece(it) }
    if (dragged != null && game.rows * game.cols <= 9) {
        drawRoundRect(
            PuzzletColors.Honey.copy(alpha = ringAlpha),
            topLeft = Offset(dragged.home.x.toFloat(), dragged.home.y.toFloat()),
            size = Size(dragged.size.x.toFloat(), dragged.size.y.toFloat()),
            cornerRadius = CornerRadius(10f),
            style = Stroke(width = 4.dp.toPx()),
        )
    }

    // The click: a short ring burst where a piece just landed.
    if (pulseId >= 0) {
        val landed = game.piece(pulseId)
        if (landed != null && pulseT < 1f) {
            val c = landed.homeCenter
            val r = (landed.halfDiagonal * (0.5f + pulseT)).toFloat()
            drawCircle(
                PuzzletColors.Honey.copy(alpha = (1f - pulseT) * 0.7f),
                radius = r,
                center = Offset(c.x.toFloat(), c.y.toFloat()),
                style = Stroke(width = 4f * (1f - pulseT) + 1f),
            )
        }
    }

    if (dragged != null) {
        val path = paths[dragged.id]
        if (path != null) {
            drawPiece(
                piece = dragged,
                path = path,
                at = dragged.current,
                scene = scene,
                board = board,
                scale = liftV,
                shadowAlpha = 0.24f,
                outlineAlpha = 0.28f,
            )
        }
    }
}

internal fun DrawScope.drawPiece(
    piece: Piece,
    path: Path,
    at: Vec2,
    scene: SceneSpec,
    board: Area,
    scale: Float,
    shadowAlpha: Float,
    outlineAlpha: Float,
) {
    val ax = at.x.toFloat()
    val ay = at.y.toFloat()
    withTransform({
        translate(ax, ay)
        if (scale != 1f) {
            scale(scale, scale, pivot = Offset(piece.size.x.toFloat() / 2f, piece.size.y.toFloat() / 2f))
        }
    }) {
        if (shadowAlpha > 0f) {
            withTransform({ translate(0f, 3f) }) {
                drawPath(path, PuzzletColors.Ink.copy(alpha = shadowAlpha))
            }
        }
        clipPath(path) {
            // The piece carries its own slice of the picture: draw the scene
            // as if the piece sat at home (scene at board, outline at home),
            // and the outer transform (position plus draw scale) moves and
            // scales the whole thing. Using the current position here would
            // double-count the move and leave the silhouette empty.
            withTransform({
                translate(
                    (board.x - piece.home.x).toFloat(),
                    (board.y - piece.home.y).toFloat(),
                )
            }) {
                drawScene(scene, board.w)
            }
        }
        // A paper halo first, so pale pieces pop on tray and board,
        // then the ink edge that reads as the cut.
        drawPath(path, PuzzletColors.Card, style = Stroke(3.2.dp.toPx()))
        drawPath(path, PuzzletColors.Ink.copy(alpha = outlineAlpha), style = Stroke(1.6.dp.toPx()))
    }
}
