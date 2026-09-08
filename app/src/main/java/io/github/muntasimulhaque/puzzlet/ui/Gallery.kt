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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
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
 * beside it, then twelve pictures with their names (D-064) and one quiet
 * count line each. Tapping a card anywhere opens its cut chooser (D-065,
 * D-070); a pick there plays and remembers. The sound switch lives in
 * the header, never over the pictures and never behind a gate (D-021,
 * D-046, D-057).
 *
 * [openChooserFor] starts with one picture's chooser open; it is the
 * capture harness's way to host that state without touch injection.
 */
@Composable
fun Gallery(
    shelf: ShelfState,
    onChoose: (String) -> Unit,
    onChooseAt: (String, Int) -> Unit,
    onSound: (Boolean) -> Unit,
    openChooserFor: String? = null,
) {
    var openId by rememberSaveable { mutableStateOf(openChooserFor) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShelfHeader(soundOn = shelf.soundOn, onSound = onSound)
            ShelfGrid(
                shelf = shelf,
                onOpen = { id -> openId = id },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        val openScene = openId?.let { id -> Scenes.all.firstOrNull { it.id == id } }
        if (openScene != null) {
            // The marked tile is the count the game will actually deal: a
            // parent's pick, else the ladder's step for this picture's wins.
            // One value for the mark, the plain path and the card line, so
            // the marked tile never promises a count the game does not open.
            val currentCount = shelf.openingCount(openScene.id)
            CutChooser(
                scene = openScene,
                current = currentCount,
                onPick = { pieces ->
                    openId = null
                    if (pieces == currentCount) {
                        onChoose(openScene.id)
                    } else {
                        onChooseAt(openScene.id, pieces)
                    }
                },
                onDismiss = { openId = null },
            )
        }
    }
}

@Composable
private fun ShelfGrid(
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
                    pieces = shelf.openingCount(scene.id),
                    onOpen = { onOpen(scene.id) },
                )
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

/**
 * One picture card: the picture, its name, and the quiet line naming the
 * count it opens at. The whole card is one button (D-070): picture, name
 * and count all open the chooser, so a three-year-old never has to find
 * the picture inside the plate. TalkBack speaks the name once for the
 * whole card; the sizes it can play at live in the chooser it opens.
 */
@Composable
private fun SceneCard(
    scene: SceneSpec,
    pieces: Int,
    onOpen: () -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .buttonShadow(shape)
            .clip(shape)
            .background(PuzzletColors.Card)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = name }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        ) {
            ScenePicture(spec = scene, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp)
        }
        CardName(name = name)
        QuietCount(pieces = pieces)
    }
}

@Composable
private fun CardName(name: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        color = PuzzletColors.Ink,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** The quiet line under a name: the count this picture opens at. */
@Composable
private fun QuietCount(pieces: Int) {
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.pieces_count, pieces),
        style = MaterialTheme.typography.bodyMedium,
        color = PuzzletColors.Ink.copy(alpha = 0.60f),
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** The sound switch: one quiet coin in the header, never over the pictures. */
@Composable
private fun SoundCoin(on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    CircleButton(
        onClick = { onToggle(!on) },
        background = PuzzletColors.Card,
        size = 48.dp,
        label = stringResource(if (on) R.string.sound_on else R.string.sound_off),
        modifier = modifier,
    ) {
        SpeakerIcon(on = on, color = PuzzletColors.Ink)
    }
}

/**
 * One floating shadow for every button in the app (D-061, D-070): the
 * round coins and the shelf cards lift off the paper by the same ink
 * shadow, so a button never melts into the ground it sits on and every
 * button in the app reads as the same kind of thing.
 */
internal fun Modifier.buttonShadow(shape: Shape, elevation: Dp = 6.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = PuzzletColors.Ink.copy(alpha = 0.10f),
    spotColor = PuzzletColors.Ink.copy(alpha = 0.18f),
)

/**
 * A round pressable used across the app: the floating shadow is baked in
 * here (D-070), so every coin wears it and no caller can forget it.
 * [label] names it for TalkBack.
 */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .buttonShadow(CircleShape)
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
