package io.github.muntasimulhaque.puzzlet.tools

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * The gather: the mark the app ships, as flat fills of plain paths.
 *
 * Three wanderers (sky, coral, grass) closing on the honey home piece with
 * its sockets open: the moment before the click. One painter owns every
 * pixel of it, and the launcher set, the store tile and the feature
 * graphic all come through here, so the mark is always the same mark.
 *
 * Flat fills of plain paths only, no strokes, no boolean ops, no blur:
 * those drift across JDKs and break the byte pins (AGENTS.md, Lessons).
 * Material depth is faked with flat tones.
 */
object V4 {
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val INK: Int = 0xFF1F2B28.toInt()
    const val LAGOON: Int = 0xFF0C7A64.toInt()
    const val DEEP: Int = 0xFF085949.toInt()
    const val GROUND: Int = 0xFF241E16.toInt()
    const val HONEY: Int = 0xFFF0B429.toInt()
    const val HONEY_LIGHT: Int = 0xFFF6C84F.toInt()
    const val CORAL: Int = 0xFFE4572E.toInt()
    const val SKY: Int = 0xFF6BB7D6.toInt()
}

private const val TILE_CORNER = 0.19

// ---------------------------------------------------------------- geometry

/**
 * Trace one joint into a path. (ox, oy) is the edge start, (ux, uy) its
 * direction, (nx, ny) its outward normal. sign +1 grows a knob, -1 carves a
 * socket. rs stem half-width, hc head centre height, hr head radius, all in
 * edge units (edge length 1).
 */
internal fun traceJoint(
    path: Path2D.Double,
    ox: Double, oy: Double,
    ux: Double, uy: Double,
    nx: Double, ny: Double,
    sign: Int,
    rs: Double, hc: Double, hr: Double,
) {
    val c = 0.5
    fun pt(t: Double, o: Double) = Pair(ox + ux * t + nx * o * sign, oy + uy * t + ny * o * sign)
    fun line(t: Double, o: Double) {
        val p = pt(t, o); path.lineTo(p.first, p.second)
    }
    fun quad(t1: Double, o1: Double, t2: Double, o2: Double) {
        val p1 = pt(t1, o1); val p2 = pt(t2, o2)
        path.quadTo(p1.first, p1.second, p2.first, p2.second)
    }
    // Shoulder flares out to the head circle, then a round head on a stem.
    // Control points stay outside the final silhouette: no stray slivers.
    val shoulder = hc + hr * 0.72
    line(c - rs * 0.9, 0.0)
    quad(c - rs * 0.9 - hr * 0.52, shoulder * 0.55, c - hr * 0.66, shoulder)
    val sinA = ((shoulder - hc) / hr).coerceIn(-1.0, 1.0)
    val startDeg = 180.0 - Math.toDegrees(kotlin.math.asin(sinA))
    val endDeg = Math.toDegrees(kotlin.math.asin(sinA))
    var deg = startDeg
    while (deg > endDeg) {
        val rad = Math.toRadians(deg)
        val p = pt(c + hr * kotlin.math.cos(rad), hc + hr * kotlin.math.sin(rad))
        path.lineTo(p.first, p.second)
        deg -= 4.0
    }
    quad(c + rs * 0.9 + hr * 0.52, shoulder * 0.55, c + rs * 0.9, 0.0)
    line(c + rs, 0.0)
}

/**
 * A piece outline with top-left corner (x, y), side s, corner radius cr.
 * Joints per edge in walk order top, right, bottom, left (+1 knob, -1 socket,
 * 0 flat).
 */
