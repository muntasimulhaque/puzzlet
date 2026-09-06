package io.github.muntasimulhaque.puzzlet.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Piece
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.clampBoxTopLeft
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** A held piece rides a little larger than the board it is headed for. */
private const val HELD_SCALE = 1.06f

/**
 * Touch is the whole pickup (D-055): a piece lifts on the first contact,
 * with no drag distance to earn first and no second tap to try. While held
 * the finger owns it, drawn straight from [heldCenter] with the pure clamp
 * from core, so it stops at the table's edge exactly as a held piece should;
 * the game state hears about the whole carry once, at release. A second
 * finger changes nothing: the gesture belongs to the pointer that grabbed.
 */
@Composable
internal fun Modifier.fieldGestures(
    game: Puzzle,
    peeking: Boolean,
    hitRadiusPx: Double,
    heldCenter: MutableState<Vec2?>,
    actions: PlayActions,
): Modifier {
    val view = LocalView.current
    val trayScale by rememberUpdatedState(game.trayScale)
    val peekingNow by rememberUpdatedState(peeking)
    val latestGame by rememberUpdatedState(game)
    val latest by rememberUpdatedState(actions)
    return pointerInput(hitRadiusPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val grabbedId = if (peekingNow) {
                null
            } else {
                latest.onGrabAt(
                    Vec2(down.position.x.toDouble(), down.position.y.toDouble()),
                    hitRadiusPx * trayScale,
                )
            }
            val grabbed = grabbedId?.let { latestGame.piece(it) }
            if (grabbed == null) {
                // Bare table: nothing lifts, nothing moves.
                waitForUpOrCancellation()
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                val center = carryHeld(grabbed, down, heldCenter, { latestGame.field })
                if (latest.onDropAt(center - grabbed.size * 0.5)) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }
}

/** Carry the piece until the finger lifts or the gesture is lost; return the final centre. */
private suspend fun AwaitPointerEventScope.carryHeld(
    grabbed: Piece,
    down: PointerInputChange,
    heldCenter: MutableState<Vec2?>,
    field: () -> Area,
): Vec2 {
    val grip = Vec2(down.position.x.toDouble(), down.position.y.toDouble()) - grabbed.currentCenter
    heldCenter.value = grabbed.currentCenter
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
        // Lift or cancel ends the carry; an unknown pointer type is this
        // Compose version's cancel signal.
        if (change.changedToUpIgnoreConsumed() || change.type == PointerType.Unknown) {
            change.consume()
            break
        }
        if (change.isConsumed) break
        val finger = Vec2(change.position.x.toDouble(), change.position.y.toDouble())
        val raw = finger - grip - grabbed.size * 0.5
        val clamped = clampBoxTopLeft(raw, grabbed.size.x, grabbed.size.y, field())
        heldCenter.value = clamped + grabbed.size * 0.5
        change.consume()
    }
    return heldCenter.value ?: grabbed.currentCenter
}

/**
 * One node per piece for the whole game (D-055): a piece keeps its node
 * through grab, release and reorder, so every transition is a spring from
 * where the piece really was. Draw order is zIndex, not loop position:
 * placed pieces lie under waiting ones, the held piece rides above all.
 */
@Composable
internal fun PieceLayer(
    game: Puzzle,
    scene: SceneSpec,
    draggedId: Int?,
    restartAt: Long,
    heldCenter: State<Vec2?>,
) {
    for (piece in game.pieces) {
        key(piece.id) {
            val held = piece.id == draggedId && !piece.placed
            PieceNode(
                piece = piece,
                targetScale = when {
                    held -> HELD_SCALE
                    piece.placed -> 1f
                    else -> game.trayScale.toFloat()
                },
                scene = scene,
                board = game.board,
                restartAt = restartAt,
                index = piece.id,
                isHeld = held,
                heldCenter = heldCenter,
                modifier = Modifier.zIndex(
                    when {
                        held -> 2f
                        piece.placed -> 0f
                        else -> 1f
                    },
                ),
            )
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
    isHeld: Boolean,
    heldCenter: State<Vec2?>,
    modifier: Modifier = Modifier,
) {
    val path = remember(piece.shape) { outlinePath(piece.shape.segments) }
    // The display animates the piece's centre, so a glide keeps the piece
    // whole under itself at any scale: to its tray seat on a miss, to its
    // slot on a snap, staggered on a restart pour-back.
    val target = piece.currentCenter
    val display = remember { Animatable(target.toOffset(), Offset.VectorConverter) }
    val wasHeld = remember { mutableStateOf(false) }
    val lastRestart = remember { mutableStateOf(restartAt) }
    LaunchedEffect(target, restartAt, isHeld) {
        display.follow(target, isHeld, heldCenter, wasHeld, lastRestart, index, restartAt)
    }
    val scale by animateFloatAsState(targetScale, pieceScaleSpec(isHeld), label = "pieceScale")
    val density = LocalDensity.current
    val wDp = with(density) { (piece.size.x * scale.toDouble()).toFloat().toDp() }
    val hDp = with(density) { (piece.size.y * scale.toDouble()).toFloat().toDp() }
    Canvas(
        modifier
            .offset { tileTopLeft(piece, isHeld, heldCenter, display, scale) }
            .size(wDp, hDp),
    ) {
        withTransform({ scale(scale, scale) }) {
            drawSlice(piece, path, scene, board, isHeld)
        }
    }
}

/** The display's march: held means the finger owns it, free means a spring home. */
private suspend fun Animatable<Offset, AnimationVector2D>.follow(
    target: Vec2,
    isHeld: Boolean,
    heldCenter: State<Vec2?>,
    wasHeld: MutableState<Boolean>,
    lastRestart: MutableState<Long>,
    index: Int,
    restartAt: Long,
) {
    if (isHeld) {
        wasHeld.value = true
        snapTo((heldCenter.value ?: target).toOffset())
        return
    }
    if (wasHeld.value) {
        wasHeld.value = false
        // A release glides from where the finger left the piece (D-040).
        snapTo(heldCenter.value?.toOffset() ?: value)
    }
    val restarted = restartAt != lastRestart.value
    lastRestart.value = restartAt
    if (restarted) delay(index * 18L)
    animateTo(target.toOffset(), spring(stiffness = 400f, dampingRatio = 0.8f))
}

/** A held piece grows under the finger on one quick tween; free pieces spring. */
private fun pieceScaleSpec(isHeld: Boolean) =
    if (isHeld) {
        tween<Float>(130, easing = LinearOutSlowInEasing)
    } else {
        spring(stiffness = 400f, dampingRatio = 0.8f)
    }

/** The tile's top-left: the held centre under the finger, else the sprung centre. */
private fun tileTopLeft(
    piece: Piece,
    isHeld: Boolean,
    heldCenter: State<Vec2?>,
    display: Animatable<Offset, AnimationVector2D>,
    scale: Float,
): IntOffset {
    val c = if (isHeld) {
        heldCenter.value ?: piece.currentCenter
    } else {
        Vec2(display.value.x.toDouble(), display.value.y.toDouble())
    }
    val topLeft = c - piece.size * (scale.toDouble() / 2.0)
    return IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
}
