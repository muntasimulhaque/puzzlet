package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.PIECE_COUNTS
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.STEPS
import io.github.muntasimulhaque.puzzlet.host.ShelfState

/** Spoken and printed picture names. The child taps the picture; the parent reads the word. */
internal fun sceneNameRes(sceneId: String): Int = when (sceneId) {
    "sail" -> R.string.scene_sail
    "house" -> R.string.scene_house
    "balloon" -> R.string.scene_balloon
    "fruit" -> R.string.scene_fruit
    "train" -> R.string.scene_train
    "castle" -> R.string.scene_castle
    "rocket" -> R.string.scene_rocket
    "lighthouse" -> R.string.scene_lighthouse
    "truck" -> R.string.scene_truck
    "plane" -> R.string.scene_plane
    "flowers" -> R.string.scene_flowers
    "icecream" -> R.string.scene_icecream
    else -> R.string.app_name
}

/**
 * The picture shelf: one quiet name at top with the sound coin docked
 * beside it, then pictures with their names and one row of piece counts
 * under each, so a parent sets the size and the child taps the picture.
 * The sound switch lives in the header, never over the pictures and never
 * behind a gate (D-021, D-046, D-057).
 */
@Composable
fun Gallery(
    shelf: ShelfState,
    onChoose: (String) -> Unit,
    onChooseAt: (String, Int) -> Unit,
    onSound: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        ShelfHeader(soundOn = shelf.soundOn, onSound = onSound)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val columns = when {
                maxWidth < 480.dp -> 1
                maxWidth < 840.dp -> 2
                else -> 3
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(Scenes.all) { scene ->
                    SceneCard(
                        scene = scene,
                        pieces = shelf.pieces[scene.id] ?: STEPS.first().pieces,
                        onChoose = { onChoose(scene.id) },
                        onChooseAt = { onChooseAt(scene.id, it) },
                    )
                }
            }
        }
    }
}

/** One calm row: the name on the left, the sound coin docked on the right. */
@Composable
private fun ShelfHeader(soundOn: Boolean, onSound: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = PuzzletColors.Ink,
            modifier = Modifier.weight(1f),
        )
        SoundCoin(on = soundOn, onToggle = onSound)
    }
}

@Composable
private fun SceneCard(
    scene: SceneSpec,
    pieces: Int,
    onChoose: () -> Unit,
    onChooseAt: (Int) -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    Column(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(PuzzletColors.Card)
            .semantics { contentDescription = name }
            .clickable(onClick = onChoose)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = PuzzletColors.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        StepRow(current = pieces, onChooseAt = onChooseAt)
    }
}

/** The five sizes a picture comes in; the current one is filled in. */
@Composable
private fun StepRow(current: Int, onChooseAt: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (pieces in PIECE_COUNTS) {
            StepChip(
                pieces = pieces,
                selected = pieces == current,
                onChoose = { onChooseAt(pieces) },
            )
        }
    }
}

@Composable
private fun StepChip(pieces: Int, selected: Boolean, onChoose: () -> Unit) {
    val label = stringResource(R.string.pieces_count, pieces)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PuzzletColors.Teal else PuzzletColors.Tray)
            .semantics { contentDescription = label }
            .clickable(onClick = onChoose),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pieces.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) PuzzletColors.Paper else PuzzletColors.Ink,
        )
    }
}

/** The sound switch: one quiet coin in the header, never over the pictures. */
@Composable
private fun SoundCoin(on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    CircleButton(
        onClick = { onToggle(!on) },
        background = PuzzletColors.Card,
        size = 48.dp,
        label = stringResource(if (on) R.string.sound_on else R.string.sound_off),
        modifier = modifier.shadow(4.dp, CircleShape),
    ) {
        SpeakerIcon(on = on, color = PuzzletColors.Ink)
    }
}

/** A round pressable used across the app. [label] names it for TalkBack. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