internal fun pieceOutline(x: Double, y: Double, s: Double, cr: Double, joints: List<Int>): Path2D.Double {
    val path = Path2D.Double()
    val rs = IconDesign.KNOB_STEM
    val hc = IconDesign.KNOB_HEAD_C
    val hr = IconDesign.KNOB_HEAD_R
    val x1 = x + s
    val y1 = y + s
    fun edge(j: Int, ox: Double, oy: Double, ux: Double, uy: Double, nx: Double, ny: Double) {
        if (j == 0) path.lineTo(ox + ux * s, oy + uy * s)
        else traceJoint(path, ox, oy, ux * s, uy * s, nx * s, ny * s, j, rs, hc, hr)
    }
    path.moveTo(x + cr, y)
    edge(joints[0], x, y, 1.0, 0.0, 0.0, -1.0)
    path.lineTo(x1 - cr, y)
    path.quadTo(x1, y, x1, y + cr)
    edge(joints[1], x1, y, 0.0, 1.0, 1.0, 0.0)
    path.lineTo(x1, y1 - cr)
    path.quadTo(x1, y1, x1 - cr, y1)
    edge(joints[2], x1, y1, -1.0, 0.0, 0.0, 1.0)
    path.lineTo(x + cr, y1)
    path.quadTo(x, y1, x, y1 - cr)
    edge(joints[3], x, y1, 0.0, -1.0, -1.0, 0.0)
    path.lineTo(x, y + cr)
    path.quadTo(x, y, x + cr, y)
    path.closePath()
    return path
}

internal fun rotated(shape: java.awt.Shape, cx: Double, cy: Double, angleDeg: Double): Path2D.Double =
    Path2D.Double(shape, AffineTransform.getRotateInstance(Math.toRadians(angleDeg), cx, cy))

// ---------------------------------------------------------------- paint

internal fun beginV4(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    return g
}

internal fun fillGround(g: Graphics2D, w: Int, h: Int, argb: Int) {
    g.color = Color(argb, true)
    g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
}

internal fun roundedTile(g: Graphics2D, size: Int, cornerFraction: Double, argb: Int) {
    val corner = size * cornerFraction * 2.0
    g.color = Color(argb, true)
    g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), corner, corner))
}

internal fun darken(argb: Int, k: Double): Int {
    val a = (argb ushr 24) and 0xFF
    val r = (((argb shr 16) and 0xFF) * k).toInt()
    val gc = (((argb shr 8) and 0xFF) * k).toInt()
    val b = ((argb and 0xFF) * k).toInt()
    return (a shl 24) or (r shl 16) or (gc shl 8) or b
}

private fun lighten(argb: Int, add: Int): Int {
    val a = (argb ushr 24) and 0xFF
    val r = Math.min(255, ((argb shr 16) and 0xFF) + add)
    val gc = Math.min(255, ((argb shr 8) and 0xFF) + add)
    val b = Math.min(255, (argb and 0xFF) + add)
    return (a shl 24) or (r shl 16) or (gc shl 8) or b
}

/** Fill a piece with a flat drop tone: a darker ground shade shifted a hair, no blur. */
internal fun fillPiece(
    g: Graphics2D,
    shape: java.awt.Shape,
    argb: Int,
    groundArgb: Int,
    sizePx: Double,
    drop: Boolean,
) {
    if (drop) {
        val moved = Path2D.Double(shape, AffineTransform.getTranslateInstance(0.0, sizePx * 0.016))
        g.color = Color(darken(groundArgb, 0.55), true)
        g.fill(moved)
    }
    g.color = Color(argb, true)
    g.fill(shape)
}

/** Flat lit cap: clip to the silhouette, paint a light band on top, restore clip. */
private fun litCap(g: Graphics2D, shape: java.awt.Shape, argb: Int, bandFrac: Double) {
    val b = shape.bounds2D
    val oldClip = g.clip
    g.clip(shape)
    g.color = Color(argb, true)
    g.fill(Rectangle2D.Double(b.minX, b.minY, b.width, b.height * bandFrac))
    g.clip = oldClip
}

private fun paintBody(g: Graphics2D, size: Int, tile: Boolean, groundArgb: Int) {
    if (tile) roundedTile(g, size, TILE_CORNER, groundArgb)
    else if (groundArgb != 0) fillGround(g, size, size, groundArgb)
}

// ---------------------------------------------------------------- take A

