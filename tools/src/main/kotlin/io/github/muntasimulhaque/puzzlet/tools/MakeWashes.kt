package io.github.muntasimulhaque.puzzlet.tools

import io.github.muntasimulhaque.puzzlet.core.Scenes
import io.github.muntasimulhaque.puzzlet.core.softWash
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The wash sheet: every picture beside the accent it declares, the soft wash
 * that accent becomes, and the coin's on state drawn at real size. The owner
 * judges colour with their own eyes (the D-036 lesson), so this renders the
 * whole set at once into build/washes (never committed: working scratch for
 * a review round).
 */
private const val CELL_W = 260
private const val CELL_H = 340
private const val SCENE = 220
private const val GAP = 24
private const val MARGIN = 40
private const val COLUMNS = 4
private const val HEADER = 96

private val PAPER = Color(0xFAF6EF)
private val INK = Color(0x1F2B28)
private val SOFT = Color(0x5C6B65)

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/washes")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }
    val scenes = Scenes.all
    val rows = (scenes.size + COLUMNS - 1) / COLUMNS
    val width = MARGIN * 2 + COLUMNS * CELL_W + (COLUMNS - 1) * GAP
    val height = HEADER + MARGIN + rows * CELL_H + (rows - 1) * GAP + MARGIN

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.paint = PAPER
    g.fillRect(0, 0, width, height)
    drawCleanString(g, "Puzzlet coin washes", "baloo2_extrabold.ttf", 40f, INK.rgb, 40f, 40f, rootDir)
    drawCleanString(
        g,
        "accent, the wash it becomes, and the coin's on state",
        "baloo2_bold.ttf",
        22f,
        SOFT.rgb,
        42f,
        84f,
        rootDir,
    )

    for ((index, scene) in scenes.withIndex()) {
        val col = index % COLUMNS
        val row = index / COLUMNS
        val x = MARGIN + col * (CELL_W + GAP)
        val y = HEADER + MARGIN + row * (CELL_H + GAP)

        val saved = g.clip
        g.clip(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), SCENE.toDouble(), SCENE.toDouble(), 36.0, 36.0))
        drawSceneJava2D(g, scene, x.toDouble(), y.toDouble(), SCENE.toDouble())
        g.clip = saved

        val accent = Color((scene.accent and 0xFFFFFFFFL).toInt(), true)
        val wash = Color((softWash(scene.accent) and 0xFFFFFFFFL).toInt(), true)
        val swatch = 44
        g.color = accent
        g.fillRoundRect(x, y + SCENE + 16, swatch, swatch, 12, 12)
        g.color = wash
        g.fillRoundRect(x + swatch + 10, y + SCENE + 16, swatch, swatch, 12, 12)

        // The coin's on state at real size: the wash disc, the picture in it,
        // and the hairline edge the app draws.
        val coin = 64
        val coinX = x + 2 * swatch + 20
        val coinY = y + SCENE + 16
        g.color = wash
        g.fill(Ellipse2D.Double(coinX.toDouble(), coinY.toDouble(), coin.toDouble(), coin.toDouble()))
        val thumb = 40
        val thumbX = coinX + (coin - thumb) / 2
        val thumbY = coinY + (coin - thumb) / 2
        val inner = g.clip
        g.clip(Ellipse2D.Double(thumbX.toDouble(), thumbY.toDouble(), thumb.toDouble(), thumb.toDouble()))
        drawSceneJava2D(g, scene, thumbX.toDouble(), thumbY.toDouble(), thumb.toDouble())
        g.clip = inner
        g.color = Color(INK.red, INK.green, INK.blue, 26)
        g.draw(Ellipse2D.Double(coinX.toDouble(), coinY.toDouble(), coin.toDouble(), coin.toDouble()))

        drawCleanString(
            g,
            scene.id,
            "baloo2_bold.ttf",
            26f,
            INK.rgb,
            (x + 2).toFloat(),
            (y + SCENE + 100).toFloat(),
            rootDir,
        )
    }
    g.dispose()
    ImageIO.write(image, "png", File(outDir, "wash-sheet.png"))
    for (scene in scenes) {
        println(
            "%-11s accent #%06X  wash #%06X".format(
                scene.id,
                scene.accent and 0xFFFFFF,
                softWash(scene.accent) and 0xFFFFFF,
            ),
        )
    }
    println("Wrote ${scenes.size} washes to $outDir")
}
