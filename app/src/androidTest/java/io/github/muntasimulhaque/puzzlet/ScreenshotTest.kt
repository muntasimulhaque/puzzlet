package io.github.muntasimulhaque.puzzlet

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.restorePuzzle
import io.github.muntasimulhaque.puzzlet.host.Screen
import io.github.muntasimulhaque.puzzlet.ui.Gallery
import io.github.muntasimulhaque.puzzlet.ui.PlayActions
import io.github.muntasimulhaque.puzzlet.ui.PlayScreen
import io.github.muntasimulhaque.puzzlet.ui.PuzzletTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped UI, for the Play Store listing and for
 * the human drift check after UI changes (AGENTS.md, Build).
 *
 * These are ordinary state renders, not a live playthrough: each screen is
 * handed straight to its composable inside a bare activity, which is only
 * possible because no composable in this app takes a ViewModel. The harness
 * deliberately avoids the compose test rule and everything under it: no
 * touch injection and no semantics queries are needed to render and copy
 * pixels, and dropping that machinery keeps these captures working on
 * whatever framework image the app targets, forever.
 *
 * The PNGs are written to the directory the instrumentation reports as
 * additional test output; the screenshots workflow
 * (.github/workflows/screenshots.yml) pulls them off the emulator and
 * prefixes each with the form factor.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private fun resolveOutDir(): File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (path != null) {
            val dir = File(path)
            if (dir.isDirectory || dir.mkdirs()) return dir
            // Cold-booted emulators can lag mounting shared storage; fall
            // back rather than fail.
        }
        return File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath,
        ).apply { mkdirs() }
    }

    /** One activity hosts every scene: each is a state change pushed into it. */
    private val render = mutableStateOf<@Composable () -> Unit>({})

    private fun launch(): ActivityScenario<ComponentActivity> {
        // Right after a cold boot the package manager can briefly refuse to
        // resolve; a short retry absorbs it without masking real breakage.
        var lastError: RuntimeException? = null
        repeat(3) { attempt ->
            try {
                val scenario = ActivityScenario.launch(ComponentActivity::class.java)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    activity.setContent {
                        PuzzletTheme {
                            render.value()
                        }
                    }
                }
                settle()
                return scenario
            } catch (e: RuntimeException) {
                lastError = e
                Thread.sleep(5000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("could not launch the host activity")
    }

    private fun push(block: @Composable () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { render.value = block }
        settle()
    }

    /** Drain the main thread, then give animations a beat to land. */
    private fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(SETTLE_MS)
    }

    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        lateinit var pane: Pane
        scenario.onActivity { pane = Pane.from(it) }

        fun shot(name: String, block: @Composable () -> Unit) {
            push(block)
            lateinit var bitmap: Bitmap
            scenario.onActivity { activity -> bitmap = captureWindow(activity) }
            File(outDir, "$name.png").outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // The shelf: the four shipped pictures with their names.
        shot("01_home") {
            Gallery(onChoose = {})
        }

        // A chunky game, barely begun: four huge sailboat pieces.
        val four = buildGame(pane, "sail", 2, 2)
        shot("02_play_4") {
            PlayScreen(four, null, -1, 0L, 0L, noActions, {})
        }

        // Mid-game on the nine-piece house: five of nine placed.
        val nine = buildGame(pane, "house", 3, 3, placed = (0..4).toSet())
        shot("03_play_9") {
            PlayScreen(nine, null, -1, 0L, 0L, noActions, {})
        }

        // A piece in hand, carried from the tray toward the board. It must
        // be a waiting piece: the host never lets a placed piece be held,
        // and drawing a placed piece twice reads as a glitch.
        val dragging = dragState(nine, pieceId = 7, at = Vec2(pane.w * 0.22, pane.h * 0.52))
        shot("04_play_drag") {
            PlayScreen(dragging.game, 7, -1, 0L, 0L, noActions, {})
        }

        // The finish: the picture complete, held up with confetti falling.
        val done = buildGame(pane, "sail", 2, 2, placed = (0 until 4).toSet())
        shot("05_celebration") {
            PlayScreen(done, null, -1, 0L, 0L, noActions, {})
        }

        // The fruit plate at nine pieces: six placed, texture galore.
        val fruit = buildGame(pane, "fruit", 3, 3, placed = (0..5).toSet())
        shot("06_play_fruit") {
            PlayScreen(fruit, null, -1, 0L, 0L, noActions, {})
        }

        scenario.close()
    }

    // -- Fixtures -----------------------------------------------------------

    /** Window truth: full-bleed pixels and the real density. */
    private class Pane(val w: Int, val h: Int, val density: Float) {
        companion object {
            fun from(activity: ComponentActivity): Pane {
                val decor = activity.window.decorView
                return Pane(
                    w = decor.width,
                    h = decor.height,
                    density = activity.resources.displayMetrics.density,
                )
            }
        }
    }

    /** A game laid out for exactly this window, with saved pieces seated. */
    private fun buildGame(pane: Pane, sceneId: String, rows: Int, cols: Int, placed: Set<Int> = emptySet()): Puzzle {
        val topBarPx = (64 * pane.density).toDouble()
        val field = Area(0.0, 0.0, pane.w.toDouble(), pane.h - topBarPx)
        return restorePuzzle(sceneId, rows, cols, placed, field, 560.0 * pane.density, seed = 7L)
    }

    /** The drag still-life: a piece in hand, carried toward the board. */
    private fun dragState(base: Puzzle, pieceId: Int, at: Vec2): Screen.Playing {
        val game = base.copy(
            pieces = base.pieces.map { piece ->
                if (piece.id == pieceId) piece.copy(current = at - piece.size * 0.5) else piece
            },
        )
        return Screen.Playing(game, draggedId = pieceId)
    }

    private val noActions = PlayActions(
        onGrabAt = { _, _ -> null },
        onDragTo = {},
        onDrop = { false },
        onLayout = { _, _ -> },
        onRestart = {},
    )

    /** The activity's own window pixels: the truth the child actually sees. */
    private fun captureWindow(activity: ComponentActivity): Bitmap {
        val decor = activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        PixelCopy.request(activity.window, bitmap, { result ->
            if (result != PixelCopy.SUCCESS) {
                // Software draw as the fallback path; static scenes render fine.
                decor.draw(android.graphics.Canvas(bitmap))
            }
            latch.countDown()
        }, Handler(Looper.getMainLooper()))
        latch.await(10, TimeUnit.SECONDS)
        return bitmap
    }

    private companion object {
        const val SETTLE_MS = 600L
    }
}
