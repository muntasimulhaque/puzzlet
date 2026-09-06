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
    fun `the knob is die true, circular head on tangent stems`() {
        val d = IconDesign
        assertTrue("stem must be narrower than the head", d.KNOB_NECK < d.KNOB_HEAD_R)
        assertTrue("fillet must fit inside the stem", d.KNOB_FIL < d.KNOB_NECK)
    }

    @Test
    fun `the block holds four pieces, red gold yellow orange`() {
        val d = IconDesign
        val x0 = d.BLOCK_X
        val y0 = d.BLOCK_Y
        val s = d.PIECE
        val g = d.GAP
        // Centred square block.
        assertEquals(x0, 1.0 - (x0 + s * 2.0 + g), 1e-9)
        assertEquals(y0, 1.0 - (y0 + s * 2.0 + g), 1e-9)
        val icon = legacyIcon(192)
        val p = tileMapper(192)
        // Piece bodies.
        val (rx, ry) = p(x0 + 0.10, y0 + 0.10)
        assertEquals("red at ($rx, $ry)", IconDesign.RED, icon.getRGB(rx, ry))
        val (gx, gy) = p(x0 + s + g + 0.30, y0 + 0.10)
        assertEquals("gold at ($gx, $gy)", IconDesign.GOLD, icon.getRGB(gx, gy))
        val (yx, yy) = p(x0 + 0.10, y0 + s + g + 0.10)
        assertEquals("yellow at ($yx, $yy)", IconDesign.YELLOW, icon.getRGB(yx, yy))
        val (ox, oy) = p(x0 + s + g + 0.30, y0 + s + g + 0.10)
        assertEquals("orange at ($ox, $oy)", IconDesign.ORANGE, icon.getRGB(ox, oy))
        // The groove between the columns.
        val (sx, sy) = p(x0 + s, y0 + 0.10)
        assertEquals("seam at ($sx, $sy)", IconDesign.SEAM, icon.getRGB(sx, sy))
    }

    @Test
    fun `block survives inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val p = fgMapper(size)
        val cx = size / 2.0
        fun inside(u: Double, v: Double): Boolean {
            val (x, y) = p(u, v)
            return hypot(x - cx, y - cx) <= size * 66.0 / 108.0
        }
        val d = IconDesign
        val mid = d.BLOCK_X + d.PIECE + d.GAP / 2.0
        assertTrue("block middle leaves the mask circle", inside(mid, 0.30))
        val (rx, ry) = p(d.BLOCK_X + 0.10, d.BLOCK_Y + 0.10)
        assertEquals(IconDesign.RED, layer.getRGB(rx, ry))
    }

    @Test
    fun `adaptive layer is transparent canvas with the block only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        val p = fgMapper(432)
        // Far from the block: untouched canvas.
        val (rx, ry) = p(0.02, 0.02)
        assertEquals(0, layer.getRGB(rx, ry) ushr 24)
        // A gold body pixel.
        val d = IconDesign
        val (gx, gy) = p(d.BLOCK_X + d.PIECE + d.GAP + 0.30, d.BLOCK_Y + 0.10)
        assertEquals(IconDesign.GOLD, layer.getRGB(gx, gy))
        // The monochrome sibling is the block silhouette in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(gx, gy))
    }
}
