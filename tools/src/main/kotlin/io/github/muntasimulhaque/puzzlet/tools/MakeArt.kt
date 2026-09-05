package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Play Store art, drawn from the same mark as the launcher: lagoon
 * teal, warm paper, the S seam (knob and socket), and Baloo 2 lettering
 * through the clean-text path so large words never slice.
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 *
 * Content rules hold here too: no faces, no eyes, no creatures, no music
 * notes. The background is quiet: one calm falloff, no lattice behind
 * the words.
 */
object MakeArt {

    private const val HONEY = 0xFFF0B429.toInt()

    /** The 1024 x 500 feature graphic: the S tile, calm type, one accent. */
    fun featureGraphic(rootDir: File): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = GradientPaint(0f, 0f, Color(IconDesign.TEAL), 0f, h.toFloat(), Color(IconDesign.DEEP))
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
        g.drawImage(storeTile(360), 96, 70, null)
        drawCleanString(g, "Puzzlet", "baloo2_extrabold.ttf", 132f, IconDesign.PAPER, 500f, 272f, rootDir)
        g.color = Color(HONEY)
        g.fill(RoundRectangle2D.Double(504.0, 298.0, 150.0, 13.0, 6.5, 6.5))
        drawCleanString(g, "A calm jigsaw for small hands.", "baloo2_bold.ttf", 36f, 0xE6FAF6EF.toInt(), 504f, 360f, rootDir)
        g.dispose()
        return image
    }

    /** The 512 x 512 store icon: the launcher tile, full bleed. */
    fun storeIcon(): BufferedImage = storeTile(512)
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val outDir = File(rootDir, "play-store")
    outDir.mkdirs()
    ImageIO.write(MakeArt.featureGraphic(rootDir), "png", File(outDir, "feature-graphic-1024x500.png"))
    ImageIO.write(MakeArt.storeIcon(), "png", File(outDir, "play-icon-512.png"))
    println("makeArt: wrote the feature graphic and the store icon under ${outDir.path}")
}
