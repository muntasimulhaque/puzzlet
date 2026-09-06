package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.puzzlet.R

/** The toy-box palette. Chosen, not defaulted; see AGENTS.md, Design seeds. */
object PuzzletColors {
    val Teal = Color(0xFF0C7A64)
    val Paper = Color(0xFFFAF6EF)
    val Ink = Color(0xFF1F2B28)
    /** The celebration accent; it arrives with things worth celebrating. */
    val Honey = Color(0xFFF0B429)
    val Coral = Color(0xFFE4572E)
    val Sky = Color(0xFF7FB8D4)
    /** Card surfaces that sit a step above the paper ground. */
    val Card = Color(0xFFFFFDF9)
    /** The tray surface: one step deeper than the paper, where pieces wait. */
    val Tray = Color(0xFFF2EBE0)
}

// The display face: Baloo 2, bundled offline (OFL text lives in docs/).
private val Baloo = FontFamily(
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

// Two voices only, until gameplay asks for more: the name speaks in Baloo
// ExtraBold, everything else stays plain and legible.
private val BrandTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 70.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    // The piece counts on the shelf: small, but still Baloo.
    titleMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
)

private val BrandScheme = lightColorScheme(
    primary = PuzzletColors.Teal,
    onPrimary = PuzzletColors.Paper,
    background = PuzzletColors.Paper,
    onBackground = PuzzletColors.Ink,
    surface = PuzzletColors.Paper,
    onSurface = PuzzletColors.Ink,
)

@Composable
fun PuzzletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandScheme,
        typography = BrandTypography,
        content = content,
    )
}
