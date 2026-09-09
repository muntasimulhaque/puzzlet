package io.github.muntasimulhaque.puzzlet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
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
        if (celebrating) {
            Celebration(game, onAgain = actions.onRestart, onHome = onBack)
        }
        if (confirming) {
            LeaveConfirm(onStay = { confirming = false }, onLeave = onBack)
        }
    }
}

@Composable
private fun PlayField(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    restartAt: Long,
    peeking: Boolean,
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val capPx = with(density) { 560.dp.toPx() }.toDouble()
        val hitPx = with(density) { 44.dp.toPx() }.toDouble()
        val field = Area(0.0, 0.0, constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble())
        LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
            actions.onLayout(field, capPx)
        }
        val pulse = remember { Animatable(1f) }
        LaunchedEffect(pulseAt) {
            if (pulseId >= 0) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(380, easing = LinearOutSlowInEasing))
            }
        }
        // The held piece's centre, written straight from the pointer. It is
        // read at draw time by the one held tile, so a drag never recomposes
        // the field; the game state hears about it once, at release (D-055).
        val heldCenter = remember { mutableStateOf<Vec2?>(null) }
        GestureBoard(
            game, draggedId, pulseId, pulse.asState(), restartAt, peeking, hitPx, heldCenter, actions, onPeek,
        )
    }
}

@Composable
private fun GestureBoard(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulse: State<Float>,
    restartAt: Long,
    peeking: Boolean,
    hitRadiusPx: Double,
    heldCenter: MutableState<Vec2?>,
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    val progress = stringResource(R.string.puzzle_progress, game.placedCount, game.pieces.size)
    Box(
        Modifier
            .fillMaxSize()
            // The field speaks its progress, and announces each change as a
            // polite live region, so a TalkBack player hears a piece land.
            .semantics {
                contentDescription = progress
                liveRegion = LiveRegionMode.Polite
            }
            .fieldGestures(game, peeking, hitRadiusPx, heldCenter, actions),
    ) {
        BoardBackdrop(game, pulseId, pulse)
        PieceLayer(game, scene, draggedId, restartAt, heldCenter)
        if (peeking && !game.completed) {
            PeekPanel(scene, onDismiss = { onPeek(false) })
        }
    }
}

/**
 * The finished picture, held up over the field on a deep scrim. Tapping
 * anywhere on the field puts it away: one rule, the biggest target there
 * is. The top bar stays above the scrim, so the picture coin keeps showing
 * its own on state (the picture's wash, D-080) while the child looks.
 */
@Composable
private fun PeekPanel(scene: SceneSpec, onDismiss: () -> Unit) {
    val label = stringResource(R.string.peek_hide)
    // The panel rises: scrim and picture fade up together and the picture
    // grows a half step into place. It leaves when asked, at once: a tap is
    // an answer, not a request.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(200, easing = LinearOutSlowInEasing)) }
    Box(
        modifier = Modifier
            .zIndex(4f) // above every piece tile (a held piece rides at 2)
            .fillMaxSize()
            .graphicsLayer { alpha = rise.value }
            .background(PuzzletColors.Scrim)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val side = minOf(maxWidth * 0.78f, maxHeight * 0.78f)
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = 0.94f + 0.06f * rise.value
                        scaleY = 0.94f + 0.06f * rise.value
                    }
                    .background(PuzzletColors.Card, RoundedCornerShape(30.dp))
                    .padding(9.dp),
            ) {
                ScenePicture(
                    spec = scene,
                    modifier = Modifier.width(side),
                    cornerRadius = 22.dp,
                )
            }
        }
    }
}

/** One size for every coin in the play top bar, so back, picture and
 *  sound can never drift apart (D-078). */
private val TOP_BAR_COIN = 48.dp

/** Back on the left; the picture coin and the sound switch on the right. */
@Composable
private fun PlayTopBar(
    game: Puzzle,
    peeking: Boolean,
    soundOn: Boolean,
    onPeek: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleButton(
            onClick = onBack,
            background = PuzzletColors.Card,
            size = TOP_BAR_COIN,
            label = stringResource(R.string.go_back),
        ) {
            BackIcon(color = PuzzletColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        // The picture coin steps inboard and the sound switch takes the
        // right corner (D-078).
        if (game.completed) {
            Spacer(Modifier.size(TOP_BAR_COIN))
        } else {
            PeekCoin(scene = scene, peeking = peeking, onPeek = onPeek)
        }
        Spacer(Modifier.width(10.dp))
        SoundCoin(on = soundOn, onToggle = onSound)
    }
}

/** The sound switch: one quiet coin beside the picture coin (D-077). */
@Composable
private fun SoundCoin(on: Boolean, onToggle: (Boolean) -> Unit) {
    CircleButton(
        onClick = { onToggle(!on) },
        background = PuzzletColors.Card,
        size = TOP_BAR_COIN,
        label = stringResource(if (on) R.string.sound_on else R.string.sound_off),
    ) {
        SpeakerIcon(on = on, color = PuzzletColors.Ink)
    }
}

@Composable
private fun PeekCoin(scene: SceneSpec, peeking: Boolean, onPeek: (Boolean) -> Unit) {
    val label = stringResource(if (peeking) R.string.peek_hide else R.string.peek_show)
    CircleButton(
        onClick = { onPeek(!peeking) },
        background = if (peeking) PuzzletColors.sceneWash(scene.accent) else PuzzletColors.Card,
        size = TOP_BAR_COIN,
        label = label,
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxSize().padding(8.dp),
            // A circle inside the round coin, so the thumbnail reads as part
            // of the coin rather than a sticker sitting on it.
            cornerRadius = 16.dp,
        )
    }
}

/**
 * Forgiving back (D-057): the first back press while pieces are placed
 * asks, the second leaves. Empty board leaves at once, finished leaves
 * at once, peek closes first. Tapping outside the card stays.
 */
@Composable
private fun LeaveConfirm(onStay: () -> Unit, onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Scrim)
            .clickable(role = Role.Button, onClick = onStay),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(PuzzletColors.Card)
                // Swallow taps on the card without adding a semantics node:
                // the pointer input eats them, TalkBack never sees them.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.leave_title),
                style = MaterialTheme.typography.titleLarge,
                color = PuzzletColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.leave_message),
                style = MaterialTheme.typography.bodyLarge,
                color = PuzzletColors.Ink,
            )
            Spacer(Modifier.height(18.dp))
            LeaveButtons(onStay = onStay, onLeave = onLeave)
        }
    }
}

@Composable
private fun LeaveButtons(onStay: () -> Unit, onLeave: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .buttonShadow(shape)
                .clip(shape)
                .background(PuzzletColors.Teal)
                .clickable(role = Role.Button, onClick = onStay)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.stay),
                style = MaterialTheme.typography.titleMedium,
                color = PuzzletColors.Paper,
            )
        }
        Box(
            modifier = Modifier
                .buttonShadow(shape)
                .clip(shape)
                .background(PuzzletColors.Tray)
                .clickable(role = Role.Button, onClick = onLeave)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.leave),
                style = MaterialTheme.typography.titleMedium,
                color = PuzzletColors.Ink,
            )
        }
    }
}
