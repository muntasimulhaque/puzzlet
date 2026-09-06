package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * Design: the toy itself. A 2x2 block of four chunky pieces (red, gold,
 * yellow, orange) with big round knobs locked into matching sockets and
 * dark grooves between them, the way a toddler's wooden tray puzzle
 * looks. Every joint is a circular head on tangent stems with concave
 * fillets to the edge: die true, never clip art. Rendered three ways:
 * the legacy tile for API 24-25 (paper tile, block on top), the adaptive
 * foreground for API 26+ (block on transparency over the paper
 * background), and a white monochrome sibling (block silhouette) for
 * Android 13+ themed icons.
 *
 * Colors here mirror app/src/main/res/values/colors.xml plus the toy
 * palette below. Change both together, then run makeIcons and commit the
 * regenerated PNGs.
 */
object IconDesign {
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val DEEP: Int = 0xFF085949.toInt()

    /** The four toy pieces, row by row from the top left. */
    const val RED: Int = 0xFFD9382B.toInt()
    const val GOLD: Int = 0xFFF0AD2E.toInt()
    const val YELLOW: Int = 0xFFF2CE1B.toInt()
    const val ORANGE: Int = 0xFFE05E1C.toInt()

    /** The grooves between the pieces. */
    const val SEAM: Int = 0xFF3A2418.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /** Macro bleed of the mark on the adaptive canvas. */
    const val FG_SPAN_DP = 90.0
    /** Fraction of a full-art tile the mark spans. */
    const val TILE_SPAN = 0.98
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
    /** Store tile corner radius as a fraction of the tile. */
    const val STORE_CORNER_FRACTION = 0.19

    /** The 2x2 block: piece side and groove width, in tile units. */
    const val PIECE = 0.44
    const val GAP = 0.024
    const val BLOCK_X = (1.0 - (PIECE * 2.0 + GAP)) / 2.0
    const val BLOCK_Y = (1.0 - (PIECE * 2.0 + GAP)) / 2.0
    const val PIECE_CORNER = 0.045

    /**
     * The knob, the way a die rule bends it: circular head of radius
     * HEAD_R on centre height HEAD_C, stem half-width NECK, fillet FIL.
     */
    const val KNOB_NECK = 0.034
    const val KNOB_HEAD_C = 0.066
    const val KNOB_HEAD_R = 0.066
    const val KNOB_FIL = 0.014

    /** Outer groove width, in tile units. Fills only: strokes drift across JDKs. */
    const val SEAM_W = 0.022

    /** Tab inset: tabs draw slightly small, so a groove ring shows around them. */
    const val TAB_SHRINK = 0.84
}

/** One toy piece: body rect, edge joints, fill color. */
private data class ToyPiece(
    val x: Double,
    val y: Double,
    val color: Int,
    /** Joints per edge in walk order: top, right, bottom, left. +1 tab, -1 socket, 0 flat. */
    val joints: List<Int>,
)

private fun toyPieces(): List<ToyPiece> {
    val d = IconDesign
    val x0 = d.BLOCK_X
    val y0 = d.BLOCK_Y
    val s = d.PIECE
    return listOf(
        ToyPiece(x0, y0, d.RED, listOf(0, -1, 1, 0)),
        ToyPiece(x0 + s + d.GAP, y0, d.GOLD, listOf(0, 0, -1, 1)),
        ToyPiece(x0, y0 + s + d.GAP, d.YELLOW, listOf(-1, 1, 0, 0)),
        ToyPiece(x0 + s + d.GAP, y0 + s + d.GAP, d.ORANGE, listOf(1, 0, 0, -1)),
    )
}

/**
 * One die joint. (ox, oy) is the edge start, (ux, uy) the edge
 * direction, (nx, ny) the outward normal; c is the joint centre in edge
 * distance from the start, sign +1 reaches outward, -1 carves inward.
 */
