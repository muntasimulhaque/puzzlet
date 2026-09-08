package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * The mark: one real die-cut piece carrying the app's first picture, the
 * sailboat.
 *
 * The outline is measured from real puzzle pieces (a photograph of die-cut
 * pieces and the classic contour): rounded corners, gently bowed edges, a
 * short concave shoulder, a short neck, a round chunky head about twice the
 * neck wide, and the same shape reversed for the blank. Two tabs (top and
 * right) and two blanks (bottom and left) keep the sailboat whole: the
 * blanks bite sky and water, never the boat.
 *
 * Flat fills of plain paths only, no strokes, no Area booleans, no blur:
 * those drift across JDKs and break the byte pins (AGENTS.md, Lessons).
 */
object PieceDesign {
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    /** The monochrome silhouette's white (Android tints it at runtime). */
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val INK: Int = 0xFF1F2B28.toInt()
    const val HONEY: Int = 0xFFF0B429.toInt()
    const val CORAL: Int = 0xFFE4572E.toInt()
    const val SAIL: Int = 0xFFFEFCF8.toInt()
    const val SKY_TOP: Int = 0xFF5FAED4.toInt()
    const val SKY_LOW: Int = 0xFFB8E2F2.toInt()
    const val SEA_TOP: Int = 0xFF6FB7D7.toInt()
    const val SEA_DEEP: Int = 0xFF2B7899.toInt()
    const val SEA_MID: Int = 0xFF54A9CC.toInt()
    const val SEA_LIGHT: Int = 0xFF8FCDE5.toInt()

    /** The piece body as a share of the icon canvas. */
    const val BODY_FRAC = 0.44
    /** Outer corner radius, as a share of the body. */
    const val CORNER_FRAC = 0.08
    /** Inward bow of an edge, as a share of the edge run. */
    const val BOW_FRAC = 0.030
    /** Paper rim width, as a share of the body. */
    const val RIM_FRAC = 0.055
    /** The gentle tilt of a piece in hand. */
    const val TILT_DEG = -8.0
    /** Top tab, right tab, bottom blank, left blank. */
    val JOINTS = listOf(1, 1, -1, -1)
}

// ---------------------------------------------------------------- geometry

/**
 * One joint in the profile a real die-cut piece shows: a short concave
 * shoulder, a short neck, a round chunky head about twice the neck wide.
 * sign +1 tab, -1 blank; [o] positive outward; fractions of the edge run.
 */
private fun traceJoint(
    path: Path2D.Double,
    ox: Double, oy: Double, ux: Double, uy: Double, nx: Double, ny: Double,
    len: Double, sign: Double,
) {
    fun pt(t: Double, o: Double): Pair<Double, Double> =
        (ox + ux * t * len + nx * o * len * sign) to (oy + uy * t * len + ny * o * len * sign)
    fun curve(t1: Double, o1: Double, t2: Double, o2: Double, t3: Double, o3: Double) {
        val p1 = pt(t1, o1)
        val p2 = pt(t2, o2)
        val p3 = pt(t3, o3)
        path.curveTo(p1.first, p1.second, p2.first, p2.second, p3.first, p3.second)
    }
    curve(0.380, 0.006, 0.428, 0.036, 0.438, 0.088)
    curve(0.418, 0.118, 0.386, 0.126, 0.374, 0.158)
    curve(0.398, 0.226, 0.448, 0.238, 0.500, 0.238)
    curve(0.552, 0.238, 0.602, 0.226, 0.626, 0.158)
    curve(0.614, 0.126, 0.582, 0.118, 0.562, 0.088)
    curve(0.572, 0.036, 0.620, 0.006, 0.668, 0.000)
}

/**
 * A piece centred at (cx, cy): square body, rounded corners, bowed edges,
 * one joint per edge in walk order top, right, bottom, left (+1 tab, -1
 * blank, 0 flat).
 */
