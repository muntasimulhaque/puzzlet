package io.github.muntasimulhaque.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The feature graphic's one law: every element sits inside the
 * cutoff-safe box, so no Play surface can slice the name or the piece.
 * The box clears Play's own cutoff zones (15 percent a side) and the
 * harshest common card crop, 4:3, which takes 17.4 percent off each side.
 */
class MakeArtTest {

    private val rootDir: File = File("..").absoluteFile

    @Test
    fun `feature graphic is the Play size and has no alpha`() {
        val image = MakeArt.featureGraphic(rootDir)
        assertEquals(1024, image.width)
        assertEquals(500, image.height)
        assertTrue(
            "the feature graphic must be 24-bit PNG, no alpha channel",
            image.type == java.awt.image.BufferedImage.TYPE_INT_RGB,
        )
    }

    @Test
    fun `feature graphic ink stays inside the cutoff-safe box`() {
        checkInsideBox(MakeArt.featureGraphic(rootDir), "the feature graphic")
    }

    @Test
    fun `feature graphic fills the box on its tighter axis`() {
        val image = MakeArt.featureGraphic(rootDir)
        val (x0, x1, y0, y1) = safeBox().toList()
        val ink = bannerInkRect(image)
        val usedW = (ink[1] - ink[0] + 1).toDouble() / (x1 - x0 + 1)
        val usedH = (ink[3] - ink[2] + 1).toDouble() / (y1 - y0 + 1)
        // The fitter sizes to the tighter axis; the other keeps the air.
        // If neither axis is close to full, the art is shrinking away
        // from a box it should own, and the take should be re-metricked.
        assertTrue("the lockup underfills the safe box: $usedW x $usedH", maxOf(usedW, usedH) > 0.9)
    }

    @Test
    fun `the 4 to 3 card crop keeps the piece and the whole name`() {
        val image = MakeArt.featureGraphic(rootDir)
        val w = (500 * 4.0 / 3.0).toInt()
        val card = image.getSubimage((1024 - w) / 2, 0, w, 500)
        // The lockup sits clear of the card's own edges: the owner's crop
        // sliced the tail off the name, and that is the failure this law
        // exists to stop.
        val ink = bannerInkRect(card)
        assertTrue("the card crop lost the left of the art", ink[0] >= 2)
        assertTrue("the card crop lost the right of the art", ink[1] <= card.width - 3)
        assertTrue("the card crop lost the top of the art", ink[2] >= 2)
        assertTrue("the card crop lost the bottom of the art", ink[3] <= card.height - 3)
    }
}
