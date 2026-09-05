package io.github.muntasimulhaque.puzzlet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Cubic
import io.github.muntasimulhaque.puzzlet.core.PieceCut
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.cutSeedFor

/** Difficulty ladder, youngest first. Rows x cols on a square board. */
data class Difficulty(val pieces: Int, val rows: Int, val cols: Int)

val DIFFICULTIES = listOf(
    Difficulty(4, 2, 2),
    Difficulty(6, 3, 2),
    Difficulty(9, 3, 3),
    Difficulty(12, 4, 3),
    Difficulty(16, 4, 4),
    Difficulty(20, 5, 4),
    Difficulty(24, 6, 4),
)

/**
 * Choosing how cut up the picture comes. Every row shows the picture cut
 * exactly as the game will cut it, same scene and same cut seed, so the
 * child picks by looking and the parent reads the count. Tapping plays.
 */
@Composable
fun DifficultyChooser(
    sceneId: String,
    onBack: () -> Unit,
    onPlay: (rows: Int, cols: Int) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(
                onClick = onBack,
                background = PuzzletColors.Card,
                label = stringResource(R.string.go_back),
            ) {
                BackIcon(color = PuzzletColors.Ink)
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxWidth * 0.86f, maxHeight * 0.92f)
            ScenePicture(
                spec = Scenes.byId(sceneId),
                modifier = Modifier.width(side),
                cornerRadius = 32.dp,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(DIFFICULTIES) { difficulty ->
                DifficultyRow(sceneId, difficulty) { onPlay(difficulty.rows, difficulty.cols) }
            }
        }
    }
}

@Composable
private fun DifficultyRow(sceneId: String, difficulty: Difficulty, onClick: () -> Unit) {
    val label = stringResource(R.string.play_option, stringResource(sceneNameRes(sceneId)), difficulty.pieces)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(PuzzletColors.Card)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CutPicture(
            sceneId = sceneId,
            rows = difficulty.rows,
            cols = difficulty.cols,
            modifier = Modifier.size(76.dp),
            cornerRadius = 20.dp,
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = difficulty.pieces.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = PuzzletColors.Teal,
            )
            Text(
                text = stringResource(R.string.pieces),
                style = MaterialTheme.typography.bodyMedium,
                color = PuzzletColors.Ink,
            )
        }
    }
}

/** The picture with its true cut hairlines over it. */
@Composable
private fun CutPicture(sceneId: String, rows: Int, cols: Int, modifier: Modifier, cornerRadius: androidx.compose.ui.unit.Dp) {
    val spec = remember(sceneId) { Scenes.byId(sceneId) }
    val cut = remember(sceneId, rows, cols) { PieceCut.generate(rows, cols, 1.0, 1.0, cutSeedFor(sceneId, rows, cols)) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        ScenePicture(spec = spec, modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width.toDouble()
            for ((i, shape) in cut.shapes.withIndex()) {
                val ox = (i % cols) * cut.cellW + shape.offsetInCell.x
                val oy = (i / cols) * cut.cellH + shape.offsetInCell.y
                val absolute = shape.segments.map { seg ->
                    Cubic(
                        (seg.p0 + Vec2(ox, oy)) * s,
                        (seg.c1 + Vec2(ox, oy)) * s,
                        (seg.c2 + Vec2(ox, oy)) * s,
                        (seg.p1 + Vec2(ox, oy)) * s,
                    )
                }
                val path = outlinePath(absolute)
                drawPath(path, PuzzletColors.Card.copy(alpha = 0.85f), style = Stroke(width = 3.4.dp.toPx()))
                drawPath(path, PuzzletColors.Ink.copy(alpha = 0.38f), style = Stroke(width = 1.8.dp.toPx()))
            }
        }
    }
}
