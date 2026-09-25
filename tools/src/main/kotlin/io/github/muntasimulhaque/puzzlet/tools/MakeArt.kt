package io.github.muntasimulhaque.puzzlet.tools

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Play Store art, drawn from the same mark as the launcher: the brand
 * teal ground, the die-cut piece sitting straight on it with no card
 * behind it, and Baloo 2 lettering through the clean-text path so large
 * words never slice.
 *
 * The ground is the brand teal, the toy-box lid. One mark, one name, one
 * line, and air: parents decide in two seconds.
 *
 * Every element stays inside the cutoff-safe box (StoreArt.kt). Play
 * crops the 1024 x 500 on some surfaces, and the owner's own phone showed
 * it: the name's tail was sliced off. The art fits the box now, so the
 * piece, the name and the line survive every surface, and the teal runs
 * to every edge so a rounded corner crop only rounds teal.
 *
 * The file is written as a 24-bit PNG with no alpha, which is what Play
 * asks for; an opaque ARGB file still carries a channel it should not.
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
    fun featureGraphic(rootDir: File): BufferedImage = composeBanner(featureLayer(rootDir))

    /**
     * The shipped banner's lockup, before the fitter: the piece left, the
     * name over the line right, one center line. The sizes are the
     * drawing's proportions; the fitter gives it the box.
     */
    internal fun featureLayer(rootDir: File): BufferedImage {
        val piece = markArt(228.0)
        val name = textArt(rootDir, STORE_NAME, STORE_EXTRA, 122f, STORE_PAPER)
        val line = textArt(rootDir, STORE_LINE, STORE_BOLD, 30f, STORE_SOFT)
        val gap = 46
        val textW = maxOf(name.width, line.width)
        val layer = transparentCanvas()
        val g = graphics(layer)
        val left = (STORE_W - (piece.width + gap + textW)) / 2
        val textCx = left + piece.width + gap + textW / 2
        val cy = STORE_H / 2
        drawAt(g, piece, left + piece.width / 2, cy)
        drawAt(g, name, textCx, cy - (line.height + 16) / 2)
        drawAt(g, line, textCx, cy + (name.height + 16) / 2)
        g.dispose()
        return layer
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
