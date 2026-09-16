package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.host.ShelfState

@Composable
internal fun ShelfGrid(
    shelf: ShelfState,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val columns = when {
            maxWidth < 360.dp -> 1
            maxWidth < 840.dp -> 2
            else -> 3
        }
        // One size for every name: the shelf speaks in one voice, and the
        // longest name sets the size for all of them (owner's law). The
        // card's own 10 dp padding each side is what the names really have.
        val names = Scenes.all.map { stringResource(sceneNameRes(it.id)) }
        val cell = (maxWidth - 40.dp - 16.dp * (columns - 1)) / columns
        val nameSize = rememberNameFontSize(names, cell - 20.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(Scenes.all, key = { it.id }) { scene ->
                SceneCard(
                    scene = scene,
                    pieces = shelf.openingCount(scene.id),
                    won = (shelf.wins[scene.id] ?: 0) > 0,
                    nameSize = nameSize,
                    onOpen = { onOpen(scene.id) },
                )
            }
        }
    }
}

/** The smallest a shelf name may step down to, so it stays readable. */
private val MIN_NAME_SIZE = 16.sp

/**
 * One font size for every picture name, measured from the longest name in
 * the card's real width. The shelf used to clip the last letter of a long
 * name (Mushroom read as Mushroo, Lighthouse as Lighthous). Shrinking one
 * name alone would break the shelf's one voice, so the whole row of names
 * steps down together, never past [MIN_NAME_SIZE].
 */
@Composable
private fun rememberNameFontSize(names: List<String>, textWidth: Dp): TextUnit {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.titleLarge
    val density = LocalDensity.current
    return remember(names, textWidth, style, density) {
        // A hair of margin so a rounded pixel can never clip a name.
        val available = with(density) { textWidth.toPx() } * 0.98f
        val widest = names.maxOfOrNull { name ->
            measurer.measure(
                text = name,
                style = style,
                maxLines = 1,
                softWrap = false,
            ).size.width.toFloat()
        } ?: 0f
        if (widest <= available || widest == 0f) style.fontSize
        else (style.fontSize.value * (available / widest))
            .coerceAtLeast(MIN_NAME_SIZE.value)
            .sp
    }
}

/** One calm row: the shelf's word alone. The sound switch plays elsewhere. */
@Composable
internal fun ShelfHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = PuzzletColors.Ink,
            // The shelf's one landmark, so a TalkBack parent can jump past
            // the title instead of swiping through it every time.
            modifier = Modifier.semantics { heading() },
        )
    }
}
