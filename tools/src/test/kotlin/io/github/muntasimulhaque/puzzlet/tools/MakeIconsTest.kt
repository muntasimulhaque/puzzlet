package io.github.muntasimulhaque.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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

    /** A point in the mark's own frame, in body units from the centre. */
    private fun markPoint(size: Int, u: Double, v: Double): Pair<Int, Int> {
        val s = size.toDouble()
        val body = s * PieceDesign.BODY_FRAC
        val cx = s / 2.0
        val cy = s / 2.0
        val a = Math.toRadians(PieceDesign.TILT_DEG)
        val rx = u * body
        val ry = v * body
        return (cx + rx * cos(a) - ry * sin(a)).toInt() to (cy + rx * sin(a) + ry * cos(a)).toInt()
    }

    private fun alphaAt(image: java.awt.image.BufferedImage, u: Double, v: Double): Int {
        val (x, y) = markPoint(image.width, u, v)
        return image.getRGB(x, y) ushr 24
    }

    private fun rgbAt(image: java.awt.image.BufferedImage, u: Double, v: Double): Int {
        val (x, y) = markPoint(image.width, u, v)
        return image.getRGB(x, y)
    }

    @Test
    fun `the mark carries two tabs and two blanks around the sail`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        // The tab tips are paper rim.
        assertEquals("top tab tip", PieceDesign.PAPER, rgbAt(layer, 0.0, -0.68))
        assertEquals("right tab tip", PieceDesign.PAPER, rgbAt(layer, 0.68, 0.0))
        // The blanks are open: the ground shows through.
        assertEquals("left blank", 0, alphaAt(layer, -0.37, 0.0))
        assertEquals("bottom blank", 0, alphaAt(layer, 0.0, 0.37))
        // The sailboat's hull sits inside the piece.
        assertEquals("hull", PieceDesign.CORAL, rgbAt(layer, 0.0, 0.06))
    }

    @Test
    fun `the foreground survives inside the launcher mask circle`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        val cx = size / 2.0
        // The 66 dp safe circle plus 2 px of antialiasing tolerance: the
        // mark reaches the safe circle on purpose, so the probe checks
        // solid art only; the soft shadow may graze the mask edge.
        val limit = size * 33.0 / 108.0 + 2.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((layer.getRGB(x, y) ushr 24) > 200) {
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
    fun `adaptive layer is transparent canvas with the mark only`() {
        val size = 432
        val layer = adaptiveLayer(size, IconDesign.PAPER)
        // Far from the mark: untouched canvas.
        assertEquals(0, layer.getRGB(4, 4) ushr 24)
        assertEquals(0, layer.getRGB(size - 5, 4) ushr 24)
        // The hull is the mark's content.
        assertEquals(PieceDesign.CORAL, rgbAt(layer, 0.0, 0.06))
        // The monochrome sibling is the piece silhouette in white.
        val mono = adaptiveLayer(size, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, rgbAt(mono, 0.0, 0.0))
        assertEquals(0, mono.getRGB(4, 4) ushr 24)
        // The blanks stay open in the silhouette too.
        assertEquals(0, alphaAt(mono, -0.37, 0.0))
    }
}