internal fun piecePath(cx: Double, cy: Double, body: Double, joints: List<Int>): Path2D.Double {
    val s = body
    val x = cx - s / 2.0
    val y = cy - s / 2.0
    val cr = s * PieceDesign.CORNER_FRAC
    val run = s - 2.0 * cr
    val path = Path2D.Double()
    fun edge(j: Int, ox: Double, oy: Double, ux: Double, uy: Double, nx: Double, ny: Double) {
        if (j == 0) {
            val mx = ox + ux * run * 0.5 - nx * run * PieceDesign.BOW_FRAC * 2.0
            val my = oy + uy * run * 0.5 - ny * run * PieceDesign.BOW_FRAC * 2.0
            path.quadTo(mx, my, ox + ux * run, oy + uy * run)
        } else {
            val bow = PieceDesign.BOW_FRAC * 0.35
            path.lineTo(
                ox + ux * run * 0.332 - nx * run * bow,
                oy + uy * run * 0.332 - ny * run * bow,
            )
            traceJoint(path, ox, oy, ux, uy, nx, ny, run, j.toDouble())
        }
    }
    path.moveTo(x + cr, y)
    edge(joints[0], x + cr, y, 1.0, 0.0, 0.0, -1.0)
    path.lineTo(x + s - cr, y)
    path.quadTo(x + s, y, x + s, y + cr)
    edge(joints[1], x + s, y + cr, 0.0, 1.0, 1.0, 0.0)
    path.lineTo(x + s, y + s - cr)
    path.quadTo(x + s, y + s, x + s - cr, y + s)
    edge(joints[2], x + s - cr, y + s, -1.0, 0.0, 0.0, 1.0)
    path.lineTo(x + cr, y + s)
    path.quadTo(x, y + s, x, y + s - cr)
    edge(joints[3], x, y + s - cr, 0.0, -1.0, -1.0, 0.0)
    path.lineTo(x, y + cr)
    path.quadTo(x, y, x + cr, y)
    path.closePath()
    return path
}

// ---------------------------------------------------------------- picture

private fun fill(g: Graphics2D, shape: Shape, argb: Int, a: Double = 1.0) {
    g.color = Color(argb, true).let { Color(it.red, it.green, it.blue, (a * 255.0).toInt()) }
    g.fill(shape)
}

private fun circle(g: Graphics2D, cx: Double, cy: Double, r: Double, argb: Int, a: Double = 1.0) {
    fill(g, Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0), argb, a)
}

private fun poly(g: Graphics2D, argb: Int, vararg pts: Double) {
    val path = Path2D.Double()
    var i = 0
    while (i + 1 < pts.size) {
        if (i == 0) path.moveTo(pts[i], pts[i + 1]) else path.lineTo(pts[i], pts[i + 1])
        i += 2
    }
    path.closePath()
    fill(g, path, argb)
}

