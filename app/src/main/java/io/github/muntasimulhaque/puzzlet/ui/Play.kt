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
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Vec2

/**
 * What the play field needs from the world. No composable takes a ViewModel
 * (the house rule): the activity wires these to the host, and the screenshot
 * harness passes no-ops, which is what keeps captures flake-free. onLayout
 * hands the host the field and the board-size cap; the tray and the board
 * are decided in core (core/Layout.kt), so tests and captures agree. A drag
 * is the field's own business while it moves (D-055): onGrabAt reports what
 * was picked up, the finger draws it, and onDropAt commits once, at release.
 */
class PlayActions(
    val onGrabAt: (Vec2, Double) -> Int?,
    val onDropAt: (Vec2) -> Boolean,
    val onLayout: (Area, Double) -> Unit,
    val onRestart: () -> Unit,
)

/**
 * The play field: a shelf above, a board below. The board stays blank, the
 * way a table does, and the picture lives behind one coin in the top bar
 * (D-048): look, then put it away. The sound switch shares that bar,
 * beside the picture coin (D-077, D-078). Each piece is its own tile
 * (one small Canvas per piece), and one tile lives for the whole game
 * (D-055), so a grab, a release and a reorder never rebuild a piece mid
 * flight.
 */
@Composable
fun PlayScreen(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    restartAt: Long,
    peeking: Boolean,
    celebrating: Boolean,
    soundOn: Boolean,
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    fun requestBack() {
        when {
            peeking -> onPeek(false)
            confirming -> onBack()
            !game.completed && game.placedCount > 0 -> confirming = true
            else -> onBack()
        }
    }
    BackHandler(onBack = ::requestBack)
    Box(modifier = Modifier.fillMaxSize().background(PuzzletColors.Paper)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayTopBar(game, peeking, soundOn, onPeek, onSound, ::requestBack)
            PlayField(
                game, draggedId, pulseId, pulseAt, restartAt, peeking, actions, onPeek,
                Modifier.fillMaxWidth().weight(1f),
            )
        }
        // The finish owns the whole screen. The picture panel stays inside
        // the field, so the top bar and the picture coin's on state stay in
        // view while the child looks at the picture (D-048, D-080).
        PlayOverlays(celebrating, confirming, game, actions.onRestart, onBack, { confirming = false })
    }
}

/** The two moments that cover the field: the finish, and the question. */
@Composable
private fun PlayOverlays(
    celebrating: Boolean,
    confirming: Boolean,
    game: Puzzle,
    onAgain: () -> Unit,
    onHome: () -> Unit,
    onStay: () -> Unit,
) {
    if (celebrating) {
        Celebration(game = game, onAgain = onAgain, onHome = onHome)
    }
    if (confirming) {
        LeaveConfirm(onStay = onStay, onLeave = onHome)
    }
}
