package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.starPoints

/**
 * One picture card: the picture, its name, and one honey star once the
 * picture has been finished at least once.
 *
 * The card used to carry a quiet line naming the count it opens at, under
 * every name on the shelf, all sixteen of them saying four. The count is
 * real information and it still lives one tap away, in the chooser, where
 * it is a numeral under the real cut and where a parent can do something
 * with it; repeated sixteen times on the shelf it was the only gray text a
 * child could not read, so it is gone (D-086). What is left is the picture
 * and the name.
 *
 * In its place the card keeps one record: a star for a finished picture.
 * It counts nothing, it can never go down, and it is the child's own
 * answer to "which ones have I done?" (D-086).
 *
 * The whole card is one button (D-070): picture, name and star all open the
 * chooser, so a three-year-old never has to find the picture inside the
 * plate. TalkBack speaks the name and the count once for the whole card.
 */
@Composable
internal fun SceneCard(
    scene: SceneSpec,
    pieces: Int,
    won: Boolean,
    nameSize: TextUnit,
    onOpen: () -> Unit,
) {
    val name = stringResource(sceneNameRes(scene.id))
    val count = stringResource(R.string.pieces_count, pieces)
    val spoken = stringResource(
        if (won) R.string.card_desc_won else R.string.card_desc,
        name,
        count,
    )
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .buttonShadow(shape)
            .clip(shape)
            .background(PuzzletColors.Card)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = spoken }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardPicture(scene = scene, won = won)
        CardName(name = name, size = nameSize)
    }
}

/** The picture, with the one mark the shelf keeps on a finished one. */
@Composable
private fun CardPicture(scene: SceneSpec, won: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        ScenePicture(spec = scene, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp)
        if (won) {
            WinStar(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
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
    Spacer(Modifier.height(2.dp))
}

/**
 * The one record the shelf keeps: a honey star, edged in card white so it
 * sits on any picture. A mark of a thing done, not a score: there is no
 * count under it, nothing to beat, and no way for it to go down. It sits
 * at the picture's lower corner, where every scene keeps its calmer
 * ground, so the badge never lands on a sun or a moon.
 *
 * The edge is a scaled copy of the star, not a stroke: a round-joined
 * stroke puffs the five points into a sticker, while a second star under
 * the first keeps every point as crisp as the confetti's.
 */
@Composable
private fun WinStar(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Canvas(modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Vec2(radius.toDouble(), radius.toDouble())
        drawPath(starPath(center, radius * 0.98), PuzzletColors.Card)
        drawPath(starPath(center, radius * 0.98 * 0.78), PuzzletColors.Honey)
    }
}

/** One five-point star with a point straight up, filled, at [rOuter]. */
private fun starPath(center: Vec2, rOuter: Double): Path {
    val points = starPoints(center, rOuter, rOuter * 0.45, 5, rotationDeg = -90.0)
    return Path().apply {
        moveTo(points[0].x.toFloat(), points[0].y.toFloat())
        for (i in 1 until points.size) lineTo(points[i].x.toFloat(), points[i].y.toFloat())
        close()
    }
}
