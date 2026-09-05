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
    fun `legacy tile is teal with the paper piece at its heart`() {
        val icon = legacyIcon(192)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // Tile, above the piece: teal.
        assertEquals(IconDesign.TEAL, icon.getRGB(96, 12))
        // Body centre: paper.
        assertEquals(IconDesign.PAPER, icon.getRGB(96, 96))
        // The top arm's head: paper, where the design says it must be.
        val span = 192 * IconDesign.LEGACY_SPAN_FRACTION
        val headDy = (0.29 * span / BRAND_SPAN).toInt()
        assertEquals(IconDesign.PAPER, icon.getRGB(96, 96 - headDy))
    }

    @Test
    fun `the four arms reach the same distance in every direction`() {
        // The owner's design is perfectly symmetric: probe all four knob
        // heads and require the same colour, and require the diagonals
        // between the arms to stay empty.
        val size = 432
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        paintBrandPiece(g, size / 2.0, size / 2.0, size * 0.80, IconDesign.PAPER)
        g.dispose()

        val c = size / 2.0
        val head = 0.29 / BRAND_SPAN * (size * 0.80)
        val probes = listOf(
            (c + head).toInt() to c.toInt(),   // right
            c.toInt() to (c - head).toInt(),   // top
            (c - head).toInt() to c.toInt(),   // left
            c.toInt() to (c + head).toInt(),   // bottom
        )
        for ((x, y) in probes) {
            assertEquals("arm at ($x, $y) is not paper", IconDesign.PAPER, image.getRGB(x, y))
        }
        val diagonal = (c - head).toInt()
        assertTrue(
            "the diagonal between arms should be empty",
            image.getRGB(diagonal, diagonal) ushr 24 == 0,
        )
    }

    @Test
    fun `adaptive layer is transparent canvas with the piece only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        // Far corner: untouched canvas.
        assertEquals(0, layer.getRGB(20, 20) ushr 24)
        // Centre: the piece body, in the requested colour.
        assertEquals(IconDesign.PAPER, layer.getRGB(216, 216))
        // The monochrome sibling renders the same geometry in white.
        val mono = adaptiveLayer(432, IconDesign.WHITE)
        assertEquals(IconDesign.WHITE, mono.getRGB(216, 216))
    }
}