private fun traceKnob(
    path: Path2D.Double,
    ox: Double, oy: Double,
    ux: Double, uy: Double,
    nx: Double, ny: Double,
    c: Double, sign: Double,
    nw: Double, hc: Double, r: Double, fil: Double,
    shrink: Double,
) {
    // A tab may draw slightly small about its own middle, opening a
    // groove ring inside its socket; sockets always draw true.
    fun dot(t: Double, o: Double) {
        val st = c + (t - c) * shrink
        val so = o * shrink
        path.lineTo(ox + ux * st + nx * so * sign, oy + uy * st + ny * so * sign)
    }
    fun cub(t1: Double, o1: Double, t2: Double, o2: Double, t3: Double, o3: Double) {
        // Same shrink mapping as dot, as true curve controls.
        path.curveTo(
            ox + ux * (c + (t1 - c) * shrink) + nx * (o1 * shrink) * sign,
            oy + uy * (c + (t1 - c) * shrink) + ny * (o1 * shrink) * sign,
            ox + ux * (c + (t2 - c) * shrink) + nx * (o2 * shrink) * sign,
            oy + uy * (c + (t2 - c) * shrink) + ny * (o2 * shrink) * sign,
            ox + ux * (c + (t3 - c) * shrink) + nx * (o3 * shrink) * sign,
            oy + uy * (c + (t3 - c) * shrink) + ny * (o3 * shrink) * sign,
        )
    }
    // Tangent points from the stem feet to the head circle, solved exact:
    // |T - C| = r with (T - P) perpendicular to (T - C), P = (-nw, 0).
    val px = -nw
    val k = r * r - hc * hc
    val qa = hc * hc + px * px
    val qb = 2 * k * hc - 2 * px * px * hc
    val qc = k * k + px * px * hc * hc - r * r * px * px
    val disc = qb * qb - 4 * qa * qc
    require(disc >= 0) { "Stem misses the head circle" }
    val v = (-qb + kotlin.math.sqrt(disc)) / (2 * qa)
    val u = (k + hc * v) / px
    // Fillet onto the stem foot, stem up to the tangent, round the head.
    dot(c - nw - fil, 0.0)
    cub(
        c - nw, 0.0,
        c - nw, fil,
        c - nw, fil,
    )
    dot(c + u, v)
    var deg = Math.toDegrees(kotlin.math.atan2(v - hc, u))
    if (deg < 90.0) deg += 360.0
    val endDeg = Math.toDegrees(kotlin.math.atan2(v - hc, -u))
    var a = deg
    while (a > endDeg) {
        val rad = Math.toRadians(a)
        dot(c + r * kotlin.math.cos(rad), hc + r * kotlin.math.sin(rad))
        a -= 0.75
    }
    dot(c - u, v)
    dot(c + nw, fil)
    cub(
        c + nw, fil,
        c + nw, 0.0,
        c + nw + fil, 0.0,
    )
}

/** Outline of one toy piece, with round outer corners. */
private fun toyOutline(p: ToyPiece, trueShapes: Boolean = false): Path2D.Double {
    fun shrinkForJoint(joint: Int) = if (trueShapes || joint <= 0) 1.0 else IconDesign.TAB_SHRINK
    val d = IconDesign
    val r = d.PIECE_CORNER
    val x0 = p.x
    val y0 = p.y
    val x1 = p.x + d.PIECE
    val y1 = p.y + d.PIECE
    val nw = d.KNOB_NECK
    val hc = d.KNOB_HEAD_C
    val hr = d.KNOB_HEAD_R
    val fil = d.KNOB_FIL
    val path = Path2D.Double()
    // Top edge, left to right.
    path.moveTo(x0 + r, y0)
    edgeInto(path, p.joints[0], x0, y0, 1.0, 0.0, 0.0, -1.0, d.PIECE / 2.0, nw, hc, hr, fil, shrinkForJoint(p.joints[0]))
    path.lineTo(x1 - r, y0)
    path.quadTo(x1, y0, x1, y0 + r)
    // Right edge, top to bottom.
    edgeInto(path, p.joints[1], x1, y0, 0.0, 1.0, 1.0, 0.0, d.PIECE / 2.0, nw, hc, hr, fil, shrinkForJoint(p.joints[1]))
    path.lineTo(x1, y1 - r)
    path.quadTo(x1, y1, x1 - r, y1)
    // Bottom edge, right to left.
    edgeInto(path, p.joints[2], x1, y1, -1.0, 0.0, 0.0, 1.0, d.PIECE / 2.0, nw, hc, hr, fil, shrinkForJoint(p.joints[2]))
    path.lineTo(x0 + r, y1)
    path.quadTo(x0, y1, x0, y1 - r)
    // Left edge, bottom to top.
    edgeInto(path, p.joints[3], x0, y1, 0.0, -1.0, -1.0, 0.0, d.PIECE / 2.0, nw, hc, hr, fil, shrinkForJoint(p.joints[3]))
    path.lineTo(x0, y0 + r)
    path.quadTo(x0, y0, x0 + r, y0)
    path.closePath()
    return path
}

