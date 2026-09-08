package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Play Store art, drawn from the same mark as the launcher: lagoon
 * ground, the die-cut piece sitting straight on it with no card behind it,
 * and Baloo 2 lettering through the clean-text path so large words never
 * slice.
 *
 * The ground is the brand teal, the toy-box lid, and there is no ghosted
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

    /** The 1024 x 500 feature graphic: the mark, the name, one line. */
    fun featureGraphic(rootDir: File): BufferedImage =
        featureOn(rootDir, IconDesign.PAPER, 0xE6FAF6EF.toInt())

    /**
     * One banner: the mark 500 px on the left, the type given the whole
     * right side, and nothing else competing with it. The mark ground is
     * the brand teal; the name and line colours are the caller's.
     */
    internal fun featureOn(
        rootDir: File,
        inkArgb: Int,
        softArgb: Int,
        markSize: Int = 500,
        markX: Int = 30,
        markY: Int = 0,
        nameSize: Float = 122f,
        nameX: Float = 556f,
        nameY: Float = 262f,
        tagSize: Float = 30f,
        tagX: Float = 558f,
        tagY: Float = 324f,
    ): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = Color(0xFF0C7A64.toInt(), true)
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
        val mark = BufferedImage(markSize, markSize, BufferedImage.TYPE_INT_ARGB)
        val mg = mark.createGraphics()
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        mg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        paintMark(mg, markSize)
        mg.dispose()
        g.drawImage(mark, markX, markY, null)
        drawCleanString(g, "Puzzlet", "baloo2_extrabold.ttf", nameSize, inkArgb, nameX, nameY, rootDir)
        drawCleanString(g, "A calm jigsaw for small hands.", "baloo2_bold.ttf", tagSize, softArgb, tagX, tagY, rootDir)
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
