package io.github.muntasimulhaque.puzzlet.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Piece
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.clampBoxTopLeft

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
