package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R

/** One size for both finish coins; Again leads by ground, not size
 *  (D-079). The Teal and Tray pair is the same primary and secondary
 *  recipe the leave confirm already uses. */
private val FINISH_COIN = 64.dp

/** Equal coins; Again leads by the brand teal, the way Stay leads. */
@Composable
internal fun FinishButtons(onAgain: () -> Unit, onHome: () -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        FinishCoin(
            onClick = onAgain,
            background = PuzzletColors.Teal,
            label = stringResource(R.string.restart),
            text = stringResource(R.string.again),
        ) {
            ReplayIcon(color = PuzzletColors.Paper, size = 28.dp)
        }
        Spacer(Modifier.width(40.dp))
        FinishCoin(
            onClick = onHome,
            background = PuzzletColors.Tray,
            label = stringResource(R.string.home),
            text = stringResource(R.string.home),
        ) {
            MenuIcon(color = PuzzletColors.Ink, size = 28.dp)
        }
    }
}

/** One finish coin: one size, one shadow, the ground carries the lead. */
@Composable
private fun FinishCoin(
    onClick: () -> Unit,
    background: Color,
    label: String,
    text: String,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleButton(
            onClick = onClick,
            background = background,
            size = FINISH_COIN,
            label = label,
        ) {
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PuzzletColors.Ink,
        )
    }
}
