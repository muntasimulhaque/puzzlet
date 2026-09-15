package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.R

/**
 * Forgiving back (D-057): the first back press while pieces are placed
 * asks, the second leaves. Empty board leaves at once, finished leaves
 * at once, peek closes first. Tapping outside the card stays.
 */
@Composable
internal fun LeaveConfirm(onStay: () -> Unit, onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PuzzletColors.Scrim)
            .clickable(role = Role.Button, onClick = onStay),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(PuzzletColors.Card)
                // Swallow taps on the card without adding a semantics node:
                // the pointer input eats them, TalkBack never sees them.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.leave_title),
                style = MaterialTheme.typography.titleLarge,
                color = PuzzletColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.leave_message),
                style = MaterialTheme.typography.bodyLarge,
                color = PuzzletColors.Ink,
            )
            Spacer(Modifier.height(18.dp))
            LeaveButtons(onStay = onStay, onLeave = onLeave)
        }
    }
}

@Composable
private fun LeaveButtons(onStay: () -> Unit, onLeave: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .buttonShadow(shape)
                .clip(shape)
                .background(PuzzletColors.Teal)
                .clickable(role = Role.Button, onClick = onStay)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.stay),
                style = MaterialTheme.typography.titleMedium,
                color = PuzzletColors.Paper,
            )
        }
        Box(
            modifier = Modifier
                .buttonShadow(shape)
                .clip(shape)
                .background(PuzzletColors.Tray)
                .clickable(role = Role.Button, onClick = onLeave)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.leave),
                style = MaterialTheme.typography.titleMedium,
                color = PuzzletColors.Ink,
            )
        }
    }
}
