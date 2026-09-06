package io.github.muntasimulhaque.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.hypot

class MakeIconsTest {

    @Test
    fun `regeneration is byte-identical`() {
        val a = Files.createTempDirectory("icons-a").toFile()
        val b = Files.createTempDirectory("icons-b").toFile()
        writeIcons(a)
        writeIcons(b)
        for (file in iconFiles()) {
            val fa = File(a, file.relativePath)
            val fb = File(b, file.relativePath)
            assertTrue("Missing ${file.relativePath} in first run", fa.exists())
            assertTrue("Missing ${file.relativePath} in second run", fb.exists())
            assertTrue(
                "${file.relativePath} is not deterministic across runs",
                fa.readBytes().contentEquals(fb.readBytes()),
            )
        }
        a.deleteRecursively()
        b.deleteRecursively()
    }

    /** Map unit space the way paintLayer does for a full-art tile. */
    private fun tileMapper(size: Int): (Double, Double) -> Pair<Int, Int> {
        val span = size * IconDesign.TILE_SPAN
        val o = size / 2.0 - 0.5 * span
        return { u: Double, v: Double -> Pair((o + u * span).toInt(), (o + v * span).toInt()) }
    }

    /** Map unit space the way paintLayer does for the adaptive foreground. */
    private fun fgMapper(size: Int): (Double, Double) -> Pair<Int, Int> {
        val span = size * IconDesign.FG_SPAN_DP / IconDesign.ADAPTIVE_DP
        val o = size / 2.0 - 0.5 * span
        return { u: Double, v: Double -> Pair((o + u * span).toInt(), (o + v * span).toInt()) }
    }

    @Test
    fun `legacy tile carries the boat on teal`() {
        val icon = legacyIcon(192)
        val p = tileMapper(192)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // Far corner water, away from the boat: the teal tile itself.
        val (bx, by) = p(0.08, 0.92)
        assertEquals("background at ($bx, $by) is not teal", IconDesign.TEAL, icon.getRGB(bx, by))
        // The honey sun.
        val (sx, sy) = p(0.76, 0.20)
        assertEquals("sun at ($sx, $sy) is not honey", IconDesign.HONEY, icon.getRGB(sx, sy))
        // The main sail: paper.
        val (mx, my) = p(0.59, 0.51)
        assertEquals("mainsail at ($mx, $my) is not paper", IconDesign.PAPER, icon.getRGB(mx, my))
        // The hull: coral.
        val (hx, hy) = p(0.50, 0.72)
        assertEquals("hull at ($hx, $hy) is not coral", IconDesign.CORAL, icon.getRGB(hx, hy))
    }

    @Test
    fun `boat survives inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val p = fgMapper(size)
        val cx = size / 2.0
        fun inside(u: Double, v: Double): Boolean {
            val (x, y) = p(u, v)
            return hypot(x - cx, y - cx) <= size * 66.0 / 108.0
        }
        assertTrue("sail tip leaves the mask circle", inside(0.53, 0.30))
        assertTrue("hull leaves the mask circle", inside(0.50, 0.72))
        val (mx, my) = p(0.59, 0.51)
        assertEquals(IconDesign.PAPER, layer.getRGB(mx, my))
        val (hx, hy) = p(0.50, 0.72)
        assertEquals(IconDesign.CORAL, layer.getRGB(hx, hy))
    }

    @Test
    fun `adaptive layer is transparent canvas with the boat only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        val p = fgMapper(432)
        // Far from the boat: untouched canvas.
        val (rx, ry) = p(0.05, 0.50)
        assertEquals(0, layer.getRGB(rx, ry) ushr 24)
        // The sail: paper.
        val (bx, by) = p(0.59, 0.51)
        assertEquals(IconDesign.PAPER, layer.getRGB(bx, by))
        // The monochrome sibling renders the same silhouette in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
        val (hx, hy) = p(0.50, 0.72)
        assertEquals(IconDesign.WHITE, mono.getRGB(hx, hy))
    }
}
