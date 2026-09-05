package io.github.muntasimulhaque.puzzlet

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.muntasimulhaque.puzzlet.host.PuzzleHost
import io.github.muntasimulhaque.puzzlet.host.Screen
import io.github.muntasimulhaque.puzzlet.ui.DifficultyChooser
import io.github.muntasimulhaque.puzzlet.ui.Gallery
import io.github.muntasimulhaque.puzzlet.ui.PlayActions
import io.github.muntasimulhaque.puzzlet.ui.PlayScreen
import io.github.muntasimulhaque.puzzlet.ui.PuzzletTheme

/** The most the toy-box lets system font scaling grow its words. */
private const val MAX_FONT_SCALE = 1.3f

class MainActivity : ComponentActivity() {

    private val host: PuzzleHost by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer {
                    val app = checkNotNull(
                        get(ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY),
                    )
                    PuzzleHost(app)
                }
            },
        )[PuzzleHost::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug builds police themselves: disk and network on the main
        // thread, leaks, unclosed resources. Release builds never see this.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build(),
            )
        }
        keepBarsHidden()
        setContent {
            val screen by host.screen.collectAsStateWithLifecycle()
            val muted by host.muted.collectAsStateWithLifecycle()
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
                    when (val s = screen) {
                        Screen.Home -> Gallery(
                            onChoose = host::choose,
                            hasProgress = host::hasProgress,
                            muted = muted,
                            onToggleMute = host::toggleMuted,
                        )
                        is Screen.Choose -> DifficultyChooser(
                            sceneId = s.sceneId,
                            onBack = host::home,
                            onPlay = { rows, cols -> host.play(s.sceneId, rows, cols) },
                        )
                        is Screen.Playing -> PlayScreen(
                            game = s.game,
                            draggedId = s.draggedId,
                            pulseId = s.pulseId,
                            pulseAt = s.pulseAt,
                            actions = playActions(),
                            onBack = host::backToChoose,
                        )
                    }
                }
            }
        }
    }

    private fun playActions() = PlayActions(
        onGrabAt = host::grabAt,
        onDragTo = host::dragTo,
        onDrop = host::drop,
        onLayout = host::layout,
        onRestart = host::restart,
        onBack = host::backToChoose,
    )

    override fun onStart() {
        super.onStart()
        keepBarsHidden()
    }

    // The shelf copy of an unfinished game goes to disk here, so a process
    // death mid-play costs at most the current picture's scattered piles.
    override fun onStop() {
        super.onStop()
        host.persistNow()
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
