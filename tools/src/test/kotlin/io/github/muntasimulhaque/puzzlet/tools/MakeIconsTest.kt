package io.github.muntasimulhaque.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    /** Head centres derived from the generator consts, never hardcoded. */
    private fun knobHeadX() = IconDesign.SEAM_X + IconDesign.BITE_D + 0.015
    private fun socketHeadX() = IconDesign.SEAM_X - IconDesign.BITE_D - 0.015

    @Test
    fun `both knobs share one size and equal gaps to each other and the edges`() {
        val d = IconDesign
        val top = d.SOCKET_Y - d.BITE_R
        val mid = d.KNOB_Y - d.SOCKET_Y - 2 * d.BITE_R
        val bottom = 1.0 - d.KNOB_Y - d.BITE_R
        assertEquals(top, mid, 1e-9)
        assertEquals(mid, bottom, 1e-9)
    }

    @Test
    fun `legacy tile carries the seam with knob and socket`() {
        val icon = legacyIcon(192)
        val p = tileMapper(192)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // The paper field, left of the seam.
        val (fx, fy) = p(0.25, 0.5)
        assertEquals(IconDesign.PAPER, icon.getRGB(fx, fy))
        // The hero knob head, reaching right: paper.
        val (kx, ky) = p(knobHeadX(), IconDesign.KNOB_Y)
        assertEquals("knob head at ($kx, $ky) is not paper", IconDesign.PAPER, icon.getRGB(kx, ky))
        // The socket bite, opening above the knob: not paper.
        val (sx, sy) = p(socketHeadX(), IconDesign.SOCKET_Y)
        assertNotEquals("socket at ($sx, $sy) should be carved out", IconDesign.PAPER, icon.getRGB(sx, sy))
        // Far right of the seam: tile, never paper.
        val (tx, ty) = p(0.92, 0.5)
        assertNotEquals(IconDesign.PAPER, icon.getRGB(tx, ty))
    }

    @Test
    fun `knob and socket survive inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val p = fgMapper(size)
        val cx = size / 2.0
        fun inside(u: Double, v: Double): Boolean {
            val (x, y) = p(u, v)
            return hypot(x - cx, y - cx) <= size * 66.0 / 108.0
        }
        assertTrue("knob leaves the mask circle", inside(knobHeadX(), IconDesign.KNOB_Y))
        assertTrue("socket leaves the mask circle", inside(socketHeadX(), IconDesign.SOCKET_Y))
        val (kx, ky) = p(knobHeadX(), IconDesign.KNOB_Y)
        assertEquals(IconDesign.PAPER, layer.getRGB(kx, ky))
    }

    @Test
    fun `adaptive layer is transparent canvas with the seam only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        val p = fgMapper(432)
        // Far right of the seam: untouched canvas. (The macro field
        // bleeds top and bottom by design; the mask crops it.)
        val (rx, ry) = p(0.97, 0.5)
        assertEquals(0, layer.getRGB(rx, ry) ushr 24)
        // The paper field: paper.
        val (bx, by) = p(0.25, 0.5)
        assertEquals(IconDesign.PAPER, layer.getRGB(bx, by))
        // The socket bite: carved open. Only the seam's own soft shadow may
        // veil it, never paper and never more than a breath of deep.
        val (sx, sy) = p(socketHeadX(), IconDesign.SOCKET_Y)
        val socketArgb = layer.getRGB(sx, sy)
        assertNotEquals("socket at ($sx, $sy) must not be paper", IconDesign.PAPER, socketArgb)
        assertTrue("socket at ($sx, $sy) must stay essentially open, alpha was ${socketArgb ushr 24}", (socketArgb ushr 24) < 48)
        // The monochrome sibling renders the same silhouette in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
        val (mx, my) = p(knobHeadX(), IconDesign.KNOB_Y)
        assertEquals(IconDesign.WHITE, mono.getRGB(mx, my))
    }
}
