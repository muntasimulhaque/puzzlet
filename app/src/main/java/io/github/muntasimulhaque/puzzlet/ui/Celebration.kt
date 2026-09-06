package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
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
 * The finish: the picture held up proudly, confetti in the house palette,
 * two big ways onward. No sounds yet (M2), no score, no pressure.
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
                .background(PuzzletColors.Ink.copy(alpha = 0.62f)),
        )
        Canvas(Modifier.fillMaxSize()) {
            if (fall.value < 1f) drawConfetti(confetti, fall.value)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BoxWithConstraints {
                val side = minOf(maxWidth * 0.72f, 300.dp)
                ScenePicture(
                    spec = Scenes.byId(game.sceneId),
                    modifier = Modifier
                        .width(side)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            alpha = ((pop.value - 0.5f) / 0.5f).coerceIn(0f, 1f)
                        },
                    cornerRadius = 28.dp,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.well_done),
                style = MaterialTheme.typography.displayMedium,
                color = PuzzletColors.Paper,
            )
            Spacer(Modifier.height(26.dp))
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircleButton(
                        onClick = onAgain,
                        background = PuzzletColors.Teal,
                        size = 72.dp,
                        label = stringResource(R.string.restart),
                    ) {
                        ReplayIcon(color = PuzzletColors.Paper)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.again),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PuzzletColors.Paper,
                    )
                }
                Spacer(Modifier.width(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircleButton(
                        onClick = onHome,
                        background = PuzzletColors.Card,
                        size = 56.dp,
                        label = stringResource(R.string.home),
                    ) {
                        MenuIcon(color = PuzzletColors.Ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PuzzletColors.Paper,
                    )
                }
            }
        }
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
                2 -> drawPath(triangle(p.size.toFloat()), p.color.copy(alpha = alpha))
                else -> drawPath(star(p.size.toFloat()), p.color.copy(alpha = alpha))
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
)

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
