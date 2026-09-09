package io.github.muntasimulhaque.puzzlet.tools

import io.github.muntasimulhaque.puzzlet.core.Scenes
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


/**
 * The picture sheet: every scene on the shelf, drawn by the same data the
 * game plays, so the owner can point at the ones to keep instead of reading
 * a description of them (the D-045 lesson, applied to pictures).
 *
 * Plain JVM, Java2D, no third-party libraries, same as every other
 * generator here. Output goes to build/scenes (never committed: it is
 * working scratch for a review round, and the repo keeps no candidate
 * folders): one contact sheet and one card per picture.
 */
private const val TILE = 300
private const val GAP = 26
private const val MARGIN = 44
private const val LABEL = 46
private const val COLUMNS = 4

private val PAPER = Color(0xFAF6EF)
private val INK = Color(0x1F2B28)

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/scenes")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }
    val scenes = Scenes.all
    val ascii = args.contains("--ascii")

    for (scene in scenes) {
        val card = BufferedImage(TILE * 2, TILE * 2, BufferedImage.TYPE_INT_ARGB)
        val g = card.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.paint = PAPER
        g.fillRect(0, 0, card.width, card.height)
        g.clipRect(0, 0, card.width, card.height)
        drawSceneJava2D(g, scene, 0.0, 0.0, (TILE * 2).toDouble())
        g.dispose()
        ImageIO.write(card, "png", File(outDir, "${scene.id}.png"))
        if (ascii) println(asciiOf(card, scene.id))
    }

    val rows = (scenes.size + COLUMNS - 1) / COLUMNS
    val width = MARGIN * 2 + COLUMNS * TILE + (COLUMNS - 1) * GAP
    val height = MARGIN * 2 + rows * (TILE + LABEL) + (rows - 1) * GAP
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.paint = PAPER
    g.fillRect(0, 0, width, height)
    val sheet = g.clip
    for ((index, scene) in scenes.withIndex()) {
        val col = index % COLUMNS
        val row = index / COLUMNS
        val x = MARGIN + col * (TILE + GAP)
        val y = MARGIN + row * (TILE + LABEL + GAP)
        // One tile at a time: reset to the whole sheet, then clip the tile.
        g.clip = sheet
        g.clip(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), TILE.toDouble(), TILE.toDouble(), 40.0, 40.0))
        drawSceneJava2D(g, scene, x.toDouble(), y.toDouble(), TILE.toDouble())
        g.clip = sheet
        drawCleanString(g, scene.id, "baloo2_bold.ttf", 26f, INK.rgb, (x + 4).toFloat(), (y + TILE + 32).toFloat(), rootDir)
    }
    g.dispose()
    ImageIO.write(image, "png", File(outDir, "scene-sheet.png"))
    println("Wrote ${scenes.size} pictures to $outDir")
}

/** A crude ASCII read of a picture, for a quick human glance in a terminal. */
private fun asciiOf(image: BufferedImage, id: String): String {
    val chars = " .:-=+*#%@"
    val cells = 34
    val step = image.width / cells
    val out = StringBuilder("$id\n")
    for (row in 0 until cells) {
        val line = StringBuilder()
        for (col in 0 until cells) {
            var r = 0
            var g = 0
            var b = 0
            var n = 0
            for (y in row * step until (row + 1) * step step 2) {
                for (x in col * step until (col + 1) * step step 2) {
                    val argb = image.getRGB(x, y)
                    r += (argb shr 16) and 0xFF
                    g += (argb shr 8) and 0xFF
                    b += argb and 0xFF
                    n++
                }
            }
            val lum = (0.299 * r / n + 0.587 * g / n + 0.114 * b / n) / 255.0
            val idx = ((1.0 - lum) * (chars.length - 1)).toInt().coerceIn(0, chars.length - 1)
            line.append(chars[idx])
        }
        out.append(line).append('\n')
    }
    return out.toString()
}
