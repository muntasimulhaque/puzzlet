package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes

/** One size for every coin in the play top bar, so back, picture and
 *  sound can never drift apart (D-078). */
private val TOP_BAR_COIN = 48.dp

/** Back on the left; the picture coin and the sound switch on the right. */
@Composable
internal fun PlayTopBar(
    game: Puzzle,
    peeking: Boolean,
    soundOn: Boolean,
    onPeek: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scene = remember(game.sceneId) { Scenes.byId(game.sceneId) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleButton(
            onClick = onBack,
            background = PuzzletColors.Card,
            size = TOP_BAR_COIN,
            label = stringResource(R.string.go_back),
        ) {
            BackIcon(color = PuzzletColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        // The picture coin steps inboard and the sound switch takes the
        // right corner (D-078).
        if (game.completed) {
            Spacer(Modifier.size(TOP_BAR_COIN))
        } else {
            PeekCoin(scene = scene, peeking = peeking, onPeek = onPeek)
        }
        Spacer(Modifier.width(10.dp))
        SoundCoin(on = soundOn, onToggle = onSound)
    }
}

/** The sound switch: one quiet coin beside the picture coin (D-077). */
@Composable
private fun SoundCoin(on: Boolean, onToggle: (Boolean) -> Unit) {
    CircleButton(
        onClick = { onToggle(!on) },
        background = PuzzletColors.Card,
        size = TOP_BAR_COIN,
        label = stringResource(if (on) R.string.sound_on else R.string.sound_off),
    ) {
        SpeakerIcon(on = on, color = PuzzletColors.Ink)
    }
}

@Composable
private fun PeekCoin(scene: SceneSpec, peeking: Boolean, onPeek: (Boolean) -> Unit) {
    val label = stringResource(if (peeking) R.string.peek_hide else R.string.peek_show)
    CircleButton(
        onClick = { onPeek(!peeking) },
        background = if (peeking) PuzzletColors.sceneWash(scene.accent) else PuzzletColors.Card,
        size = TOP_BAR_COIN,
        label = label,
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxSize().padding(8.dp),
            // A circle inside the round coin, so the thumbnail reads as part
            // of the coin rather than a sticker sitting on it.
            cornerRadius = 16.dp,
        )
    }
}
