package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Cubic
import io.github.muntasimulhaque.puzzlet.core.Piece
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
 * One piece slice, drawn in piece local coordinates. The caller scales the
 * scope (tray scale in the shelf, full size in hand), so this stays exact
 * at any size with no shared cache to go stale.
 *
 * Every piece is die-cut: artwork, one narrow paper edge as the cut
 * thickness, and a soft two-pass shadow beneath. There is no dark outline:
 * the ink score read as a cartoon border, so a piece now stands off the
 * tray and the table by its edge and shadow alone (D-057).
 */
internal fun DrawScope.drawSlice(
    piece: Piece,
    path: Path,
    scene: SceneSpec,
    board: Area,
    lifted: Boolean,
) {
    // Two offsets at a gentle alpha read as one soft shadow. In hand the
    // piece is off the table, so its shadow drops deeper.
    val drop = (if (lifted) 7.dp else 5.dp).toPx()
    shadowPass(path, drop, SHADOW_ALPHA)
    shadowPass(path, drop * 0.5f, SHADOW_ALPHA * 0.7f)
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
    drawPath(path, PuzzletColors.Card, style = Stroke(RIM_STROKE.toPx()))
}

private fun DrawScope.shadowPass(path: Path, drop: Float, alpha: Float) {
    withTransform({ translate(0f, drop) }) {
        drawPath(path, PuzzletColors.Ink.copy(alpha = alpha))
    }
}

private val RIM_STROKE = 2.0.dp
private const val SHADOW_ALPHA = 0.13f

internal fun Vec2.toOffset(): Offset = Offset(x.toFloat(), y.toFloat())
