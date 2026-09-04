package app.puzzlet.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.puzzlet.core.Area
import app.puzzlet.core.Cubic
import app.puzzlet.core.Piece
import app.puzzlet.core.Puzzle
import app.puzzlet.core.SceneSpec
import app.puzzlet.core.Scenes
import app.puzzlet.core.Vec2

/** The board sizing policy, one function so tests and captures agree. */
fun boardSideFor(fieldW: Double, fieldH: Double, chunky: Boolean, capPx: Double): Double =
    minOf(fieldW * 0.92, fieldH * (if (chunky) 0.60 else 0.78), capPx)

/**
 * What the play field needs from the world. No composable takes a ViewModel
 * (the house rule): the activity wires these to the host, and the screenshot
 * harness passes no-ops, which is what keeps captures flake-free.
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
 * The play field: one canvas, one gesture authority. A piece is the picture
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
            val chunky = game.rows * game.cols <= 9
            val boardSide = boardSideFor(
                constraints.maxWidth.toDouble(),
                constraints.maxHeight.toDouble(),
                chunky,
                capPx,
            )
            val field = Area(0.0, 0.0, constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble())
            LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
                actions.onLayout(field, boardSide)
            }

            val view = LocalView.current
            var grabOffset by remember { mutableStateOf(Vec2(0.0, 0.0)) }

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

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val grabbed = actions.onGrabAt(
                                    Vec2(pos.x.toDouble(), pos.y.toDouble()),
                                    hitRadiusPx,
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
        CircleButton(onClick = onBack, background = PuzzletColors.Card) {
            BackIcon(color = PuzzletColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        // Peek: the finished picture on a coin. Tap to strengthen the ghost
        // on the board; the picture itself is the affordance, no eye icon.
        CircleButton(
            onClick = onPeek,
            background = if (peek) PuzzletColors.Honey else PuzzletColors.Card,
        ) {
            ScenePicture(
                spec = Scenes.byId(sceneId),
                modifier = Modifier.padding(5.dp),
                cornerRadius = 40.dp,
            )
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        CircleButton(onClick = onRestart, background = PuzzletColors.Card) {
            ReplayIcon(color = PuzzletColors.Ink)
        }
    }
}

private fun outlinePath(segments: List<Cubic>): Path {
    val path = Path()
    var first = true
    for (seg in segments) {
        if (first) {
            path.moveTo(seg.p0.x.toFloat(), seg.p0.y.toFloat())
            first = false
        }
        path.cubicTo(
            seg.c1.x.toFloat(), seg.c1.y.toFloat(),
            seg.c2.x.toFloat(), seg.c2.y.toFloat(),
            seg.p1.x.toFloat(), seg.p1.y.toFloat(),
        )
    }
    path.close()
    return path
}

private fun DrawScope.drawBoard(
    game: Puzzle,
    scene: SceneSpec,
    paths: Map<Int, Path>,
    ghostAlpha: Float,
    dragId: Int?,
    ringAlpha: Float,
    pulseId: Int,
    pulseT: Float,
) {
    val board = game.board
    val boardR = Rect(
        board.x.toFloat(), board.y.toFloat(),
        (board.x + board.w).toFloat(), (board.y + board.h).toFloat(),
    )

    // The faint whole picture: where every piece is going, always visible.
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.alpha = ghostAlpha
        canvas.saveLayer(boardR, paint)
    }
    withTransform({ translate(board.x.toFloat(), board.y.toFloat()) }) {
        drawScene(scene, board.w)
    }
    drawIntoCanvas { it.restore() }

    val dragged = dragId?.let { game.piece(it) }
    for (piece in game.pieces) {
        if (piece.id == dragged?.id) continue
        val path = paths[piece.id] ?: continue
        drawPiece(
            piece = piece,
            path = path,
            at = piece.current,
            scene = scene,
            board = board,
            scale = 1f,
            shadowAlpha = if (piece.placed) 0f else 0.10f,
            outlineAlpha = 0.16f,
        )
    }

    // While a piece is held, its slot glows softly: the piece shows you
    // where it wants to go, without moving anything for you.
    if (dragged != null) {
        drawRoundRect(
            PuzzletColors.Honey.copy(alpha = ringAlpha),
            topLeft = Offset(dragged.home.x.toFloat(), dragged.home.y.toFloat()),
            size = Size(dragged.size.x.toFloat(), dragged.size.y.toFloat()),
            cornerRadius = CornerRadius(10f),
            style = Stroke(width = 4.dp.toPx()),
        )
    }

    // The click: a short ring burst where a piece just landed.
    if (pulseId >= 0) {
        val landed = game.piece(pulseId)
        if (landed != null && pulseT < 1f) {
            val c = landed.homeCenter
            val r = (landed.halfDiagonal * (0.5f + pulseT)).toFloat()
            drawCircle(
                PuzzletColors.Honey.copy(alpha = (1f - pulseT) * 0.7f),
                radius = r,
                center = Offset(c.x.toFloat(), c.y.toFloat()),
                style = Stroke(width = 4f * (1f - pulseT) + 1f),
            )
        }
    }

    if (dragged != null) {
        val path = paths[dragged.id]
        if (path != null) {
            drawPiece(
                piece = dragged,
                path = path,
                at = dragged.current,
                scene = scene,
                board = board,
                scale = 1.06f,
                shadowAlpha = 0.22f,
                outlineAlpha = 0.20f,
            )
        }
    }
}

private fun DrawScope.drawPiece(
    piece: Piece,
    path: Path,
    at: Vec2,
    scene: SceneSpec,
    board: Area,
    scale: Float,
    shadowAlpha: Float,
    outlineAlpha: Float,
) {
    val ax = at.x.toFloat()
    val ay = at.y.toFloat()
    withTransform({
        translate(ax, ay)
        if (scale != 1f) {
            scale(scale, scale, pivot = Offset(piece.size.x.toFloat() / 2f, piece.size.y.toFloat() / 2f))
        }
    }) {
        if (shadowAlpha > 0f) {
            withTransform({ translate(0f, 3f) }) {
                drawPath(path, PuzzletColors.Ink.copy(alpha = shadowAlpha))
            }
        }
        clipPath(path) {
            // The scene sits at the board's origin in field coordinates; the
            // piece transform is at the piece's bbox corner, so draw the
            // scene offset by (board - at) and the clip does the rest.
            withTransform({ translate((board.x).toFloat() - ax, (board.y).toFloat() - ay) }) {
                drawScene(scene, board.w)
            }
        }
        drawPath(path, PuzzletColors.Ink.copy(alpha = outlineAlpha), style = Stroke(1.6.dp.toPx()))
    }
}
