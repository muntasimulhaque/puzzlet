package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.starPoints
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The finish: the picture held up proudly on one clean plate, confetti
 * in the house palette behind it, and the two ways onward sitting below
 * the picture where a small hand finds them at once. Again leads by size;
 * both coins wear the shelf recipe, round with ink icons and one soft
 * shadow. No score, no pressure.
 */
@Composable
fun Celebration(game: Puzzle, onAgain: () -> Unit, onHome: () -> Unit) {
    val confetti = remember(game.seed) { buildConfetti(game.seed) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(game.seed) {
        fall.animateTo(1f, tween(3200, easing = LinearEasing))
    }
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(game.seed) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PuzzletColors.Scrim)
                // The plate owns the screen: taps that miss the coins land on
                // the scrim and stop there, never on the top bar underneath.
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Canvas(Modifier.fillMaxSize()) {
            if (fall.value < 1f) drawConfetti(confetti, fall.value)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CelebrationPlate(game = game, pop = pop.asState(), onAgain = onAgain, onHome = onHome)
        }
    }
}

/** One clean plate: picture, praise, then the two ways onward below it. */
@Composable
private fun CelebrationPlate(game: Puzzle, pop: State<Float>, onAgain: () -> Unit, onHome: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(PuzzletColors.Card)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        // The plate yields to the shape of the field it lands in. In
        // landscape the picture shares the row with the praise and the
        // coins under it, so the whole plate always fits the height a
        // phone gives it; nothing can end up off the plate (D-068). A
        // scroll is the last safety net for a field too short for even
        // the smallest plate: nothing is ever unreachable.
        val landscape = maxWidth > maxHeight
        val pictureSide = if (landscape) {
            minOf(232.dp, maxHeight * 0.74f)
        } else {
            minOf(maxWidth, 280.dp, (maxHeight - 236.dp).coerceAtLeast(104.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (landscape) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CelebratedPicture(game = game, side = pictureSide, pop = pop)
                    Spacer(Modifier.width(22.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Praise()
                        Spacer(Modifier.height(18.dp))
                        FinishButtons(onAgain = onAgain, onHome = onHome)
                    }
                }
            } else {
                CelebratedPicture(game = game, side = pictureSide, pop = pop)
                Spacer(Modifier.height(16.dp))
                Praise()
                Spacer(Modifier.height(18.dp))
                FinishButtons(onAgain = onAgain, onHome = onHome)
            }
        }
    }
}

/** The finished picture, popped to its home size and centred. */
@Composable
private fun CelebratedPicture(game: Puzzle, side: Dp, pop: State<Float>) {
    ScenePicture(
        spec = Scenes.byId(game.sceneId),
        modifier = Modifier
            .width(side)
            // The pop is read inside the layer, so the spring moves the
            // picture without recomposing the plate's praise and coins.
            .graphicsLayer {
                val value = pop.value
                scaleX = value
                scaleY = value
                alpha = ((value - 0.5f) / 0.5f).coerceIn(0f, 1f)
            },
        cornerRadius = 22.dp,
    )
}

/** The one praise, in the display face. */
@Composable
private fun Praise() {
    Text(
        text = stringResource(R.string.well_done),
        style = MaterialTheme.typography.displayMedium,
        color = PuzzletColors.Ink,
        textAlign = TextAlign.Center,
    )
}

/** One size for both finish coins; Again leads by ground, not size
 *  (D-079). The Teal and Tray pair is the same primary and secondary
 *  recipe the leave confirm already uses. */
private val FINISH_COIN = 64.dp

/** Equal coins; Again leads by the brand teal, the way Stay leads. */
@Composable
private fun FinishButtons(onAgain: () -> Unit, onHome: () -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        FinishCoin(
            onClick = onAgain,
            background = PuzzletColors.Teal,
            label = stringResource(R.string.restart),
            text = stringResource(R.string.again),
        ) {
            ReplayIcon(color = PuzzletColors.Paper, size = 28.dp)
        }
        Spacer(Modifier.width(40.dp))
        FinishCoin(
            onClick = onHome,
            background = PuzzletColors.Tray,
            label = stringResource(R.string.home),
            text = stringResource(R.string.home),
        ) {
            MenuIcon(color = PuzzletColors.Ink, size = 28.dp)
        }
    }
}

