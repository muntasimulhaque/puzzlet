package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.SceneSpec

/**
 * One picture card: the picture, its name, and the quiet line naming the
 * count it opens at. The whole card is one button (D-070): picture, name
 * and count all open the chooser, so a three-year-old never has to find
 * the picture inside the plate. TalkBack speaks the name once for the
 * whole card; the sizes it can play at live in the chooser it opens.
 */
@Composable
internal fun SceneCard(
    scene: SceneSpec,
    pieces: Int,
    nameSize: TextUnit,
    onOpen: () -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    val count = stringResource(R.string.pieces_count, pieces)
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .buttonShadow(shape)
            .clip(shape)
            .background(PuzzletColors.Card)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "$name, $count" }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        ) {
            ScenePicture(spec = scene, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp)
        }
        CardName(name = name, size = nameSize)
        QuietCount(pieces = pieces)
    }
}

@Composable
private fun CardName(name: String, size: TextUnit) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = size),
        color = PuzzletColors.Ink,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** The quiet line under a name: the count this picture opens at. */
@Composable
private fun QuietCount(pieces: Int) {
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.pieces_count, pieces),
        style = MaterialTheme.typography.bodyMedium,
        color = PuzzletColors.Ink.copy(alpha = 0.70f),
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}
