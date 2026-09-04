package app.puzzlet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.puzzlet.ui.BrandScreen
import app.puzzlet.ui.PuzzletTheme

/** The most the toy-box lets system font scaling grow its words. */
private const val MAX_FONT_SCALE = 1.3f

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepBarsHidden()
        setContent {
            // A toy-box, not a document: text follows the system font setting,
            // but only so far. Past this cap the words stop fitting the fixed
            // play surfaces and begin to overlap them, which serves nobody, so
            // the whole UI is composed under a bounded density instead.
            val system = LocalDensity.current
            val capped = remember(system.fontScale) {
                Density(density = system.density, fontScale = minOf(system.fontScale, MAX_FONT_SCALE))
            }
            CompositionLocalProvider(LocalDensity provides capped) {
                PuzzletTheme {
                    BrandScreen()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) keepBarsHidden()
    }

    // A child's toy-box owns the whole screen. Edge-to-edge is enforced at
    // this targetSdk, so without some handling the screen draws under the
    // status and navigation bars. We hide both bars for an immersive,
    // distraction-free surface; they only flash back transiently on a swipe.
    private fun keepBarsHidden() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
