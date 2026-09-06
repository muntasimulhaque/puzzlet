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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * The play field: a shelf above, a board below. The board stays blank, the
 * way a table does, and the picture lives behind one coin in the top bar
 * (D-048): look, then put it away. Each piece is its own tile (one small
 * Canvas per piece), so a stale cache can never blank the tray again: a
 * tile rebuilds its outline from its own shape whenever the cut grows.
 * Tiles glide home with a spring; logic already sits home, so a cancel can
 * strand nothing.
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
        GestureBoard(
            game, draggedId, pulseId, pulse.value, restartAt, peeking, hitPx, actions, onPeek, onBack,
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
    actions: PlayActions,
    onPeek: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    Box(Modifier.fillMaxSize().fieldGestures(game, hitRadiusPx, actions)) {
        BoardBackdrop(game, pulseId, pulseT)
        PieceLayer(game, scene, draggedId, restartAt)
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
