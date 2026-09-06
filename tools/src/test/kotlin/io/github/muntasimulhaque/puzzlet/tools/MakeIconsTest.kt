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
    fun `the tab is a mushroom, neck narrower than head`() {
        val d = IconDesign
        assertTrue("neck must be narrower than the head", d.TAB_NECK_HALF < d.TAB_HEAD_R)
        assertEquals("tab tip", d.TAB_HEAD_CY - d.TAB_HEAD_R, 0.15, 1e-9)
    }

    @Test
    fun `legacy tile carries the piece with its boat`() {
        val icon = legacyIcon(192)
        val p = tileMapper(192)
        // Far outside the piece: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // The tab head, reaching up: teal.
        val (tx, ty) = p(0.50, 0.16)
        assertEquals("tab at ($tx, $ty) is not teal", IconDesign.TEAL, icon.getRGB(tx, ty))
        // The socket bite, carved from the right edge: open canvas.
        val (sx, sy) = p(0.84, 0.55)
        assertEquals("socket at ($sx, $sy) must be transparent", 0, icon.getRGB(sx, sy) ushr 24)
        // The honey sun.
        val (ux, uy) = p(0.63, 0.42)
        assertEquals("sun at ($ux, $uy) is not honey", IconDesign.HONEY, icon.getRGB(ux, uy))
        // The main sail: paper.
        val (mx, my) = p(0.55, 0.55)
        assertEquals("mainsail at ($mx, $my) is not paper", IconDesign.PAPER, icon.getRGB(mx, my))
        // The hull: coral.
        val (hx, hy) = p(0.50, 0.67)
        assertEquals("hull at ($hx, $hy) is not coral", IconDesign.CORAL, icon.getRGB(hx, hy))
    }

    @Test
    fun `tab tip and hull survive inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val p = fgMapper(size)
        val cx = size / 2.0
        fun inside(u: Double, v: Double): Boolean {
            val (x, y) = p(u, v)
            return hypot(x - cx, y - cx) <= size * 66.0 / 108.0
        }
        assertTrue("tab tip leaves the mask circle", inside(0.50, 0.15))
        assertTrue("hull leaves the mask circle", inside(0.50, 0.67))
        val (tx, ty) = p(0.50, 0.16)
        assertEquals(IconDesign.TEAL, layer.getRGB(tx, ty))
        val (hx, hy) = p(0.50, 0.67)
        assertEquals(IconDesign.CORAL, layer.getRGB(hx, hy))
    }

    @Test
    fun `adaptive layer is transparent canvas with the piece only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        val p = fgMapper(432)
        // Far from the piece: untouched canvas.
        val (rx, ry) = p(0.05, 0.50)
        assertEquals(0, layer.getRGB(rx, ry) ushr 24)
        // The sail: paper.
        val (bx, by) = p(0.55, 0.55)
        assertEquals(IconDesign.PAPER, layer.getRGB(bx, by))
        // The monochrome sibling is the piece silhouette in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
        val (tx, ty) = p(0.50, 0.16)
        assertEquals(IconDesign.WHITE, mono.getRGB(tx, ty))
    }
}
