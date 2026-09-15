package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Scenes

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
