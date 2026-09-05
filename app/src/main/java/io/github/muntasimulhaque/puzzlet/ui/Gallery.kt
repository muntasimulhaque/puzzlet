package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.SceneSpec

/** Spoken picture names for TalkBack. The cards themselves stay wordless. */
internal fun sceneNameRes(sceneId: String): Int = when (sceneId) {
    "sail" -> R.string.scene_sail
    "rocket" -> R.string.scene_rocket
    "house" -> R.string.scene_house
    "lighthouse" -> R.string.scene_lighthouse
    "balloon" -> R.string.scene_balloon
    "train" -> R.string.scene_train
    "castle" -> R.string.scene_castle
    "fruit" -> R.string.scene_fruit
    else -> R.string.app_name
}

/**
 * The picture menu: pictures only, edge to edge. A child who cannot read
 * chooses everything, so no headline stands between the child and the
 * pictures; the launcher and the store already carry the name. The sound
 * switch floats quietly where a parent's thumb falls.
 */
@Composable
fun Gallery(
    onChoose: (String) -> Unit,
    hasProgress: (String) -> Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth < 480.dp -> 1
                maxWidth < 840.dp -> 2
                else -> 3
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 84.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(Scenes.all) { scene ->
                    SceneCard(
                        scene = scene,
                        showProgress = hasProgress(scene.id),
                        onClick = { onChoose(scene.id) },
                    )
                }
            }
        }
        CircleButton(
            onClick = onToggleMute,
            background = PuzzletColors.Card,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp),
            label = stringResource(if (muted) R.string.sound_off else R.string.sound_on),
        ) {
            if (muted) SoundOffIcon(color = PuzzletColors.Ink) else SoundOnIcon(color = PuzzletColors.Ink)
        }
    }
}

@Composable
private fun SceneCard(scene: SceneSpec, showProgress: Boolean, onClick: () -> Unit) {
    val name = stringResource(sceneNameRes(scene.id))
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(PuzzletColors.Card)
            .semantics { contentDescription = name }
            .clickable(onClick = onClick),
    ) {
        ScenePicture(
            spec = scene,
            modifier = Modifier.fillMaxSize().padding(10.dp),
            cornerRadius = 20.dp,
        )
        if (showProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(PuzzletColors.Honey),
            )
        }
    }
}

/** A round pressable used across the app. [label] names it for TalkBack. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
