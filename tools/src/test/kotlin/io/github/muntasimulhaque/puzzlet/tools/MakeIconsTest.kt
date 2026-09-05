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

    /** Map piece-unit coordinates the way paintBrandPiece does. */
    private fun mapper(size: Int, spanFraction: Double): (Double, Double) -> Pair<Int, Int> {
        val scale = size * spanFraction / BRAND_SPAN
        val ox = size / 2.0 - scale / 2.0
        return { u: Double, v: Double -> Pair((ox + u * scale).toInt(), (ox + v * scale).toInt()) }
    }

    @Test
    fun `legacy tile is teal with the plain four-armed piece on it`() {
        val icon = legacyIcon(192)
        val p = mapper(192, IconDesign.LEGACY_SPAN_FRACTION)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // The piece body: paper.
        val (bx, by) = p(0.5, 0.5)
        assertEquals(IconDesign.PAPER, icon.getRGB(bx, by))
        // The four knob heads, reaching outward: paper.
        for ((u, v) in listOf(0.5 to 0.21, 0.79 to 0.5, 0.5 to 0.79, 0.21 to 0.5)) {
            val (x, y) = p(u, v)
            assertEquals("knob head at ($x, $y) is not paper", IconDesign.PAPER, icon.getRGB(x, y))
        }
        // Where cuts once were: plain paper again, no holes.
        for ((u, v) in listOf(0.5 to 0.365, 0.635 to 0.5, 0.5 to 0.635, 0.365 to 0.5)) {
            val (x, y) = p(u, v)
            assertEquals("a hole survives at ($x, $y)", IconDesign.PAPER, icon.getRGB(x, y))
        }
    }

    @Test
    fun `the four arms are perfectly symmetric`() {
        val size = 432
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(IconDesign.TEAL, true)
        g.fillRect(0, 0, size, size)
        paintBrandPiece(g, size / 2.0, size / 2.0, size * 0.80, IconDesign.PAPER)
        g.dispose()

        val p = mapper(size, 0.80)
        // All four knob heads: paper.
        for ((u, v) in listOf(0.5 to 0.21, 0.79 to 0.5, 0.5 to 0.79, 0.21 to 0.5)) {
            val (x, y) = p(u, v)
            assertEquals("knob head at ($x, $y) is not paper", IconDesign.PAPER, image.getRGB(x, y))
        }
        // The four necks, halfway out: paper.
        for ((u, v) in listOf(0.5 to 0.36, 0.64 to 0.5, 0.5 to 0.64, 0.36 to 0.5)) {
            val (x, y) = p(u, v)
            assertEquals("neck at ($x, $y) is not paper", IconDesign.PAPER, image.getRGB(x, y))
        }
        // The diagonals between arms: tile, and the centre: paper.
        for ((u, v) in listOf(0.30 to 0.30, 0.70 to 0.30, 0.30 to 0.70, 0.70 to 0.70)) {
            val (x, y) = p(u, v)
            assertEquals(IconDesign.PAPER, image.getRGB(x, y))
        }
    }

    @Test
    fun `adaptive layer is transparent canvas with the piece only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        val p = mapper(432, IconDesign.ADAPTIVE_SPAN_DP / IconDesign.ADAPTIVE_DP)
        // Far corner: untouched canvas.
        assertEquals(0, layer.getRGB(20, 20) ushr 24)
        // The piece body: paper.
        val (bx, by) = p(0.5, 0.5)
        assertEquals(IconDesign.PAPER, layer.getRGB(bx, by))
        // No punched holes: where cuts once were, the piece is solid.
        val (cx2, cy2) = p(0.5, 0.365)
        assertEquals(IconDesign.PAPER, layer.getRGB(cx2, cy2))
        // The monochrome sibling renders the same geometry in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
    }
}
