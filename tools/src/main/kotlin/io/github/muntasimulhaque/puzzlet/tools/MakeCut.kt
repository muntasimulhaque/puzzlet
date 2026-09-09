package io.github.muntasimulhaque.puzzlet.tools

import io.github.muntasimulhaque.puzzlet.core.PieceCut
import io.github.muntasimulhaque.puzzlet.core.PieceShape
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.cutSeedFor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The cut sheet: the game's real die-cut pieces beside the mark, so the
 * owner can judge the shapes with their own eyes (the D-036 lesson: render
 * takes as images and point at one). The terminal view prints every cut as
 * ASCII, one letter per piece, so the shape can be read without a screen.
 *
 * Output goes to build/cut (never committed: working scratch for a review
 * round, and the repo keeps no candidate folders).
 */
private val PAPER = Color(0xFAF6EF)
private val INK = Color(0x1F2B28)
private val TRAY = Color(0xEBE0CC)

private val TINTS = listOf(
    Color(0x5FAED4), Color(0xF0B429), Color(0xE4572E), Color(0x6FB863),
    Color(0x0C7A64), Color(0x8FCDE5), Color(0xF2E8D4), Color(0x54A9CC),
    Color(0xB8E2F2), Color(0x4E8C46), Color(0xFEFCF8), Color(0xE0D2B4),
    Color(0x86CC72), Color(0x2B7899), Color(0xE3D6B8), Color(0xCEE3DB),
)

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/cut")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }
    val cuts = listOf(2 to 2, 3 to 3, 4 to 4)
    for ((rows, cols) in cuts) {
        val cut = PieceCut.generate(rows, cols, 1.0, 1.0, cutSeedFor("sail", rows, cols))
        println(asciiCut(cut, rows, cols))
    }
    val sheet = BufferedImage(1800, 700, BufferedImage.TYPE_INT_ARGB)
    val g = sheet.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.color = PAPER
    g.fillRect(0, 0, sheet.width, sheet.height)
    // The mark, on the left: the app's own measured real piece.
    g.color = TRAY
    g.fill(RoundRectangle2D.Double(40.0, 40.0, 560.0, 560.0, 48.0, 48.0))
    val mark = piecePath(320.0, 320.0, 440.0, listOf(1, 1, -1, -1))
    g.color = Color(0xFFFFFF, true)
    g.fill(mark)
    g.color = INK
    g.stroke = BasicStroke(3f)
    g.draw(mark)
    // The game cuts, each piece tinted and inked.
    var x = 660.0
    for ((rows, cols) in cuts) {
        val cut = PieceCut.generate(rows, cols, 1.0, 1.0, cutSeedFor("sail", rows, cols))
        g.color = TRAY
        g.fill(RoundRectangle2D.Double(x, 40.0, 340.0, 340.0, 36.0, 36.0))
        drawCut(g, cut, rows, cols, x + 20.0, 60.0, 300.0)
        x += 380.0
    }
    g.dispose()
    ImageIO.write(sheet, "png", File(outDir, "cut-sheet.png"))
    println("Wrote cut sheet to $outDir")
}

private fun drawCut(g: Graphics2D, cut: PieceCut.Cut, rows: Int, cols: Int, ox: Double, oy: Double, side: Double) {
    for (r in 0 until rows) for (c in 0 until cols) {
        val shape = cut.shapes[r * cols + c]
        val path = pathOf(shape, Vec2(ox + c * cut.cellW * side, oy + r * cut.cellH * side), side)
        val tint = TINTS[(r * cols + c) % TINTS.size]
        g.color = Color(tint.red, tint.green, tint.blue, 210)
        g.fill(path)
        g.color = INK
        g.stroke = BasicStroke(1.6f)
        g.draw(path)
    }
}

private fun pathOf(shape: PieceShape, origin: Vec2, side: Double): Path2D.Double {
    val path = Path2D.Double()
    var first = true
    for (seg in shape.segments) {
        fun x(v: Vec2) = origin.x + (v.x + shape.offsetInCell.x) * side
        fun y(v: Vec2) = origin.y + (v.y + shape.offsetInCell.y) * side
        if (first) {
            path.moveTo(x(seg.p0), y(seg.p0))
            first = false
        }
        path.curveTo(x(seg.c1), y(seg.c1), x(seg.c2), y(seg.c2), x(seg.p1), y(seg.p1))
    }
    path.closePath()
    return path
}

/** One letter per piece, a space for the table, so the cut reads in text. */
private fun asciiCut(cut: PieceCut.Cut, rows: Int, cols: Int, width: Int = 96, height: Int = 48): String {
    val polys = ArrayList<List<Vec2>>(rows * cols)
    for (r in 0 until rows) for (c in 0 until cols) polys.add(polyOf(cut, r, c, cols))
    val out = StringBuilder("--- cut ${rows}x$cols ---\n")
    for (gy in 0 until height) {
        for (gx in 0 until width) {
            val x = (gx + 0.5) / width
            val y = (gy + 0.5) / height
            var mark = ' '
            for ((index, poly) in polys.withIndex()) {
                if (inside(poly, x, y)) {
                    mark = if (index < 26) 'A' + index else 'a' + (index - 26)
                    break
                }
            }
            out.append(mark)
        }
        out.append('\n')
    }
    return out.toString()
}

/** One piece's outline as a fine polyline in board coordinates. */
private fun polyOf(cut: PieceCut.Cut, r: Int, c: Int, cols: Int): List<Vec2> {
    val shape = cut.shapes[r * cols + c]
    val origin = Vec2(c * cut.cellW + shape.offsetInCell.x, r * cut.cellH + shape.offsetInCell.y)
    val poly = ArrayList<Vec2>(shape.segments.size * 10)
    for (seg in shape.segments) {
        for (i in 0 until 10) {
            val t = i / 10.0
            val m = 1.0 - t
            val a = m * m * m
            val b = 3 * m * m * t
            val cc = 3 * m * t * t
            val d = t * t * t
            poly.add(
                Vec2(
                    origin.x + a * seg.p0.x + b * seg.c1.x + cc * seg.c2.x + d * seg.p1.x,
                    origin.y + a * seg.p0.y + b * seg.c1.y + cc * seg.c2.y + d * seg.p1.y,
                )
            )
        }
    }
    return poly
}

private fun inside(poly: List<Vec2>, x: Double, y: Double): Boolean {
    var hit = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) hit = !hit
        j = i
    }
    return hit
}
