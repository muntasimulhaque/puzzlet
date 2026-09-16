package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.PIECE_COUNTS
import io.github.muntasimulhaque.puzzlet.core.SceneSpec

/**
 * The cut chooser (D-065): the child taps a picture, and the sizes the
 * picture comes in rise from the bottom as five tiles, each one the real
 * picture cut the real way it will be dealt (same cut seed as the game,
 * the D-042 truth kept). A three-year-old reads the tiles by look: four
 * big pieces or sixteen small ones; the numeral beneath speaks to the
 * parent. Tapping a tile plays at once and remembers it; the tile already
 * marked plays through the plain path, so where nobody has picked, wins
 * still walk the ladder (D-047). Tapping anywhere else puts it away.
 */
@Composable
fun CutChooser(
    scene: SceneSpec,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // It rises once, quickly: scrim and plate arrive together, the plate
    // a half step up as it comes. Leaving is at once: a tap is an answer.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(Motion.ARRIVE_MS, easing = Motion.arrive)) }
    val dismissLabel = stringResource(R.string.close)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = rise.value }
            .background(PuzzletColors.Scrim)
            .semantics { contentDescription = dismissLabel }
            .clickable(role = Role.Button, onClick = onDismiss),
    ) {
        ChooserPlate(scene = scene, current = current, rise = rise, onPick = onPick)
    }
}

/** The plate itself: the picture's name, the question, and the five real cuts. */
@Composable
private fun BoxScope.ChooserPlate(
    scene: SceneSpec,
    current: Int,
    rise: Animatable<Float, AnimationVector1D>,
    onPick: (Int) -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .graphicsLayer {
                translationY = (1f - rise.value) * 56.dp.toPx()
            }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(PuzzletColors.Card)
            // The plate names itself for TalkBack, so the five tiles are
            // heard as five ways to play this picture rather than as five
            // bare numbers on a nameless sheet (D-084).
            .semantics { paneTitle = name }
            // Swallow taps on the plate without adding a semantics node.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChooserHeader(name = name)
        Spacer(Modifier.height(14.dp))
        CutTileRow(scene = scene, current = current, onPick = onPick)
    }
}

/** The picture's name and the one question the plate asks. */
@Composable
private fun ChooserHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        color = PuzzletColors.Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.choose_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = PuzzletColors.Ink.copy(alpha = 0.72f),
    )
}

/** The five real cuts, smallest count first, the coming one marked. */
@Composable
private fun CutTileRow(scene: SceneSpec, current: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (pieces in PIECE_COUNTS) {
            CutTile(
                scene = scene,
                pieces = pieces,
                marked = pieces == current,
                onPick = onPick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
