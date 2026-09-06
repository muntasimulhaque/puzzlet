package io.github.muntasimulhaque.puzzlet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
 * (D-048): look, then put it away. Each piece is its own tile (one small
 * Canvas per piece), and one tile lives for the whole game (D-055), so a
 * grab, a release and a reorder never rebuild a piece mid flight.
 */
@Composable
fun PlayScreen(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    restartAt: Long,
    peeking: Boolean,
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = { if (peeking) onPeek(false) else onBack() })
    Column(modifier = Modifier.fillMaxSize().background(PuzzletColors.Paper)) {
        PlayTopBar(game, peeking, onPeek, onBack)
        PlayField(
            game, draggedId, pulseId, pulseAt, restartAt, peeking, actions, onPeek, onBack,
            Modifier.fillMaxWidth().weight(1f),
        )
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
    onBack: () -> Unit,
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
            game, draggedId, pulseId, pulse.value, restartAt, peeking, hitPx, heldCenter, actions, onPeek, onBack,
        )
    }
}

@Composable
private fun GestureBoard(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseT: Float,
    restartAt: Long,
    peeking: Boolean,
    hitRadiusPx: Double,
    heldCenter: MutableState<Vec2?>,
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    Box(
        Modifier
            .fillMaxSize()
            .fieldGestures(game, peeking, hitRadiusPx, heldCenter, actions),
    ) {
        BoardBackdrop(game, pulseId, pulseT)
        PieceLayer(game, scene, draggedId, restartAt, heldCenter)
        if (peeking && !game.completed) {
            PeekPanel(scene, onDismiss = { onPeek(false) })
        }
        if (game.completed) {
            Celebration(game, onAgain = actions.onRestart, onHome = onBack)
        }
    }
}

/**
 * The finished picture, held up over the field on a deep scrim. Tapping
 * anywhere puts it away: one rule, the biggest target on the screen.
 */
@Composable
private fun PeekPanel(scene: SceneSpec, onDismiss: () -> Unit) {
    val label = stringResource(R.string.peek_hide)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Ink.copy(alpha = 0.72f))
            .semantics { contentDescription = label }
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val side = minOf(maxWidth * 0.78f, maxHeight * 0.78f)
            Box(Modifier.background(PuzzletColors.Card, RoundedCornerShape(30.dp)).padding(9.dp)) {
                ScenePicture(
                    spec = scene,
                    modifier = Modifier.width(side),
                    cornerRadius = 22.dp,
                )
            }
        }
    }
}

/** Back on the left, the picture itself on the right: tap it to look. */
@Composable
private fun PlayTopBar(
    game: Puzzle,
    peeking: Boolean,
    onPeek: (Boolean) -> Unit,
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
            label = stringResource(R.string.go_back),
        ) {
            BackIcon(color = PuzzletColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        // The finish holds the picture up by itself, so the coin steps aside.
        if (game.completed) {
            Spacer(Modifier.size(52.dp))
        } else {
            PeekCoin(scene = scene, peeking = peeking, onPeek = onPeek)
        }
    }
}

@Composable
private fun PeekCoin(scene: SceneSpec, peeking: Boolean, onPeek: (Boolean) -> Unit) {
    val label = stringResource(if (peeking) R.string.peek_hide else R.string.peek_show)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (peeking) PuzzletColors.Teal else PuzzletColors.Card)
            .semantics { contentDescription = label }
            .clickable { onPeek(!peeking) },
        contentAlignment = Alignment.Center,
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxSize().padding(if (peeking) 8.dp else 5.dp),
            cornerRadius = 10.dp,
        )
    }
}