private object Unibody {
    fun paint(size: Int, tile: Boolean, groundArgb: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = beginV4(image)
        paintBody(g, size, tile, groundArgb)
        val span = size * 0.60
        val t = AffineTransform.getTranslateInstance(size * 0.5 - span * 0.5, size * 0.53 - span * 0.5)
        t.concatenate(AffineTransform.getScaleInstance(span, span))
        val piece = Path2D.Double(pieceOutline(0.0, 0.0, 1.0, 0.14, listOf(1, -1, 0, 0)), t)
        fillPiece(g, piece, V4.LAGOON, if (groundArgb != 0) groundArgb else V4.GROUND, size.toDouble(), drop = true)
        litCap(g, piece, lighten(V4.LAGOON, 24), 0.16)
        g.dispose()
        return image
    }
}

// ---------------------------------------------------------------- take B

private object Onepiece {
    fun paint(size: Int, angle: Double, pieceArgb: Int, tile: Boolean, groundArgb: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = beginV4(image)
        paintBody(g, size, tile, groundArgb)
        val s = size * 0.56
        val raw = pieceOutline(0.0, 0.0, s, s * 0.25, listOf(1, -1, 0, 0))
        val b = raw.bounds2D
        val cx = size / 2.0
        val cy = size / 2.0 + size * 0.01
        val t = AffineTransform.getTranslateInstance(cx - b.centerX, cy - b.centerY)
        val placed = rotated(Path2D.Double(raw, t), cx.toDouble(), cy.toDouble(), angle)
        fillPiece(g, placed, pieceArgb, if (groundArgb != 0) groundArgb else V4.PAPER, size.toDouble(), drop = true)
        litCap(g, placed, lighten(pieceArgb, 28), 0.16)
        g.dispose()
        return image
    }
}

// ---------------------------------------------------------------- take C

/**
 * The four gather colours in walk order: sky corner, coral top, grass
 * left, honey home. One list, so the palette can be read at a glance and
 * a candidate sheet can try another without touching the painter.
 */
internal fun gatherPalette(mono: Boolean = false): List<Int> =
    if (mono) listOf(IconDesign.WHITE, IconDesign.WHITE, IconDesign.WHITE, IconDesign.WHITE)
    else listOf(IconDesign.SKY, IconDesign.CORAL, IconDesign.GRASS, IconDesign.HONEY)

/**
 * The gather, shared with the shipped launcher: three wanderers closing
 * on the honey home piece. Defaults reproduce the approved candidate
 * pixels exactly; the launcher passes smaller insets so nothing clips.
 */
internal object Gather {
    fun paint(
        size: Int,
        tile: Boolean,
        groundArgb: Int,
        gapFrac: Double = IconDesign.GAP_FRAC,
        mono: Boolean = false,
        insetFrac: Double = 0.0,
        cornerFraction: Double = 0.19,
        palette: List<Int> = gatherPalette(mono),
    ): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = beginV4(image)
        if (tile) roundedTile(g, size, cornerFraction, groundArgb)
        else if (groundArgb != 0) fillGround(g, size, size, groundArgb)
        val o = size * insetFrac
        val se = size * (1.0 - 2.0 * insetFrac)
        val s = se * 0.40
        val gap = s * gapFrac
        val bg = if (groundArgb != 0) groundArgb else IconDesign.DEEP
        // The 2x2 field centred (gap runs through the middle): home bottom-right.
        val hx = o + se / 2.0 + gap / 2.0
        val hy = o + se / 2.0 + gap / 2.0
        val (sky, coral, grass, honey) = palette
        val home = pieceOutline(hx, hy, s, s * 0.12, listOf(-1, 0, 0, -1))
        val top = pieceOutline(hx, hy - s - gap, s, s * 0.12, listOf(0, 0, 1, 0))
        val left = pieceOutline(hx - s - gap, hy, s, s * 0.12, listOf(0, 1, 0, 0))
        val corner = pieceOutline(hx - s - gap, hy - s - gap, s, s * 0.12, listOf(0, 1, 1, 0))
        fillPiece(g, rotated(corner, hx - s / 2 - gap, hy - s / 2 - gap, -7.0), sky, bg, se, drop = false)
        fillPiece(g, rotated(top, hx + s / 2, hy - s / 2 - gap, 6.0), coral, bg, se, drop = false)
        fillPiece(g, rotated(left, hx - s / 2 - gap, hy + s / 2, -5.0), grass, bg, se, drop = false)
        fillPiece(g, home, honey, bg, se, drop = false)
        g.dispose()
        return image
    }
}
