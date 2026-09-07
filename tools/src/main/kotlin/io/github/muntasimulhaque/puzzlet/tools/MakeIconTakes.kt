package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot

/**
 * The icon round: from-scratch takes of the mark, rendered as images so the
 * owner points at one instead of reading prose about them (the D-036
 * lesson). Working scratch under build/, never committed; the generator
 * and its task are swept once the round is decided.
 *
 * Every take is the gather family or a plain sibling of it, flat fills of
 * plain paths only, so any winner can become the shipped icon with byte
 * pins that hold across JDKs (AGENTS.md, Lessons). Output: one contact
 * sheet and one 512 tile per take, under build/icon-takes.
 */
object MakeIconTakes {

    /** One take: a name and the way its gather is laid out. */
    data class Take(
        val id: String,
        val gapFrac: Double,
        val pieceScale: Double,
        val tiltsDeg: List<Double>,
        val span: Double,
    )

    val TAKES: List<Take> = listOf(
        // The shipped mark, for comparison: gap 0.17, pieces at 0.40.
        Take("a-current", 0.17, 0.40, listOf(-7.0, 6.0, -5.0), 0.88),
        // The hover: the knobs stop a hair outside the open sockets, so the
        // bites stay clean and the story is the moment before the click.
        Take("g-hover", 0.26, 0.40, listOf(-7.0, 6.0, -5.0), 0.88),
        // The hover, with the pieces grown to fill the tile.
        Take("h-hover-big", 0.26, 0.42, listOf(-7.0, 6.0, -5.0), 0.94),
        // The hover, calm: less wobble in the wanderers.
        Take("i-hover-calm", 0.26, 0.42, listOf(-4.0, 4.0, -3.0), 0.94),
        // Dead straight: the four pieces squared, a fresh box on the table.
        Take("e-square", 0.08, 0.42, listOf(0.0, 0.0, 0.0), 0.90),
        // The loose, toy-scatter feel: more tilt, honest gap.
        Take("f-playful", 0.13, 0.40, listOf(-9.0, 8.0, -6.0), 0.88),
    )

    /** One 512 store tile of a take: paper tile, the gather on it. */
    fun tile(take: Take, sizePx: Int): java.awt.image.BufferedImage =
        Gather.paint(
            size = sizePx,
            tile = true,
            groundArgb = IconDesign.PAPER,
            cornerFraction = IconDesign.STORE_CORNER_FRACTION,
            gapFrac = take.gapFrac,
            insetFrac = (1.0 - take.span) / 2.0,
            pieceScale = take.pieceScale,
            tiltsDeg = take.tiltsDeg,
        )

