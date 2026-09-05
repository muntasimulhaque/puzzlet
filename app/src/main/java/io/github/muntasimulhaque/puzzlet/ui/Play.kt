package io.github.muntasimulhaque.puzzlet.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Piece
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.Vec2
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
)

/**
 * The play field: a shelf above, a board below. Each piece is its own tile
 * (one small Canvas per piece), so a stale cache can never blank the tray
 * again: a tile rebuilds its outline from its own shape whenever the cut
 * grows. Tiles glide home with a spring; logic already sits home, so a
 * cancel can strand nothing.
 */
@Composable
fun PlayScreen(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    restartAt: Long,
    actions: PlayActions,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var peek by remember { mutableStateOf(false) }
    val ghostAlpha by animateFloatAsState(if (peek) 1f else 0f, label = "ghost")
    Column(modifier = Modifier.fillMaxSize().background(PuzzletColors.Paper)) {
        PlayTopBar(
            sceneId = game.sceneId,
            peek = peek,
            onPeek = { peek = !peek },
            onBack = onBack,
        )
        PlayField(game, draggedId, pulseId, pulseAt, restartAt, ghostAlpha, actions, onBack, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun PlayField(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseAt: Long,
    restartAt: Long,
    ghostAlpha: Float,
    actions: PlayActions,
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
        GestureBoard(game, draggedId, pulseId, pulse.value, restartAt, ghostAlpha, hitPx, actions, onBack)
    }
}

@Composable
private fun GestureBoard(
    game: Puzzle,
    draggedId: Int?,
    pulseId: Int,
    pulseT: Float,
    restartAt: Long,
    ghostAlpha: Float,
    hitRadiusPx: Double,
    actions: PlayActions,
    onBack: () -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    Box(Modifier.fillMaxSize().fieldGestures(game, hitRadiusPx, actions)) {
        BoardBackdrop(game, scene, ghostAlpha, pulseId, pulseT)
        PieceLayer(game, scene, draggedId, restartAt)
        if (game.completed) {
            Celebration(game, onAgain = actions.onRestart, onHome = onBack)
        }
    }
}

@Composable
private fun Modifier.fieldGestures(game: Puzzle, hitRadiusPx: Double, actions: PlayActions): Modifier {
    val view = LocalView.current
    val trayScale by rememberUpdatedState(game.trayScale)
    val latest by rememberUpdatedState(actions)
    return pointerInput(hitRadiusPx) {
        var grip = Vec2(0.0, 0.0)
        detectDragGestures(
            onDragStart = { pos ->
                val grabbed = latest.onGrabAt(Vec2(pos.x.toDouble(), pos.y.toDouble()), hitRadiusPx * trayScale)
                grip = if (grabbed != null) Vec2(pos.x.toDouble(), pos.y.toDouble()) - grabbed else Vec2(0.0, 0.0)
            },
            onDrag = { change, _ ->
                latest.onDragTo(Vec2(change.position.x.toDouble(), change.position.y.toDouble()) - grip)
            },
            onDragEnd = {
                if (latest.onDrop()) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            },
            onDragCancel = { latest.onDrop() },
        )
    }
}

@Composable
private fun PieceLayer(game: Puzzle, scene: SceneSpec, draggedId: Int?, restartAt: Long) {
    val board = game.board
    for (piece in game.pieces) {
        if (piece.placed) {
            key(piece.id) {
                PieceNode(piece, 1f, scene, board, restartAt, piece.id, false)
            }
        }
    }
    for (piece in game.pieces) {
        if (!piece.placed && piece.id != draggedId) {
            key(piece.id) {
                PieceNode(piece, game.trayScale.toFloat(), scene, board, restartAt, piece.id, false)
            }
        }
    }
    val dragged = draggedId?.let { game.piece(it) }
    if (dragged != null) {
        key(dragged.id) {
            PieceNode(dragged, 1.06f, scene, board, restartAt, dragged.id, true)
        }
    }
}

@Composable
private fun PieceNode(
    piece: Piece,
    targetScale: Float,
    scene: SceneSpec,
    board: Area,
    restartAt: Long,
    index: Int,
    isDragged: Boolean,
) {
    val path = remember(piece.shape) { outlinePath(piece.shape.segments) }
    val target = pieceTargetTopLeft(piece, targetScale).let { Offset(it.x.toFloat(), it.y.toFloat()) }
    val display = remember { Animatable(target, Offset.VectorConverter) }
    var lastRestart by remember { mutableStateOf(restartAt) }
    LaunchedEffect(target, restartAt, isDragged) {
        if (isDragged) {
            lastRestart = restartAt
            display.snapTo(target)
        } else {
            val restarted = restartAt != lastRestart
            lastRestart = restartAt
            if (restarted) delay(index * 18L)
            display.animateTo(target, spring(stiffness = 400f, dampingRatio = 0.8f))
        }
    }
    val scale by animateFloatAsState(targetScale, if (isDragged) tween(130, easing = LinearOutSlowInEasing) else spring(stiffness = 400f, dampingRatio = 0.8f), label = "pieceScale")
    val density = LocalDensity.current
    val wDp = with(density) { (piece.size.x * scale.toDouble()).toFloat().toDp() }
    val hDp = with(density) { (piece.size.y * scale.toDouble()).toFloat().toDp() }
    Canvas(
        Modifier
            .offset { IntOffset(display.value.x.roundToInt(), display.value.y.roundToInt()) }
            .size(wDp, hDp),
    ) {
        withTransform({ scale(scale, scale) }) {
            drawSlice(piece, path, scene, board, if (isDragged) 0.24f else 0.16f, 0.28f)
        }
    }
}

@Composable
private fun PlayTopBar(
    sceneId: String,
    peek: Boolean,
    onPeek: () -> Unit,
    onBack: () -> Unit,
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
        // Peek: the finished picture on a coin, big enough for small thumbs.
        // Tap to reveal the goal on the board; the picture itself is the
        // affordance, no eye icon.
        CircleButton(
            onClick = onPeek,
            background = if (peek) PuzzletColors.Honey else PuzzletColors.Card,
            size = 56.dp,
            label = stringResource(R.string.peek),
        ) {
            ScenePicture(
                spec = Scenes.byId(sceneId),
                modifier = Modifier.padding(5.dp),
                cornerRadius = 40.dp,
            )
        }
    }
}
