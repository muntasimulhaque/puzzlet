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

    /**
     * Map piece-unit coordinates the way paintBrandPiece does: the piece
     * occupies the centre span of the canvas, never the whole canvas.
     */
    private fun mapper(size: Int, spanFraction: Double): (Double, Double) -> Pair<Int, Int> {
        val scale = size * spanFraction / BRAND_SPAN
        val ox = size / 2.0 - scale / 2.0
        return { u: Double, v: Double -> Pair((ox + u * scale).toInt(), (ox + v * scale).toInt()) }
    }

    @Test
    fun `legacy tile is teal with the piece on it`() {
        val icon = legacyIcon(192)
        val p = mapper(192, IconDesign.LEGACY_SPAN_FRACTION)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // The piece body, clear of every cut: paper.
        val (bx, by) = p(0.5, 0.5)
        assertEquals(IconDesign.PAPER, icon.getRGB(bx, by))
        // A knob head reaching outward: paper.
        val (kx, ky) = p(0.5, 0.21)
        assertEquals(IconDesign.PAPER, icon.getRGB(kx, ky))
        // A cut head punched in the middle: the tile shows through.
        val (cx2, cy2) = p(0.5, 0.365)
        assertEquals(IconDesign.TEAL, icon.getRGB(cx2, cy2))
    }

    @Test
    fun `four arms reach outward and four cuts sit punched, symmetric`() {
        val size = 432
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(IconDesign.TEAL, true)
        g.fillRect(0, 0, size, size)
        paintBrandPiece(g, size / 2.0, size / 2.0, size * 0.80, IconDesign.PAPER)
        g.dispose()

        val p = mapper(size, 0.80)
        // The four knob heads: paper, at their outward positions.
        for ((u, v) in listOf(
            0.5 to 0.21, 0.79 to 0.5, 0.5 to 0.79, 0.21 to 0.5,
        )) {
            val (x, y) = p(u, v)
            assertEquals("knob head at ($x, $y) is not paper", IconDesign.PAPER, image.getRGB(x, y))
        }
        // The four cut heads: tile teal through the punched blanks.
        for ((u, v) in listOf(
            0.5 to 0.365, 0.635 to 0.5, 0.5 to 0.635, 0.365 to 0.5,
        )) {
            val (x, y) = p(u, v)
            assertEquals("cut head at ($x, $y) is not punched", IconDesign.TEAL, image.getRGB(x, y))
        }
        // The cut mouths (toward the centre): also teal.
        for ((u, v) in listOf(
            0.5 to 0.44, 0.56 to 0.5, 0.5 to 0.56, 0.44 to 0.5,
        )) {
            val (x, y) = p(u, v)
            assertEquals("cut mouth at ($x, $y) is not punched", IconDesign.TEAL, image.getRGB(x, y))
        }
        // Between the cuts, the piece itself is still paper.
        val (bx, by) = p(0.5, 0.5)
        assertEquals(IconDesign.PAPER, image.getRGB(bx, by))
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
        // A cut head: punched, the canvas shows through.
        val (cx2, cy2) = p(0.5, 0.365)
        assertEquals(0, layer.getRGB(cx2, cy2) ushr 24)
        // The monochrome sibling renders the same geometry in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(bx, by))
    }
}
