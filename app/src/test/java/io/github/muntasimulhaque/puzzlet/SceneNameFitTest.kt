package io.github.muntasimulhaque.puzzlet

import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every picture name must fit its shelf card at the smallest size the
 * auto-size text steps down to, even on the narrowest two-column phone
 * (360 dp) under the 1.3 font-scale cap.
 *
 * The shelf card used to clip the last letter of a long name: Mushroom
 * read as Mushroo and Lighthouse as Lighthous, because the name was one
 * fixed line at the title size. The shelf now measures the longest name
 * and gives every card that same size (Gallery.kt, rememberNameFontSize),
 * stepping the whole row down together, never past 16 sp. This test pins
 * that floor with the same font the app bundles: if a future name cannot
 * fit at 16 sp, the test fails before a child ever sees a clipped word.
 */
class SceneNameFitTest {

    /** The shipped scene names, read from the one strings file. */
    private fun sceneNames(): List<String> {
        val file = File("src/main/res/values/strings.xml")
        assertTrue("strings.xml not found at ${file.absolutePath}", file.isFile)
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).mapNotNull { i ->
            val el = nodes.item(i)
            val name = el.attributes?.getNamedItem("name")?.nodeValue
            if (name != null && name.startsWith("scene_")) el.textContent.trim() else null
        }
    }

    @Test
    fun `every scene name fits the narrowest shelf card`() {
        val names = sceneNames()
        assertTrue("no scene names found in strings.xml", names.size >= 16)
        val font = Font.createFont(Font.TRUETYPE_FONT, File("src/main/res/font/baloo2_bold.ttf"))
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        // Worst case: a 360 dp phone, two columns, 20 dp page margins, a
        // 16 dp gap and 10 dp of card padding each side leaves 132 dp of
        // text width. The auto-size floor is 16 sp.
        val availablePx = 132f
        for (name in names) {
            val width = font.deriveFont(16f).getStringBounds(name, g.fontRenderContext).width
            assertTrue(
                "$name is ${width.toInt()} px wide at 16 sp, past the 132 dp card",
                width <= availablePx,
            )
        }
        g.dispose()
    }
}
