package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.PieceCut
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.cutSeedFor
import io.github.muntasimulhaque.puzzlet.core.stepForPieces
import kotlin.math.ceil

/** One cut of the picture, drawn the way the game will deal it. */
@Composable
internal fun CutTile(
    scene: SceneSpec,
    pieces: Int,
    marked: Boolean,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.pieces_count, pieces)
    val shape = RoundedCornerShape(18.dp)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(PuzzletColors.Tray)
                .then(
                    if (marked) {
                        Modifier.border(3.dp, PuzzletColors.Honey, shape)
                    } else {
                        Modifier
                    },
                )
                .semantics {
                    contentDescription = label
                    selected = marked
                }
                .clickable(role = Role.Button) { onPick(pieces) },
        ) {
            CutPreview(scene = scene, pieces = pieces)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = pieces.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (marked) PuzzletColors.Teal else PuzzletColors.Ink.copy(alpha = 0.72f),
        )
    }
}

/** The real cut, scored over the picture; the scene and paths are cached per tile size. */
@Composable
private fun CutPreview(scene: SceneSpec, pieces: Int) {
    Canvas(
        Modifier.fillMaxSize().drawWithCache {
            val side = ceil(size.width.toDouble()).toInt().coerceAtLeast(1)
            val image = sceneRaster(scene, side)
            val strokes = cutOverlayPaths(scene, pieces, size.width.toDouble())
            val ink = PuzzletColors.Ink.copy(alpha = 0.34f)
            val score = Stroke(1.6.dp.toPx())
            onDrawBehind {
                drawImage(image = image, dstSize = IntSize(side, side))
                for (path in strokes) drawPath(path, ink, style = score)
            }
        },
    ) { }
}

/** The whole picture scored by the real cut for [pieces], as overlay paths. */
private fun cutOverlayPaths(scene: SceneSpec, pieces: Int, sidePx: Double): List<Path> {
    if (sidePx <= 0.0) return emptyList()
    val step = stepForPieces(pieces)
    val cut = PieceCut.generate(
        step.rows, step.cols, sidePx, sidePx,
        cutSeedFor(scene.id, step.rows, step.cols),
    )
    return cut.shapes.mapIndexed { index, pieceShape ->
        // Piece outlines are piece-local (origin at the bbox corner): they
        // walk to their cell through span and offset, the same mapping the
        // game uses to seat a piece.
        val ox = (index % step.cols) * cut.cellW + pieceShape.offsetInCell.x
        val oy = (index / step.cols) * cut.cellH + pieceShape.offsetInCell.y
        outlinePath(pieceShape.segments).apply {
            translate(Offset(ox.toFloat(), oy.toFloat()))
        }
    }
}
