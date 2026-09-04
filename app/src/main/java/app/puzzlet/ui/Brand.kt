package app.puzzlet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.puzzlet.R

/**
 * The M0 home: the mark, the name, the promise. Nothing pretends to be a
 * button, because nothing plays yet; this screen states what the app is
 * while gameplay is built (AGENTS.md, The game).
 */
@Composable
fun BrandScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = PuzzletColors.Teal,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = PuzzletColors.Ink,
            )
        }
    }
}

/** The launcher tile, held up large: the same committed PNG, one source of truth. */
@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(176.dp)
            .clip(RoundedCornerShape(percent = 22))
            .background(PuzzletColors.Teal),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null, // decorative; the tagline carries the words
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
