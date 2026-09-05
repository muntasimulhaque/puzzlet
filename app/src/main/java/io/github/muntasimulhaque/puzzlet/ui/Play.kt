package io.github.muntasimulhaque.puzzlet.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.Vec2

/**
 * What the play field needs from the world. No composable takes a ViewModel
 * (the house rule): the activity wires these to the host, and the screenshot
 * harness passes no-ops, which is what keeps captures flake-free. onLayout
 * hands the host the field and the board-size cap; the tray and the board
 * are decided in core (core/Layout.kt), so tests and captures agree.
 */
class PlayActions(
    val onGrabAt: (Vec2, Double) -> Vec2?,
    val onDragTo: (Vec2) -> Unit,
    val onDrop: () -> Boolean,
    val onLayout: (Area, Double) -> Unit,
    val onRestart: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * The play field: one canvas, two zones. The tray above holds the waiting
 * pieces at tray scale; the mat below frames the picture assembling. A piece
 * in hand grows to board size around the grip point; a piece is the picture
 * it belongs to, clipped by its own outline, so no bitmap crops exist and
 * every size stays crisp.
 */
@Composable
fun PlayScreen(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    actions: PlayActions,
    onBack: () -> Unit,
    returning: Set<Int> = emptySet(),
) {
    BackHandler(onBack = onBack)
    var peek by remember { mutableStateOf(false) }
    val ghostAlpha by animateFloatAsState(if (peek) 0.45f else 0.13f, label = "ghost")

    Column(modifier = Modifier.fillMaxSize().background(PuzzletColors.Paper)) {
        PlayTopBar(
            sceneId = game.sceneId,
            peek = peek,
            onPeek = { peek = !peek },
            onBack = onBack,
            onRestart = actions.onRestart,
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            val capPx = with(density) { 560.dp.toPx() }.toDouble()
            val hitRadiusPx = with(density) { 44.dp.toPx() }.toDouble()
            val field = Area(0.0, 0.0, constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble())
            LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
                actions.onLayout(field, capPx)
            }

            val view = LocalView.current
            var grabOffset by remember { mutableStateOf(Vec2(0.0, 0.0)) }
            val trayScale by rememberUpdatedState(game.trayScale)

            // One stable path per piece id, rebuilt only when the cut changes.
            val paths = remember(
                game.rows, game.cols, game.seed,
                constraints.maxWidth, constraints.maxHeight,
            ) {
                game.pieces.associate { it.id to outlinePath(it.shape.segments) }
            }

            val pulse = remember { Animatable(1f) }
            LaunchedEffect(pulseAt) {
                if (pulseId >= 0) {
                    pulse.snapTo(0f)
                    pulse.animateTo(1f, tween(380, easing = LinearOutSlowInEasing))
                }
            }
            val ring = rememberInfiniteTransition(label = "ring").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "ringAlpha",
            )

            // A grabbed piece pops from tray scale to board scale around the
            // grip point; the drag math keeps the same spot under the finger.
            val lift = remember { Animatable(1f) }
            LaunchedEffect(draggedId) {
                if (draggedId != null) {
                    lift.snapTo(game.trayScale.toFloat())
                    lift.animateTo(1.06f, tween(130, easing = LinearOutSlowInEasing))
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val grabbed = actions.onGrabAt(
                                    Vec2(pos.x.toDouble(), pos.y.toDouble()),
                                    hitRadiusPx * trayScale,
                                )
                                grabOffset = if (grabbed != null) {
                                    Vec2(pos.x.toDouble(), pos.y.toDouble()) - grabbed
                                } else {
                                    Vec2(0.0, 0.0)
                                }
                            },
                            onDrag = { change, _ ->
                                actions.onDragTo(
                                    Vec2(change.position.x.toDouble(), change.position.y.toDouble()) - grabOffset,
                                )
                            },
                            onDragEnd = {
                                if (actions.onDrop()) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            },
                            onDragCancel = { actions.onDrop() },
                        )
                    },
            ) {
                drawBoard(
                    game = game,
                    scene = Scenes.byId(game.sceneId),
                    paths = paths,
                    ghostAlpha = ghostAlpha,
                    dragId = draggedId,
                    ringAlpha = 0.30f + 0.35f * ring.value,
                    pulseId = pulseId,
                    pulseT = pulse.value,
                    liftV = lift.value,
                    returning = returning,
                )
            }

            if (game.completed) {
                Celebration(game, onAgain = actions.onRestart, onHome = onBack)
            }
        }
    }
}

@Composable
private fun PlayTopBar(
    sceneId: String,
    peek: Boolean,
    onPeek: () -> Unit,
    onBack: () -> Unit,
    onRestart: () -> Unit,
) {
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
        // Peek: the finished picture on a coin. Tap to strengthen the ghost
        // on the board; the picture itself is the affordance, no eye icon.
        CircleButton(
            onClick = onPeek,
            background = if (peek) PuzzletColors.Honey else PuzzletColors.Card,
            label = stringResource(R.string.peek),
        ) {
            ScenePicture(
                spec = Scenes.byId(sceneId),
                modifier = Modifier.padding(5.dp),
                cornerRadius = 40.dp,
            )
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        CircleButton(
            onClick = onRestart,
            background = PuzzletColors.Card,
            label = stringResource(R.string.restart),
        ) {
            ReplayIcon(color = PuzzletColors.Ink)
        }
    }
}