/** One finish coin: one size, one shadow, the ground carries the lead. */
@Composable
private fun FinishCoin(
    onClick: () -> Unit,
    background: Color,
    label: String,
    text: String,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleButton(
            onClick = onClick,
            background = background,
            size = FINISH_COIN,
            label = label,
        ) {
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PuzzletColors.Ink,
        )
    }
}

private fun DrawScope.drawConfetti(pieces: List<ConfettiPiece>, t: Float) {
    for (p in pieces) {
        val local = ((t - p.delay) / p.fall).coerceIn(0.0, 1.0)
        if (local <= 0.0) continue
        val y = ((-0.08 + local * 1.25) * size.height).toFloat()
        val x = ((p.x0 + p.sway * sin(local * p.freq * 2 * PI + p.phase)) * size.width).toFloat()
        val alpha = if (local > 0.82) ((1.0 - local) / 0.18).toFloat() else 1f
        val angle = (p.rot0 + t * p.spin).toFloat()
        withTransform({
            translate(x, y)
            rotate(angle)
        }) {
            when (p.kind) {
                0 -> drawCircle(p.color.copy(alpha = alpha), radius = p.size.toFloat())
                1 -> drawRoundRect(
                    p.color.copy(alpha = alpha),
                    topLeft = androidx.compose.ui.geometry.Offset(-p.size.toFloat(), -p.size.toFloat()),
                    size = androidx.compose.ui.geometry.Size(p.size.toFloat() * 2, p.size.toFloat() * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                )
                else -> p.shape?.let { drawPath(it, p.color.copy(alpha = alpha)) }
            }
        }
    }
}

private fun triangle(s: Float): Path = Path().apply {
    moveTo(0f, -s)
    lineTo(s * 0.9f, s * 0.7f)
    lineTo(-s * 0.9f, s * 0.7f)
    close()
}

private fun star(s: Float): Path = Path().apply {
    val pts = starPoints(Vec2(0.0, 0.0), s.toDouble(), (s * 0.45).toDouble(), 5)
    moveTo(pts[0].x.toFloat(), pts[0].y.toFloat())
    for (i in 1 until pts.size) lineTo(pts[i].x.toFloat(), pts[i].y.toFloat())
    close()
}

private class ConfettiPiece(
    val x0: Double,
    val delay: Double,
    val fall: Double,
    val sway: Double,
    val freq: Double,
    val phase: Double,
    val rot0: Double,
    val spin: Double,
    val size: Double,
    val color: Color,
    val kind: Int,
) {
    /** Kinds 2 and 3 are a triangle or a star, built once, not per frame. */
    val shape: Path? = when (kind) {
        2 -> triangle(size.toFloat())
        3 -> star(size.toFloat())
        else -> null
    }
}

private fun buildConfetti(seed: Long): List<ConfettiPiece> {
    val rnd = Random(seed + 31)
    val colors = listOf(PuzzletColors.Teal, PuzzletColors.Honey, PuzzletColors.Coral, PuzzletColors.Sky)
    return List(64) {
        ConfettiPiece(
            x0 = 0.05 + rnd.nextDouble() * 0.9,
            delay = rnd.nextDouble() * 0.25,
            fall = 0.85 + rnd.nextDouble() * 0.5,
            sway = 0.02 + rnd.nextDouble() * 0.05,
            freq = 1.0 + rnd.nextDouble() * 2.0,
            phase = rnd.nextDouble() * 2 * PI,
            rot0 = rnd.nextDouble() * 360,
            spin = (rnd.nextDouble() - 0.5) * 720,
            size = 5.0 + rnd.nextDouble() * 7.0,
            color = colors[rnd.nextInt(colors.size)],
            kind = rnd.nextInt(4),
        )
    }
}
