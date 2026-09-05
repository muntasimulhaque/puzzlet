package io.github.muntasimulhaque.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files

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

    @Test
    fun `legacy tile is teal with four joined pieces on it`() {
        val icon = legacyIcon(192)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // Map piece-unit coordinates the way paintBrandPiece does: the piece
        // occupies the centre span, not the whole canvas.
        val span = 192 * IconDesign.LEGACY_SPAN_FRACTION
        val scale = span / BRAND_SPAN
        val ox = 96.0 - scale / 2.0
        fun P(u: Double, v: Double): Pair<Int, Int> =
            Pair((ox + u * scale).toInt(), (ox + v * scale).toInt())
        // The seam cross in the middle: the tile shows through.
        val (sx, sy) = P(0.5, 0.5)
        assertEquals(IconDesign.TEAL, icon.getRGB(sx, sy))
        // A quadrant body: paper.
        val (bx, by) = P(0.29375, 0.29375)
        assertEquals(IconDesign.PAPER, icon.getRGB(bx, by))
        // A knob head reaching outward: paper.
        val (kx, ky) = P(0.29375, 0.065)
        assertEquals(IconDesign.PAPER, icon.getRGB(kx, ky))
        // A cut in the middle: the tile shows through the blank.
        val (cx2, cy2) = P(0.29375, 0.470)
        assertEquals(IconDesign.TEAL, icon.getRGB(cx2, cy2))
    }

    @Test
    fun `the four cuts sit in the middle, one per piece, pinwheel-symmetric`() {
        // Map piece-unit coordinates onto a rendered layer and probe all
        // four cuts and all four heads: heads paper, cuts tile.
        val size = 432
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(IconDesign.TEAL, true)
        g.fillRect(0, 0, size, size)
        paintBrandPiece(g, size / 2.0, size / 2.0, size * 0.80, IconDesign.PAPER)
        g.dispose()

        val scale = (size * 0.80) / BRAND_SPAN
        val ox = size / 2.0 - scale / 2.0
        fun P(u: Double, v: Double): Pair<Int, Int> =
            Pair((ox + u * scale).toInt(), (ox + v * scale).toInt())

        val cuts = listOf(
            P(0.29375, 0.470), P(0.530, 0.29375), P(0.70625, 0.530), P(0.470, 0.70625),
        )
        for ((x, y) in cuts) {
            assertEquals("the cut at ($x, $y) is not punched through", IconDesign.TEAL, image.getRGB(x, y))
        }
        val heads = listOf(
            P(0.29375, 0.065), P(0.935, 0.29375), P(0.70625, 0.935), P(0.065, 0.70625),
        )
        for ((x, y) in heads) {
            assertEquals("the head at ($x, $y) is not paper", IconDesign.PAPER, image.getRGB(x, y))
        }
        // Between the cuts, the pieces themselves are still paper.
        for ((x, y) in listOf(P(0.20, 0.20), P(0.80, 0.80))) {
            assertEquals(IconDesign.PAPER, image.getRGB(x, y))
        }
    }

    @Test
    fun `adaptive layer is transparent canvas with the piece only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        // Far corner: untouched canvas.
        assertEquals(0, layer.getRGB(20, 20) ushr 24)
        // Map piece-unit coordinates the way paintBrandPiece does.
        val span = 432 * IconDesign.ADAPTIVE_SPAN_DP / IconDesign.ADAPTIVE_DP
        val scale = span / BRAND_SPAN
        val ox = 216.0 - scale / 2.0
        fun P(u: Double, v: Double): Pair<Int, Int> =
            Pair((ox + u * scale).toInt(), (ox + v * scale).toInt())
        // The seam cross: the canvas shows through where four pieces meet.
        val (sx, sy) = P(0.5, 0.5)
        assertEquals(0, layer.getRGB(sx, sy) ushr 24)
        // A quadrant body: paper.
        val (bx, by) = P(0.29375, 0.29375)
        assertEquals(IconDesign.PAPER, layer.getRGB(bx, by))
        // The monochrome sibling renders the same geometry in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
    }
}
