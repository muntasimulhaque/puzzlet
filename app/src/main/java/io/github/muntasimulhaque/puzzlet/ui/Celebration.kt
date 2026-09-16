package io.github.muntasimulhaque.puzzlet.ui

import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Scenes
import kotlinx.coroutines.launch

/** The confetti's one long fall. */
private const val CONFETTI_MS = 3200

/** How far the plate rises into place, and how small it starts. */
private val PLATE_RISE = 30.dp
private const val PLATE_FROM = 0.965f

/** The picture lands a hair after the plate, and a hair smaller. */
private const val PICTURE_FROM = 0.92f

/**
 * The finish: the picture held up proudly on one clean plate, confetti
 * in the house palette behind it, and the two ways onward sitting below
 * the picture where a small hand finds them at once. Again leads by size;
 * both coins wear the shelf recipe, round with ink icons and one soft
 * shadow. No score, no pressure.
 *
 * The plate arrives as one gesture (D-084): the scrim fades beneath it,
 * the plate rises the last few dp and settles, and the finished picture
 * lands inside it a beat later. Before this the plate simply appeared,
 * which made the app's proudest moment its only unanimated one.
 */
@Composable
fun Celebration(game: Puzzle, onAgain: () -> Unit, onHome: () -> Unit) {
    val clocks = rememberCelebrationClocks(game.seed)
    val confetti = remember(game.seed) { buildConfetti(game.seed) }
    // A plate that rises in silence is a plate a TalkBack player misses.
    val view = LocalView.current
    val praise = stringResource(R.string.well_done)
    LaunchedEffect(game.seed) { view.announceIfHeard(praise) }

    Box(modifier = Modifier.fillMaxSize()) {
        CelebrationScrim(rise = clocks.rise.asState())
        Canvas(Modifier.fillMaxSize()) {
            if (clocks.fall.value < 1f) drawConfetti(confetti, clocks.fall.value)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CelebrationPlate(
                game = game,
                rise = clocks.rise.asState(),
                pop = clocks.pop.asState(),
                onAgain = onAgain,
                onHome = onHome,
            )
        }
    }
}

/**
 * The dimmed ground the plate lands on. It is ready before the plate is:
 * the fade rides the front half of the plate's own spring, so the plate
 * never falls against a half-dimmed field. The alpha is read at draw time,
 * so the fade never recomposes the plate's praise and coins. Taps that miss
 * the coins stop here, never on the top bar underneath.
 */
@Composable
private fun CelebrationScrim(rise: State<Float>) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (rise.value * 1.8f).coerceIn(0f, 1f) }
            .background(PuzzletColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { } },
    )
}

/** The three clocks the celebration runs on: the fall, the plate, the picture. */
private class CelebrationClocks(
    val fall: Animatable<Float, AnimationVector1D>,
    val rise: Animatable<Float, AnimationVector1D>,
    val pop: Animatable<Float, AnimationVector1D>,
)

/**
 * Start the three moves together (D-084): the confetti falls for its long
 * breath, the plate rises the last few dp and settles, and the finished
 * picture lands inside it a hair behind. One effect, three clocks, so the
 * whole arrival is one gesture instead of three animations that happen to
 * overlap. Before this the plate simply appeared, which made the app's
 * proudest moment its only unanimated one.
 */
