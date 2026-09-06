package io.github.muntasimulhaque.puzzlet.tools

import io.github.muntasimulhaque.puzzlet.core.CircleSpec
import io.github.muntasimulhaque.puzzlet.core.EllipseSpec
import io.github.muntasimulhaque.puzzlet.core.PolygonSpec
import io.github.muntasimulhaque.puzzlet.core.RingSpec
import io.github.muntasimulhaque.puzzlet.core.RoundRectSpec
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import io.github.muntasimulhaque.puzzlet.core.Scenes
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
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
        drawScene(g, scene, 0.0, 0.0, (TILE * 2).toDouble())
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
        drawScene(g, scene, x.toDouble(), y.toDouble(), TILE.toDouble())
        g.clip = sheet
        drawCleanString(g, scene.id, "baloo2_bold.ttf", 26f, INK.rgb, (x + 4).toFloat(), (y + TILE + 32).toFloat(), rootDir)
    }
    g.dispose()
    ImageIO.write(image, "png", File(outDir, "scene-sheet.png"))
    println("Wrote ${scenes.size} pictures to $outDir")
}

/** The one renderer, in Java2D: the app's own scene data, drawn flat. */
private fun drawScene(g: Graphics2D, spec: SceneSpec, ox: Double, oy: Double, side: Double) {
    for (shape in spec.shapes) {
        g.color = Color((shape.argb and 0xFFFFFFFFL).toInt(), true)
        when (shape) {
            is CircleSpec -> {
                val d = shape.radius * 2.0 * side
                g.fill(
                    Ellipse2D.Double(
                        ox + (shape.center.x - shape.radius) * side,
                        oy + (shape.center.y - shape.radius) * side,
                        d, d,
                    ),
                )
            }
            is EllipseSpec -> rotated(g, ox + shape.center.x * side, oy + shape.center.y * side, shape.angleDeg) {
                val d = shape.rx * 2.0 * side
                val h = shape.ry * 2.0 * side
                fill(Ellipse2D.Double(-d / 2.0, -h / 2.0, d, h))
            }
            is RoundRectSpec -> {
                val cx = ox + (shape.x + shape.w / 2.0) * side
                val cy = oy + (shape.y + shape.h / 2.0) * side
                rotated(g, cx, cy, shape.angleDeg) {
                    val w = shape.w * side
                    val h = shape.h * side
                    val r = shape.cornerRadius * side * 2.0
                    fill(RoundRectangle2D.Double(-w / 2.0, -h / 2.0, w, h, r, r))
                }
            }
            is PolygonSpec -> {
                val path = Path2D.Double()
                shape.points.forEachIndexed { i, p ->
                    val x = ox + p.x * side
                    val y = oy + p.y * side
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.closePath()
                g.fill(path)
            }
            is RingSpec -> {
                val cx = ox + shape.center.x * side
                val cy = oy + shape.center.y * side
                val shrink = ((shape.rx - shape.thickness) / shape.rx).coerceIn(0.25, 0.9)
                val path = Path2D.Double(Path2D.WIND_EVEN_ODD)
                path.append(oval(cx, cy, shape.rx * side, shape.ry * side, shape.angleDeg), false)
                path.append(oval(cx, cy, shape.rx * side * shrink, shape.ry * side * shrink, shape.angleDeg), false)
                g.fill(path)
            }
        }
    }
}

private fun oval(cx: Double, cy: Double, rx: Double, ry: Double, angleDeg: Double): java.awt.Shape {
    val e = Ellipse2D.Double(cx - rx, cy - ry, rx * 2.0, ry * 2.0)
    if (angleDeg == 0.0) return e
    return java.awt.geom.AffineTransform
        .getRotateInstance(Math.toRadians(angleDeg), cx, cy)
        .createTransformedShape(e)
}

private inline fun rotated(g: Graphics2D, cx: Double, cy: Double, angleDeg: Double, draw: Graphics2D.() -> Unit) {
    if (angleDeg == 0.0) {
        val saved = g.transform
        g.translate(cx, cy)
        g.draw()
        g.transform = saved
    } else {
        val saved = g.transform
        g.translate(cx, cy)
        g.rotate(Math.toRadians(angleDeg))
        g.draw()
        g.transform = saved
    }
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
