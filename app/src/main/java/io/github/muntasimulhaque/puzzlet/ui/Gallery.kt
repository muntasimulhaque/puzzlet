package io.github.muntasimulhaque.puzzlet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.host.ShelfState

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
        ChooserOverlay(
            openId = openId,
            shelf = shelf,
            onChoose = onChoose,
            onChooseAt = onChooseAt,
            onDismiss = { openId = null },
        )
    }
}

/** The open picture's cut chooser, or nothing at all. */
@Composable
private fun ChooserOverlay(
    openId: String?,
    shelf: ShelfState,
    onChoose: (String) -> Unit,
    onChooseAt: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val openScene = openId?.let { id -> Scenes.all.firstOrNull { it.id == id } } ?: return
    SceneChooser(
        scene = openScene,
        current = shelf.openingCount(openScene.id),
        onChoose = onChoose,
        onChooseAt = onChooseAt,
        onDismiss = onDismiss,
    )
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
