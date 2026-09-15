package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.SceneSpec

/**
 * The finished picture, held up over the field on a deep scrim. Tapping
 * anywhere on the field puts it away: one rule, the biggest target there
 * is. The top bar stays above the scrim, so the picture coin keeps showing
 * its own on state (the picture's wash, D-080) while the child looks.
 */
@Composable
internal fun PeekPanel(scene: SceneSpec, onDismiss: () -> Unit) {
    val label = stringResource(R.string.peek_hide)
    // The panel rises: scrim and picture fade up together and the picture
    // grows a half step into place. It leaves when asked, at once: a tap is
    // an answer, not a request.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(200, easing = LinearOutSlowInEasing)) }
    Box(
        modifier = Modifier
            .zIndex(4f) // above every piece tile (a held piece rides at 2)
            .fillMaxSize()
            .graphicsLayer { alpha = rise.value }
            .background(PuzzletColors.Scrim)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            val side = minOf(maxWidth * 0.78f, maxHeight * 0.78f)
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = 0.94f + 0.06f * rise.value
                        scaleY = 0.94f + 0.06f * rise.value
                    }
                    .background(PuzzletColors.Card, RoundedCornerShape(30.dp))
                    .padding(9.dp),
            ) {
                ScenePicture(
                    spec = scene,
                    modifier = Modifier.width(side),
                    cornerRadius = 22.dp,
                )
            }
        }
    }
}
