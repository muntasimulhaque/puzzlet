package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Piece
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Vec2
import kotlinx.coroutines.delay

/** A held piece rides a little larger than the board it is headed for. */
private const val HELD_SCALE = 1.06f

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
    val lastRestart = remember { mutableLongStateOf(restartAt) }
    LaunchedEffect(target, restartAt, isHeld) {
        display.follow(target, isHeld, heldCenter, wasHeld, lastRestart, index, restartAt)
    }
    val scale by animateFloatAsState(targetScale, pieceScaleSpec(isHeld), label = "pieceScale")
    val density = LocalDensity.current
    val wDp = with(density) { piece.size.x.toFloat().toDp() }
    val hDp = with(density) { piece.size.y.toFloat().toDp() }
    Canvas(
        modifier
            // The carry is a draw-phase transform: translation and scale
            // move the recorded slice, so a drag never relayouts the tile
            // and never redraws its scene under the finger. The pivot is
            // the tile's own top-left, not its centre (D-056).
            .graphicsLayer { placeTile(piece, isHeld, heldCenter, display, scale) }
            .size(wDp, hDp),
    ) {
        drawSlice(piece, path, scene, board, isHeld)
    }
}

/** Where the tile is drawn: centred on the finger, else on the sprung centre. */
private fun GraphicsLayerScope.placeTile(
    piece: Piece,
    isHeld: Boolean,
    heldCenter: State<Vec2?>,
    display: Animatable<Offset, AnimationVector2D>,
    scale: Float,
) {
    val center = if (isHeld) {
        heldCenter.value ?: piece.currentCenter
    } else {
        Vec2(display.value.x.toDouble(), display.value.y.toDouble())
    }
    val topLeft = center - piece.size * (scale / 2.0)
    translationX = topLeft.x.toFloat()
    translationY = topLeft.y.toFloat()
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(0f, 0f)
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