/** One edge: flat line, or the joint at its middle. c is edge distance of the middle. */
private fun edgeInto(
    path: Path2D.Double,
    joint: Int,
    ox: Double, oy: Double,
    ux: Double, uy: Double,
    nx: Double, ny: Double,
    c: Double,
    nw: Double, hc: Double, r: Double, fil: Double,
    shrink: Double,
) {
    if (joint == 0) {
        path.lineTo(ox + ux * (c + IconDesign.PIECE / 2.0), oy + uy * (c + IconDesign.PIECE / 2.0))
        return
    }
    traceKnob(path, ox, oy, ux, uy, nx, ny, c, joint.toDouble(), nw, hc, r, fil, shrink)
}

internal enum class Layer { TILE, FOREGROUND, MONO }

private fun beginIcon(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    return g
}

private fun unitTransform(size: Int, span: Double): AffineTransform {
    val t = AffineTransform.getTranslateInstance(size / 2.0 - 0.5 * span, size / 2.0 - 0.5 * span)
    t.concatenate(AffineTransform.getScaleInstance(span, span))
    return t
}

/** One icon layer: paper tile with the block, bare block, or white block. No strokes anywhere: flat fills pin identically on every JDK. */
internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val d = IconDesign
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = beginIcon(image)
    if (layer == Layer.TILE) {
        val corner = size * cornerFraction * 2.0
        g.color = Color(d.PAPER, true)
        g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), corner, corner))
    }
    val span = if (layer == Layer.FOREGROUND) size * d.FG_SPAN_DP / d.ADAPTIVE_DP else size * d.TILE_SPAN
    val t = unitTransform(size, span)
    val mono = layer == Layer.MONO
    val pieces = toyPieces()
    if (mono) {
        for (p in pieces) {
            g.color = Color(d.WHITE, true)
            g.fill(Area(toyOutline(p)).createTransformedArea(t))
        }
        g.dispose()
        return image
    }
    // The groove bed: the whole block silhouette in seam color, peeking
    // out around an expanded round rect for the outer groove.
    val bed = Area()
    for (p in pieces) bed.add(Area(toyOutline(p, trueShapes = true)))
    val bx = d.BLOCK_X - d.SEAM_W / 2.0
    val by = d.BLOCK_Y - d.SEAM_W / 2.0
    val bs = d.PIECE * 2.0 + d.GAP + d.SEAM_W
    val bc = (d.PIECE_CORNER + d.SEAM_W / 2.0) * 2.0
    g.color = Color(d.SEAM, true)
    g.fill(Area(RoundRectangle2D.Double(bx, by, bs, bs, bc, bc)).createTransformedArea(t))
    g.fill(bed.createTransformedArea(t))
    // The straight grooves: seam bars underlapping both edges.
    val midX = d.BLOCK_X + d.PIECE + d.GAP / 2.0
    val midY = d.BLOCK_Y + d.PIECE + d.GAP / 2.0
    val barX = midX - d.GAP / 2.0 - d.SEAM_W / 2.0
    val barY = midY - d.GAP / 2.0 - d.SEAM_W / 2.0
    val barW = d.GAP + d.SEAM_W
    val barL = d.PIECE * 2.0 + d.GAP + d.SEAM_W
    g.fill(Area(Rectangle2D.Double(barX, by, barW, barL)).createTransformedArea(t))
    g.fill(Area(Rectangle2D.Double(bx, barY, barL, barW)).createTransformedArea(t))
    // The pieces on top; tabs sit small inside their sockets, ringed dark.
    for (p in pieces) {
        g.color = Color(p.color, true)
        g.fill(Area(toyOutline(p)).createTransformedArea(t))
    }
    g.dispose()
    return image
}

