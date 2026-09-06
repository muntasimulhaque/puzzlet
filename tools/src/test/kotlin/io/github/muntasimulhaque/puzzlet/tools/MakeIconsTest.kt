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

    /** The gather frame: content origin, content span, piece side, gap, home origin, in px. */
    private data class Frame(
        val se: Double,
        val s: Double,
        val gap: Double,
        val hx: Double,
        val hy: Double,
    )

    /** Map canvas pixels the way Gather.paint lays out its field for an inset fraction. */
    private fun gatherFrame(size: Int, insetFrac: Double): Frame {
        val o = size * insetFrac
        val se = size * (1.0 - 2.0 * insetFrac)
        val s = se * 0.40
        val gap = s * IconDesign.GAP_FRAC
        return Frame(se, s, gap, o + se / 2.0 + gap / 2.0, o + se / 2.0 + gap / 2.0)
    }

    private fun fgFrame(size: Int): Frame = gatherFrame(size, IconDesign.FG_INSET)

    private fun tileFrame(size: Int): Frame =
        gatherFrame(size, (1.0 - IconDesign.TILE_SPAN) / 2.0)

    /** Piece body centres: home, top wanderer, left wanderer, corner wanderer. */
    private fun Frame.centres(): List<Triple<Double, Double, Int>> = listOf(
        Triple(hx + s / 2.0, hy + s / 2.0, IconDesign.HONEY),
        Triple(hx + s / 2.0, hy - s - gap + s / 2.0, IconDesign.CORAL),
        Triple(hx - s - gap + s / 2.0, hy + s / 2.0, IconDesign.HONEY_LIGHT),
        Triple(hx - s - gap + s / 2.0, hy - s - gap + s / 2.0, IconDesign.SKY),
    )

    @Test
    fun `the knob profile stays sane`() {
        val d = IconDesign
        assertTrue("stem must be narrower than the head", d.KNOB_STEM < d.KNOB_HEAD_R)
        assertTrue("the head must fit its edge half", d.KNOB_HEAD_C + d.KNOB_HEAD_R < 0.5)
        assertTrue("wanderers need a positive gap", d.GAP_FRAC > 0.0)
    }

    @Test
    fun `the gather holds four pieces in their colours`() {
        val size = 384
        val icon = legacyIcon(size)
        val f = tileFrame(size)
        val names = listOf("honey home", "coral top", "gold left", "sky corner")
        for ((i, c) in f.centres().withIndex()) {
            val x = c.first.toInt()
            val y = c.second.toInt()
            assertEquals("${names[i]} at ($x, $y)", c.third, icon.getRGB(x, y))
        }
    }

    @Test
    fun `the foreground survives inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val cx = size / 2.0
        // 32 dp of the 108 dp canvas: 1 dp of air inside the 66 dp mask.
        val limit = size * 32.0 / 108.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((layer.getRGB(x, y) ushr 24) > 16) {
                    assertTrue(
                        "art escapes the mask circle at ($x, $y)",
                        hypot(x - cx, y - cx) <= limit,
                    )
                }
            }
        }
    }

    @Test
    fun `tile art stays inside the tile silhouette`() {
        checkSilhouette(storeTile(512), 512, IconDesign.STORE_CORNER_FRACTION, "store")
        checkSilhouette(legacyIcon(192), 192, IconDesign.LEGACY_CORNER_FRACTION, "legacy")
    }

    private fun checkSilhouette(
        tile: java.awt.image.BufferedImage,
        size: Int,
        cornerFraction: Double,
        name: String,
    ) {
        // Signed distance outside the rounded tile, in px: antialiasing
        // paints a sub-pixel rim exactly on the boundary, so solid pixels
        // pass within a 1.5 px tolerance while a real poke stands far out.
        fun outside(x: Double, y: Double): Double {
            val r = size * cornerFraction
            val qx = kotlin.math.abs(x - size / 2.0) - (size / 2.0 - r)
            val qy = kotlin.math.abs(y - size / 2.0) - (size / 2.0 - r)
            val ax = kotlin.math.max(qx, 0.0)
            val ay = kotlin.math.max(qy, 0.0)
            return kotlin.math.hypot(ax, ay) + kotlin.math.min(kotlin.math.max(qx, qy), 0.0) - r
        }
        for (y in 0 until size step 2) {
            for (x in 0 until size step 2) {
                if ((tile.getRGB(x, y) ushr 24) > 200) {
                    assertTrue(
                        "$name art pokes past the tile silhouette at ($x, $y)",
                        outside(x.toDouble(), y.toDouble()) <= 1.5,
                    )
                }
            }
        }
    }

    @Test
    fun `adaptive layer is transparent canvas with the gather only`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        // Far from the gather: untouched canvas.
        assertEquals(0, layer.getRGB(4, 4) ushr 24)
        assertEquals(0, layer.getRGB(size - 5, 4) ushr 24)
        // A honey home body pixel.
        val f = fgFrame(size)
        val home = f.centres()[0]
        assertEquals(IconDesign.HONEY, layer.getRGB(home.first.toInt(), home.second.toInt()))
        // The monochrome sibling is the gather silhouette in white.
        val mono = adaptiveLayer(size, IconDesign.WHITE)
        for (c in f.centres()) {
            assertEquals(IconDesign.WHITE, mono.getRGB(c.first.toInt(), c.second.toInt()))
        }
        assertEquals(0, mono.getRGB(4, 4) ushr 24)
    }
}
