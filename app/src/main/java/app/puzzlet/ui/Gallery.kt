package app.puzzlet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.puzzlet.R
import app.puzzlet.core.Scenes
import app.puzzlet.core.SceneSpec

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
 * The picture menu. Cards are pure pictures: a child who cannot read can
 * choose everything. A small honey dot marks a picture with unfinished work
 * waiting (this session).
 */
@Composable
fun Gallery(onChoose: (String) -> Unit, hasProgress: (String) -> Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMarkSmall()
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = PuzzletColors.Teal,
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PuzzletColors.Ink,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth < 480.dp -> 1
                maxWidth < 840.dp -> 2
                else -> 3
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(Scenes.all) { scene ->
                    SceneCard(
                        scene = scene,
                        showProgress = hasProgress(scene.id),
                        onClick = { onChoose(scene.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneCard(scene: SceneSpec, showProgress: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(PuzzletColors.Card)
            .clickable(onClick = onClick),
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxSize().padding(10.dp),
            cornerRadius = 20.dp,
        )
        if (showProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(PuzzletColors.Honey),
            )
        }
    }
}

/**
 * Choosing how many pieces. The numbers are for the parent reading over a
 * shoulder; the grid pictures are what a child actually taps.
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
            CircleButton(onClick = onBack, background = PuzzletColors.Card) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (difficulty in DIFFICULTIES) {
                DifficultyButton(difficulty) { onPlay(difficulty.rows, difficulty.cols) }
            }
        }
    }
}

@Composable
private fun DifficultyButton(difficulty: Difficulty, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(PuzzletColors.Card)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PieceGridIcon(rows = difficulty.rows, cols = difficulty.cols)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = difficulty.pieces.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = PuzzletColors.Ink,
        )
    }
}

/** The cut, drawn small: rows by cols tiles, the child's own preview. */
@Composable
private fun PieceGridIcon(rows: Int, cols: Int) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(44.dp),
    ) {
        val side = size.width
        val gap = side * 0.045f
        val cell = (side - gap * (cols - 1)) / cols
        val cellH = (side - gap * (rows - 1)) / rows
        for (r in 0 until rows) for (c in 0 until cols) {
            drawRoundRect(
                PuzzletColors.Teal,
                topLeft = androidx.compose.ui.geometry.Offset(c * (cell + gap), r * (cellH + gap)),
                size = androidx.compose.ui.geometry.Size(cell, cellH),
                cornerRadius = CornerRadius(cell * 0.24f),
            )
        }
    }
}

/** The launcher tile, small, for headers: the same committed PNG. */
@Composable
fun BrandMarkSmall() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(percent = 22))
            .background(PuzzletColors.Teal),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** A round pressable used across the app. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
