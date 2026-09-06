package io.github.muntasimulhaque.puzzlet.tools

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Play Store art, drawn from the same gather as the launcher: deep
 * lagoon ground, the gather tile, a macro gather bleeding off the edge,
 * and Baloo 2 lettering through the clean-text path so large words
 * never slice.
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 *
 * Content rules hold here too: no faces, no eyes, no creatures, no music
 * notes. One hero tile, one name, one line: parents decide in two seconds.
 */
object MakeArt {

    /** The 1024 x 500 feature graphic: the gather tile, the name, one line. */
    fun featureGraphic(rootDir: File): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = Color(IconDesign.DEEP)
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
        g.dispose()
        pasteArt(image, Gather.paint(640, tile = false, groundArgb = 0, gapFrac = 0.55), 500, 44, 0.35f)
        pasteArt(image, Gather.paint(370, tile = true, groundArgb = IconDesign.DEEP, gapFrac = 0.17), 76, 65, 1.0f)
        val g2 = image.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        drawCleanString(g2, "Puzzlet", "baloo2_extrabold.ttf", 116f, IconDesign.PAPER, 490f, 252f, rootDir)
        drawCleanString(g2, "A calm jigsaw for small hands.", "baloo2_bold.ttf", 28f, 0xD9FAF6EF.toInt(), 494f, 330f, rootDir)
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
