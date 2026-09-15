package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.Vec2

@Composable
internal fun PlayField(
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