    /** One adaptive foreground: the take on transparency, for mask checks. */
    fun foreground(take: Take, sizePx: Int): java.awt.image.BufferedImage =
        Gather.paint(
            size = sizePx,
            tile = false,
            groundArgb = 0,
            gapFrac = take.gapFrac,
            insetFrac = IconDesign.FG_INSET,
            pieceScale = take.pieceScale,
            tiltsDeg = take.tiltsDeg,
        )
}

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/icon-takes")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }

    for (take in MakeIconTakes.TAKES) {
        ImageIO.write(MakeIconTakes.tile(take, 512), "png", File(outDir, "${take.id}-512.png"))
        ImageIO.write(MakeIconTakes.foreground(take, 432), "png", File(outDir, "${take.id}-fg.png"))
    }

    // The contact sheet: one row per take, big tile then small tiles, so the
    // launcher sizes are judged too, not only the poster.
    val margin = 36
    val big = 300
    val small = 96
    val label = 40
    val row = margin * 2 + big + 16 + small * 2 + 16
    val width = margin * 2 + big + 16 + small * 2 + 16 + 48
    val height = margin * 2 + MakeIconTakes.TAKES.size * (big + label + 18)
    val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.paint = Color(IconDesign.PAPER, true)
    g.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
    var y = margin
    for (take in MakeIconTakes.TAKES) {
        g.clipRect(0, 0, width, height)
        drawCleanString(
            g, take.id, "baloo2_bold.ttf", 26f, Color(IconDesign.INK).rgb,
            margin.toFloat(), (y + 26).toFloat(), rootDir,
        )
        g.drawImage(MakeIconTakes.tile(take, big), margin, y + label, null)
        g.drawImage(MakeIconTakes.tile(take, small), margin + big + 16, y + label, null)
        g.drawImage(MakeIconTakes.tile(take, 48), margin + big + 16 + small + 16, y + label, null)
        // The adaptive foreground, pasted on paper at its real inset.
        val fg = MakeIconTakes.foreground(take, small)
        g.drawImage(fg, margin + big + 16 + small + 16 + 48 + 16, y + label, null)
        y += big + label + 18
    }
    g.dispose()
    ImageIO.write(image, "png", File(outDir, "take-sheet.png"))

    // Mask math: how far each take's foreground reaches from the centre,
    // against the 32 dp of 108 dp the launcher mask allows. And the tile
    // silhouette: nothing may poke past the rounded store corner.
    for (take in MakeIconTakes.TAKES) {
        val fg = MakeIconTakes.foreground(take, 432)
        val cx = 216.0
        var reach = 0.0
        for (py in 0 until 432) {
            for (px in 0 until 432) {
                if ((fg.getRGB(px, py) ushr 24) > 16) {
                    reach = maxOf(reach, kotlin.math.hypot(px - cx, py - cx))
                }
            }
        }
        println(
            "${take.id}: fg reach ${"%.1f".format(reach)} px of limit ${432 * 32.0 / 108.0} " +
                "(${if (reach <= 432 * 32.0 / 108.0) "inside" else "OUTSIDE"} the mask)",
        )
        val tile = MakeIconTakes.tile(take, 512)
        var pokes = 0
        val corner = 512 * MakeIconTakes.TAKES[0].let { IconDesign.STORE_CORNER_FRACTION }
        fun outside(x: Double, y: Double): Double {
            val r = corner
            val qx = kotlin.math.abs(x - 256) - (256 - r)
            val qy = kotlin.math.abs(y - 256) - (256 - r)
            val ax = kotlin.math.max(qx, 0.0)
            val ay = kotlin.math.max(qy, 0.0)
            return kotlin.math.hypot(ax, ay) + kotlin.math.min(kotlin.math.max(qx, qy), 0.0) - r
        }
        for (py in 0 until 512 step 2) {
            for (px in 0 until 512 step 2) {
                if ((tile.getRGB(px, py) ushr 24) > 200 && outside(px.toDouble(), py.toDouble()) > 1.5) {
                    pokes += 1
                }
            }
        }
        println("${take.id}: tile silhouette pokes: $pokes")
    }

    // The banner takes: same mark, tuned metrics, for the feature round.
    val banners = mapOf(
        "fg-current" to arrayOf(340, 96, 80, 108f, 540f, 268f, 28f, 542f, 322f),
        "fg-big" to arrayOf(372, 88, 64, 112f, 516f, 266f, 30f, 518f, 324f),
        "fg-tall" to arrayOf(340, 96, 80, 116f, 520f, 264f, 30f, 522f, 326f),
    )
    for ((name, m) in banners) {
        val banner = MakeArt.featureOn(
            rootDir,
            IconDesign.LAGOON, IconDesign.PAPER, 0xD9FAF6EF.toInt(),
            markSize = m[0] as Int, markX = m[1] as Int, markY = m[2] as Int,
            nameSize = m[3] as Float, nameX = m[4] as Float, nameY = m[5] as Float,
            tagSize = m[6] as Float, tagX = m[7] as Float, tagY = m[8] as Float,
        )
        ImageIO.write(banner, "png", File(outDir, "$name.png"))
    }
    println("Wrote the icon takes to $outDir")
}