@Composable
private fun rememberCelebrationClocks(seed: Long): CelebrationClocks {
    val clocks = remember(seed) {
        CelebrationClocks(Animatable(0f), Animatable(0f), Animatable(PICTURE_FROM))
    }
    LaunchedEffect(seed) {
        launch { clocks.fall.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
        launch { clocks.rise.animateTo(1f, Motion.settle(stiffness = 300f, damping = 0.9f)) }
        launch { clocks.pop.animateTo(1f, Motion.settle(stiffness = 320f, damping = 0.72f)) }
    }
    return clocks
}

/** One clean plate: picture, praise, then the two ways onward below it. */
@Composable
private fun CelebrationPlate(
    game: Puzzle,
    rise: State<Float>,
    pop: State<Float>,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.plateLift(rise)) {
        // The plate yields to the shape of the field it lands in. In
        // landscape the picture shares the row with the praise and the
        // coins under it, so the whole plate always fits the height a
        // phone gives it; nothing can end up off the plate (D-068). A
        // scroll is the last safety net for a field too short for even
        // the smallest plate: nothing is ever unreachable.
        val landscape = maxWidth > maxHeight
        val side = pictureSide(landscape, maxWidth, maxHeight)
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (landscape) {
                LandscapePlate(game, side, pop, onAgain, onHome)
            } else {
                PortraitPlate(game, side, pop, onAgain, onHome)
            }
        }
    }
}

/**
 * The plate's own lift: it rises the last few dp, grows the last hair and
 * fades, on the one spring the celebration shares (D-084). The values are
 * read inside the layer, so the arrival never recomposes the plate.
 */
private fun Modifier.plateLift(rise: State<Float>): Modifier = this
    .graphicsLayer {
        val value = rise.value
        translationY = (1f - value) * PLATE_RISE.toPx()
        val scale = PLATE_FROM + (1f - PLATE_FROM) * value
        scaleX = scale
        scaleY = scale
        alpha = value.coerceIn(0f, 1f)
    }
    .padding(horizontal = 24.dp)
    .clip(RoundedCornerShape(30.dp))
    .background(PuzzletColors.Card)
    .padding(horizontal = 20.dp, vertical = 20.dp)

/** How much room the picture may take: the plate is measured, not guessed. */
private fun pictureSide(landscape: Boolean, maxWidth: Dp, maxHeight: Dp): Dp =
    if (landscape) {
        minOf(232.dp, maxHeight * 0.74f)
    } else {
        minOf(maxWidth, 280.dp, (maxHeight - 236.dp).coerceAtLeast(104.dp))
    }

/** The portrait plate: the picture, then the praise and the coins under it. */
@Composable
private fun PortraitPlate(
    game: Puzzle,
    side: Dp,
    pop: State<Float>,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    CelebratedPicture(game = game, side = side, pop = pop)
    Spacer(Modifier.height(16.dp))
    Praise()
    Spacer(Modifier.height(18.dp))
    FinishButtons(onAgain = onAgain, onHome = onHome)
}

/** The landscape plate: the picture shares the row with the praise. */
@Composable
private fun LandscapePlate(
    game: Puzzle,
    side: Dp,
    pop: State<Float>,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CelebratedPicture(game = game, side = side, pop = pop)
        Spacer(Modifier.width(22.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Praise()
            Spacer(Modifier.height(18.dp))
            FinishButtons(onAgain = onAgain, onHome = onHome)
        }
    }
}

/** The finished picture, settled at its home size and centred. */
@Composable
private fun CelebratedPicture(game: Puzzle, side: Dp, pop: State<Float>) {
    ScenePicture(
        spec = Scenes.byId(game.sceneId),
        modifier = Modifier
            .width(side)
            // The settle is read inside the layer, so it moves the picture
            // without recomposing the plate's praise and coins.
            .graphicsLayer {
                val value = pop.value
                scaleX = value
                scaleY = value
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

/**
 * Speak [text] when a screen reader is really listening. The plate is new
 * content over a screen whose focus has not moved, so without this the
 * app's proudest moment would arrive silently; a plain sighted play has
 * nothing to hear (D-084).
 */
private fun View.announceIfHeard(text: String) {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return
    if (!manager.isEnabled || !manager.isTouchExplorationEnabled) return
    // The platform deprecated this call in favour of live regions, which
    // announce a node's content *changing*. A plate arriving is not a
    // change, so the announcement stays the only honest way to speak it.
    @Suppress("DEPRECATION")
    announceForAccessibility(text)
}
