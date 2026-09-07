package io.github.muntasimulhaque.puzzlet.tools

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Working scratch for the icon round, never shipped and never committed
 * as art: four gather takes at launcher size plus their 48 px selves, so
 * the owner can point at one. Take A is the approved mark byte for byte;
 * B tightens the shared gap; C tightens and stands the pieces straight;
 * D goes tighter still. Same four colours throughout, flat fills only.
 * Delete this file and its task once the round is decided (house rule).
 */
object MakeGatherSheet {

    private data class Take(val name: String, val gapFrac: Double, val tilt: Double)

    private val takes = listOf(
        Take("A_current", IconDesign.GAP_FRAC, 1.0),
        Take("B_gap10", 0.10, 1.0),
        Take("C_gap10_straight", 0.10, 0.0),
        Take("D_gap06_straight", 0.06, 0.0),
    )

    fun render(outDir: File) {
        outDir.mkdirs()
        for (take in takes) {
            val big = Gather.paint(
                512, tile = true, groundArgb = IconDesign.PAPER,
                gapFrac = take.gapFrac, tilt = take.tilt,
            )
            ImageIO.write(big, "png", File(outDir, "take_${take.name}_512.png"))
            ImageIO.write(small(big, 48), "png", File(outDir, "take_${take.name}_48.png"))
        }
        println("makeGatherSheet: wrote ${takes.size * 2} files under ${outDir.path}")
    }

    private fun small(src: BufferedImage, px: Int): BufferedImage {
        val out = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(src, 0, 0, px, px, null)
        g.dispose()
        return out
    }
}

fun main(args: Array<String>) {
    MakeGatherSheet.render(File(File(args[0]), "build/gather-sheet"))
}
