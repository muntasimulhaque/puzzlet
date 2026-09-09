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
import io.github.muntasimulhaque.puzzlet.host.ShelfState
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

    /**
     * The eight store captures, the Play listing maximum per form factor
     * (24 across the three form factors). The set leads with the shelf,
     * shows the field from four pieces to sixteen, the carry, the finish,
     * the picture held up, and the cut chooser with its marked size.
     */
    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        lateinit var pane: Pane
        scenario.onActivity { pane = Pane.from(it) }
        captureShelf(scenario, outDir)
        capturePlay(scenario, outDir, pane)
        captureOverlays(scenario, outDir, pane)
        captureChooser(scenario, outDir)
        scenario.close()
    }

    /** 01: the shelf, every shipped picture with its name and count line. */
    private fun captureShelf(scenario: ActivityScenario<ComponentActivity>, outDir: File) {
        shot(scenario, outDir, "01_home") {
            Gallery(ShelfState(), {}, { _, _ -> })
        }
    }

    /** 02, 03, 04 and 07: the field from the first pieces to sixteen. */
    private fun capturePlay(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        pane: Pane,
    ) {
        // Four huge sailboat pieces, barely begun.
        val four = buildGame(pane, "sail", 2, 2)
        shot(scenario, outDir, "02_play_4") {
            PlayScreen(four, null, -1, 0L, 0L, false, false, true, noActions, {}, {}, {})
        }
        // Mid-game on the nine-piece house: five of nine placed.
        val nine = buildGame(pane, "house", 3, 3, placed = (0..4).toSet())
        shot(scenario, outDir, "03_play_9") {
            PlayScreen(nine, null, -1, 0L, 0L, false, false, true, noActions, {}, {}, {})
        }
        // A waiting piece in hand, carried toward the board. The host never
        // lets a placed piece be held, and drawing one twice reads as a glitch.
        val dragging = dragState(nine, pieceId = 7, at = Vec2(pane.w * 0.22, pane.h * 0.52))
        shot(scenario, outDir, "04_play_drag") {
            PlayScreen(dragging.game, 7, -1, 0L, 0L, false, false, true, noActions, {}, {}, {})
        }
        // Sixteen pieces: the biggest count the shelf offers.
        val sixteen = buildGame(pane, "balloon", 4, 4, placed = (0..5).toSet())
        shot(scenario, outDir, "07_play_16") {
            PlayScreen(sixteen, null, -1, 0L, 0L, false, false, true, noActions, {}, {}, {})
        }
    }

    /** 05 and 06: the finish, and the picture held up over the field. */
    private fun captureOverlays(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        pane: Pane,
    ) {
        // The finish: complete, held up with confetti falling. One quiet beat
        // on the table comes first in the real game (D-066); the plate is what
        // this still-life hosts.
        val done = buildGame(pane, "sail", 2, 2, placed = (0 until 4).toSet())
        shot(scenario, outDir, "05_celebration") {
            PlayScreen(done, null, -1, 0L, 0L, false, true, true, noActions, {}, {}, {})
        }
        // Looking at the picture: the board stays blank, the panel holds the
        // whole thing up over the field, and the picture coin keeps its own
        // wash while the child looks (D-080).
        val peek = buildGame(pane, "sail", 3, 3, placed = (0..1).toSet())
        shot(scenario, outDir, "06_play_peek") {
            PlayScreen(peek, null, -1, 0L, 0L, true, false, true, noActions, {}, {}, {})
        }
    }

    /**
     * 08: the cut chooser on a picture with one win and no parent pick. The
     * marked tile is the ladder's 6, and tapping it deals 6: the mark must
     * read exactly what the plain path will play (the 4-that-opened-6 bug,
     * pinned here).
     */
    private fun captureChooser(scenario: ActivityScenario<ComponentActivity>, outDir: File) {
        shot(scenario, outDir, "08_choose") {
            Gallery(
                shelf = ShelfState(wins = mapOf("balloon" to 1)),
                onChoose = {},
                onChooseAt = { _, _ -> },
                openChooserFor = "balloon",
            )
        }
    }

    /** Render one state, settle it, and copy the window's own pixels out. */
    private fun shot(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        name: String,
        block: @Composable () -> Unit,
    ) {
        push(block)
        lateinit var bitmap: Bitmap
        scenario.onActivity { activity -> bitmap = captureWindow(activity) }
        File(outDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
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
        onDropAt = { false },
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
