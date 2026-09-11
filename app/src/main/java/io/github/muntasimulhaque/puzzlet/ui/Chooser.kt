package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.PIECE_COUNTS
import io.github.muntasimulhaque.puzzlet.core.PieceCut
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.cutSeedFor
import io.github.muntasimulhaque.puzzlet.core.stepForPieces
import kotlin.math.ceil

/**
 * The cut chooser (D-065): the child taps a picture, and the sizes the
 * picture comes in rise from the bottom as five tiles, each one the real
 * picture cut the real way it will be dealt (same cut seed as the game,
 * the D-042 truth kept). A three-year-old reads the tiles by look: four
 * big pieces or sixteen small ones; the numeral beneath speaks to the
 * parent. Tapping a tile plays at once and remembers it; the tile already
 * marked plays through the plain path, so where nobody has picked, wins
 * still walk the ladder (D-047). Tapping anywhere else puts it away.
 */
@Composable
fun CutChooser(
    scene: SceneSpec,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // It rises once, quickly: scrim and plate arrive together, the plate
    // a half step up as it comes. Leaving is at once: a tap is an answer.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(220, easing = LinearOutSlowInEasing)) }
    val dismissLabel = stringResource(R.string.close)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = rise.value }
            .background(PuzzletColors.Scrim)
            .semantics { contentDescription = dismissLabel }
            .clickable(role = Role.Button, onClick = onDismiss),
    ) {
        ChooserPlate(scene = scene, current = current, rise = rise, onPick = onPick)
    }
}

/** The plate itself: the picture's name, the question, and the five real cuts. */
@Composable
private fun BoxScope.ChooserPlate(
    scene: SceneSpec,
    current: Int,
    rise: Animatable<Float, AnimationVector1D>,
    onPick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .graphicsLayer {
                translationY = (1f - rise.value) * 56.dp.toPx()
            }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(PuzzletColors.Card)
            // Swallow taps on the plate without adding a semantics node.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(sceneNameRes(scene.id)),
            style = MaterialTheme.typography.titleLarge,
            color = PuzzletColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.choose_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = PuzzletColors.Ink.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (pieces in PIECE_COUNTS) {
                CutTile(
                    scene = scene,
                    pieces = pieces,
                    marked = pieces == current,
                    onPick = onPick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One cut of the picture, drawn the way the game will deal it. */
@Composable
private fun CutTile(
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