/** The legacy tile: paper tile with the block, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: the block on transparency. */
fun adaptiveLayer(sizePx: Int, pieceArgb: Int): BufferedImage {
    // The monochrome sibling renders the block silhouette in white; the
    // paper argument stays so every caller keeps one shape of call.
    if (pieceArgb == IconDesign.WHITE) return paintLayer(sizePx, Layer.MONO, 0.0)
    return paintLayer(sizePx, Layer.FOREGROUND, 0.0)
}

/** The 512 store tile: full art with the store corner. */
fun storeTile(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.STORE_CORNER_FRACTION)

/** One icon file: where it lives under res, and the image that belongs there. */
data class IconFile(val relativePath: String, val image: () -> BufferedImage)

/** Every file makeIcons owns, with its size bound to its density. */
fun iconFiles(): List<IconFile> = buildList {
    for (i in IconDesign.DENSITY_DIRS.indices) {
        val dir = "mipmap-${IconDesign.DENSITY_DIRS[i]}"
        val scale = IconDesign.DENSITY_SCALES[i]
        add(IconFile("$dir/ic_launcher.png") { legacyIcon(Math.round(IconDesign.LEGACY_DP * scale).toInt()) })
        add(IconFile("$dir/ic_launcher_foreground.png") {
            adaptiveLayer(Math.round(IconDesign.ADAPTIVE_DP * scale).toInt(), IconDesign.PAPER)
        })
        add(IconFile("$dir/ic_launcher_monochrome.png") {
            adaptiveLayer(Math.round(IconDesign.ADAPTIVE_DP * scale).toInt(), IconDesign.WHITE)
        })
    }
}

private fun pngBytes(image: BufferedImage): ByteArray {
    val bytes = java.io.ByteArrayOutputStream()
    ImageIO.write(image, "png", bytes)
    return bytes.toByteArray()
}

/** Write the whole icon set into a res directory, overwriting in place. */
fun writeIcons(resDir: File) {
    for (file in iconFiles()) {
        val out = File(resDir, file.relativePath)
        out.parentFile.mkdirs()
        ImageIO.write(file.image(), "png", out)
    }
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val resDir = File(rootDir, "app/src/main/res")
    val check = args.size > 1 && args[1] == "--check"

    if (!check) {
        writeIcons(resDir)
        println("makeIcons: wrote ${iconFiles().size} files under ${resDir.path}")
        return
    }

    val drift = mutableListOf<String>()
    for (file in iconFiles()) {
        val committed = File(resDir, file.relativePath)
        if (!committed.exists()) {
            drift.add("${file.relativePath}: missing")
            continue
        }
        if (!pngBytes(file.image()).contentEquals(committed.readBytes())) {
            drift.add("${file.relativePath}: differs from regeneration")
        }
    }
    if (drift.isNotEmpty()) {
        println("checkIcons: committed icons drifted from the generator:")
        drift.forEach { println("  $it") }
        println("Run :tools:makeIcons, inspect, and commit the regenerated PNGs.")
        exitProcess(1)
    }
    println("checkIcons: all ${iconFiles().size} icon files match the generator.")
}