private fun bands(
    g: Graphics2D, x: Double, y: Double, w: Double, h: Double, top: Int, bottom: Int, steps: Int,
) {
    val band = h / steps
    for (i in 0 until steps) {
        val t = i.toDouble() / (steps - 1)
        fun ch(shift: Int): Int {
            val a = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return (a + (b - a) * t).toInt().coerceIn(0, 255)
        }
        val argb = (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        fill(g, Rectangle2D.Double(x, y + i * band, w, band + 0.6), argb)
    }
}

private fun cloud(g: Graphics2D, cx: Double, cy: Double, scale: Double, argb: Int) {
    fill(g, RoundRectangle2D.Double(cx - 0.10 * scale, cy - 0.012 * scale, 0.20 * scale, 0.052 * scale, 0.052 * scale, 0.052 * scale), argb)
    circle(g, cx - 0.05 * scale, cy - 0.022 * scale, 0.045 * scale, argb)
    circle(g, cx + 0.03 * scale, cy - 0.030 * scale, 0.055 * scale, argb)
    circle(g, cx + 0.083 * scale, cy - 0.012 * scale, 0.036 * scale, argb)
}

/** The sailboat, composed so the blanks bite sky and water, never the boat. */
private fun paintSail(g: Graphics2D, s: Double) {
    bands(g, 0.0, 0.0, s, s * 0.56, PieceDesign.SKY_TOP, PieceDesign.SKY_LOW, 12)
    bands(g, 0.0, s * 0.56, s, s * 0.44, PieceDesign.SEA_TOP, PieceDesign.SEA_DEEP, 10)
    circle(g, s * 0.60, s * 0.32, s * 0.075, PieceDesign.HONEY, 0.18)
    circle(g, s * 0.60, s * 0.32, s * 0.045, PieceDesign.HONEY)
    cloud(g, s * 0.37, s * 0.31, s * 0.30, PieceDesign.SAIL)
    for (i in 0..15) circle(g, s * (0.015 + i * 0.066), s * 0.56, s * 0.028, PieceDesign.SEA_MID)
    for (i in 0..12) circle(g, s * (0.03 + i * 0.079), s * 0.72, s * 0.024, PieceDesign.SEA_LIGHT)
    for (i in 0..10) circle(g, s * (0.02 + i * 0.093), s * 0.88, s * 0.020, PieceDesign.SEA_MID)
    circle(g, s * 0.24, s * 0.66, s * 0.009, PieceDesign.SAIL)
    circle(g, s * 0.70, s * 0.68, s * 0.009, PieceDesign.SAIL)
    poly(g, PieceDesign.CORAL, s * 0.42, s * 0.50, s * 0.62, s * 0.50, s * 0.57, s * 0.585, s * 0.47, s * 0.585)
    fill(g, Rectangle2D.Double(s * 0.505, s * 0.345, s * 0.016, s * 0.155), PieceDesign.INK)
    poly(g, PieceDesign.SAIL, s * 0.52, s * 0.365, s * 0.52, s * 0.495, s * 0.635, s * 0.495)
    poly(g, PieceDesign.HONEY, s * 0.495, s * 0.395, s * 0.495, s * 0.495, s * 0.395, s * 0.495)
    poly(g, PieceDesign.CORAL, s * 0.51, s * 0.325, s * 0.51, s * 0.36, s * 0.46, s * 0.345)
}

// ---------------------------------------------------------------- painting

private fun begin(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    return g
}

/** A soft drop shadow: the piece path stamped a few times, fading out. */
private fun shadow(g: Graphics2D, shape: Shape, s: Double) {
    for (i in 5 downTo 1) {
        val k = i / 5.0
        val moved = AffineTransform.getTranslateInstance(s * 0.016 * k, s * 0.026 * k).createTransformedShape(shape)
        fill(g, moved, PieceDesign.INK, 0.24 / 5.0 * 1.6)
    }
}

/** Paint the mark, centred and sized by the canvas. */
internal fun paintMark(g: Graphics2D, size: Int) {
    val s = size.toDouble()
    val body = s * PieceDesign.BODY_FRAC
    val cx = s / 2.0
    val cy = s / 2.0
    val base = piecePath(cx, cy, body, PieceDesign.JOINTS)
    val t = AffineTransform.getRotateInstance(Math.toRadians(PieceDesign.TILT_DEG), cx, cy)
    val shape = t.createTransformedShape(base)
    shadow(g, shape, s)
    // The die-cut bed: paper under the artwork, then the artwork clipped to
    // a slightly smaller piece, so the rim is a fill, not a stroke.
    fill(g, shape, PieceDesign.PAPER)
    val inner = piecePath(cx, cy, body * (1.0 - PieceDesign.RIM_FRAC), PieceDesign.JOINTS)
    val content = g.create() as Graphics2D
    content.transform(t)
    content.clip(inner)
    paintSail(content, s)
    content.dispose()
}

/** The full icon tile: a ground with the mark on it. */
internal fun markTile(size: Int, groundArgb: Int, cornerFraction: Double): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    val corner = size * cornerFraction * 2.0
    g.color = Color(groundArgb, true)
    g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), corner, corner))
    paintMark(g, size)
    g.dispose()
    return image
}

/** The adaptive foreground: the mark alone, on transparency. */
internal fun markForeground(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    paintMark(g, size)
    g.dispose()
    return image
}

/** The monochrome sibling: the piece silhouette in white. */
internal fun markMonochrome(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    val s = size.toDouble()
    val base = piecePath(s / 2.0, s / 2.0, s * PieceDesign.BODY_FRAC, PieceDesign.JOINTS)
    val t = AffineTransform.getRotateInstance(Math.toRadians(PieceDesign.TILT_DEG), s / 2.0, s / 2.0)
    fill(g, t.createTransformedShape(base), PieceDesign.WHITE)
    g.dispose()
    return image
}
