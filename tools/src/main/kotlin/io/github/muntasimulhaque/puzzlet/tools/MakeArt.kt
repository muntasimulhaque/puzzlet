package io.github.muntasimulhaque.puzzlet.tools

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Play Store art, drawn from the same gather as the launcher: lagoon
 * ground, the four pieces sitting straight on it with no card behind
 * them, and Baloo 2 lettering through the clean-text path so large words
 * never slice.
 *
 * The ground is the brand teal, the toy-box lid, at its true brightness
 * rather than the deeper shade it used to carry, and there is no ghosted
 * puzzle behind the wordmark fighting it for attention. One mark, one
 * name, one line, and air: parents decide in two seconds.
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 *
 * Content rules hold here too: no faces, no eyes, no creatures, no music
 * notes.
 */
object MakeArt {

    /** The 1024 x 500 feature graphic: the gather, the name, one line. */
    fun featureGraphic(rootDir: File): BufferedImage =
        featureOn(rootDir, IconDesign.LAGOON, IconDesign.PAPER, 0xD9FAF6EF.toInt())

    /**
     * One banner: the mark 340 px on the left, the type given the whole
     * right side, and nothing else competing with it. Ground, name and
     * line colours are the caller's, so a candidate sheet can try another
     * ground without a second copy of the layout.
     */
    internal fun featureOn(rootDir: File, groundArgb: Int, inkArgb: Int, softArgb: Int): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = Color(groundArgb)
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
        g.dispose()
        pasteArt(image, Gather.paint(340, tile = false, groundArgb = 0), 96, 80, 1.0f)
        val g2 = image.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        drawCleanString(g2, "Puzzlet", "baloo2_extrabold.ttf", 108f, inkArgb, 540f, 268f, rootDir)
        drawCleanString(g2, "A calm jigsaw for small hands.", "baloo2_bold.ttf", 28f, softArgb, 542f, 322f, rootDir)
        g2.dispose()
        return image
    }

    /** The 512 x 512 store icon: the launcher tile, full bleed. */
    fun storeIcon(): BufferedImage = storeTile(512)
}

private fun pasteArt(dst: BufferedImage, src: BufferedImage, x: Int, y: Int, alpha: Float) {
    val g = dst.createGraphics()
    g.composite = AlphaComposite.SrcOver.derive(alpha)
    g.drawImage(src, x, y, null)
    g.dispose()
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val outDir = File(rootDir, "play-store")
    outDir.mkdirs()
    ImageIO.write(MakeArt.featureGraphic(rootDir), "png", File(outDir, "feature-graphic-1024x500.png"))
    ImageIO.write(MakeArt.storeIcon(), "png", File(outDir, "play-icon-512.png"))
    println("makeArt: wrote the feature graphic and the store icon under ${outDir.path}")
}
