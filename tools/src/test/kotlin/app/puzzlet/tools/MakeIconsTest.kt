package app.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun `legacy tile is teal with a paper glyph`() {
        val icon = legacyIcon(192)
        // Outside the rounded corners: fully transparent.
        assertEquals(0, icon.getRGB(8, 8) ushr 24)
        // Tile, above the glyph: teal.
        assertEquals(IconDesign.TEAL, icon.getRGB(96, 12))
        // Glyph body, near the canvas centre: paper.
        assertEquals(IconDesign.PAPER, icon.getRGB(96, 96))
    }

    @Test
    fun `adaptive layer is transparent canvas with a glyph only`() {
        val layer = adaptiveLayer(432, IconDesign.PAPER)
        // Far corner: untouched canvas.
        assertEquals(0, layer.getRGB(20, 20) ushr 24)
        // Centre: the glyph body, in the requested colour.
        assertEquals(IconDesign.PAPER, layer.getRGB(216, 216))
    }
}
