package io.github.muntasimulhaque.puzzlet.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    "kite" -> R.string.scene_kite
    "windmill" -> R.string.scene_windmill
    "beach" -> R.string.scene_beach
    "mushroom" -> R.string.scene_mushroom
    else -> R.string.app_name
}

/**
 * The picture shelf: one quiet name at top, then sixteen pictures with
 * their names (D-064, D-072) and one quiet count line each. Tapping a card
 * anywhere opens its cut chooser (D-065, D-070); a pick there plays and
 * remembers. The sound switch lives on the play screen now (D-077).
 *
 * [openChooserFor] starts with one picture's chooser open; it is the
 * capture harness's way to host that state without touch injection.
 */
@Composable
fun Gallery(
    shelf: ShelfState,
    onChoose: (String) -> Unit,
    onChooseAt: (String, Int) -> Unit,
    openChooserFor: String? = null,
) {
    // The saved shelf arrives in a few milliseconds. Until it does, the home
    // screen holds the paper ground instead of showing ladder defaults a card
    // might play at: nothing flashes, and no count is ever wrong on screen.
    if (!shelf.loaded) {
        Box(Modifier.fillMaxSize().background(PuzzletColors.Paper))
        return
    }
    var openId by rememberSaveable { mutableStateOf(openChooserFor) }
    // Back is the same answer as tapping the scrim: the chooser closes.
    BackHandler(enabled = openId != null) { openId = null }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShelfHeader()
            ShelfGrid(
                shelf = shelf,
                onOpen = { id -> openId = id },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        val openScene = openId?.let { id -> Scenes.all.firstOrNull { it.id == id } }
        if (openScene != null) {
            SceneChooser(
                scene = openScene,
                current = shelf.openingCount(openScene.id),
                onChoose = onChoose,
                onChooseAt = onChooseAt,
                onDismiss = { openId = null },
            )
        }
    }
}

/**
 * One picture's cut chooser. The marked tile is the count the game will
 * actually deal: a parent's pick, else the ladder's step for this picture's
 * wins. One value for the mark, the plain path and the card line, so the
 * marked tile never promises a count the game does not open. Tapping the
 * marked tile plays the plain path, so where nobody has picked, wins still
 * walk the ladder (D-047, D-065).
 */
@Composable
private fun SceneChooser(
    scene: SceneSpec,
    current: Int,
    onChoose: (String) -> Unit,
    onChooseAt: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    CutChooser(
        scene = scene,
        current = current,
        onPick = { pieces ->
            onDismiss()
            if (pieces == current) onChoose(scene.id) else onChooseAt(scene.id, pieces)
        },
        onDismiss = onDismiss,
    )
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
private fun ShelfHeader() {
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
        )
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
    nameSize: TextUnit,
    onOpen: () -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    val count = stringResource(R.string.pieces_count, pieces)
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .buttonShadow(shape)
            .clip(shape)
            .background(PuzzletColors.Card)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "$name, $count" }
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
        CardName(name = name, size = nameSize)
        QuietCount(pieces = pieces)
    }
}

@Composable
private fun CardName(name: String, size: TextUnit) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = size),
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
        color = PuzzletColors.Ink.copy(alpha = 0.70f),
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
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
            .clickable(role = Role.Button, onClick = onClick)
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
