package io.github.muntasimulhaque.puzzlet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The wash law (D-080): every picture's peek-coin tint is the picture's own
 * accent held in one measured band. A bold accent (the rocket's night sky,
 * the toadstool's cap) and a pale one (the ice cream's mint scoop) must come
 * out equally soft, and the hue must stay the picture's, because that is
 * what makes the button change from picture to picture.
 */
class SceneWashTest {

    @Test
    fun `every wash keeps its accent's hue in the one band`() {
        for (scene in Scenes.all) {
            val wash = softWash(scene.accent)
            val lab = labOf(wash)
            assertEquals("${scene.id} lightness", WASH_LIGHTNESS, lab.l, 1.5)
            assertEquals("${scene.id} chroma", WASH_CHROMA, lab.chroma, 1.5)
            val drift = hueGap(labOf(scene.accent).hue, lab.hue)
            assertTrue("${scene.id} hue drifted $drift degrees", drift <= 3.0)
        }
    }

    @Test
    fun `every accent is a colour the picture itself wears`() {
        for (scene in Scenes.all) {
            assertTrue(
                "${scene.id} declares an accent it never paints",
                scene.shapes.any { it.argb == scene.accent },
            )
        }
    }

    @Test
    fun `a wash is a softer step off its accent, and clear of the card`() {
        val card = labOf(0xFFFFFDF9)
        for (scene in Scenes.all) {
            val wash = softWash(scene.accent)
            // Softer means less colour, not always lighter: the beach's sand
            // is already paler than the band, and still softens to it.
            assertTrue(
                "${scene.id} wash is not softer than its accent",
                labOf(wash).chroma < labOf(scene.accent).chroma,
            )
            assertTrue(
                "${scene.id} wash is not a real step off its accent",
                deltaE(labOf(wash), labOf(scene.accent)) >= 5.0,
            )
            assertTrue(
                "${scene.id} wash melts into the card",
                deltaE(labOf(wash), card) >= 10.0,
            )
        }
    }

    @Test
    fun `the accents cover a spread of hues, so the coin really changes`() {
        // Five 30-degree families is the floor: the shelf must not hand the
        // child sixteen buttons that all look the same when they light up.
        val families = Scenes.all.map { (labOf(it.accent).hue / 30.0).toInt() }.distinct()
        assertTrue("only ${families.size} hue families across the shelf", families.size >= 5)
    }

    private fun hueGap(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }

    private fun deltaE(a: Lab, b: Lab): Double =
        sqrt((a.l - b.l) * (a.l - b.l) + (a.a - b.a) * (a.a - b.a) + (a.b - b.b) * (a.b - b.b))
}
