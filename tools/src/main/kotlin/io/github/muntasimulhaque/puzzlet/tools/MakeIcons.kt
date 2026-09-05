package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * Design (the owner's, D-030): ONE jigsaw piece reaching out in all four
 * directions, top, right, bottom, left: four identical arms offering
 * friendship. Perfect symmetry keeps the mark calm, kills any double
 * meaning outright, and reads as a puzzle at any size. Rendered three ways:
 * the legacy tile for API 24-25, the adaptive foreground for API 26+, and a
 * white monochrome sibling for Android 13+ themed icons. The app's brand
 * mark reuses the committed foreground PNG, one source of truth.
 *
 * Colors here mirror app/src/main/res/values/colors.xml. Change both
 * together, then run makeIcons and commit the regenerated PNGs.
 */
object IconDesign {
    const val TEAL: Int = 0xFF0C7A64.toInt()
    const val TEAL_DEEP: Int = 0xFF085949.toInt()
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val HONEY: Int = 0xFFF0B429.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /** Piece span inside the adaptive canvas; far inside the 66 dp circle. */
    const val ADAPTIVE_SPAN_DP = 44.0
    /** Piece span as a fraction of the legacy tile. */
    const val LEGACY_SPAN_FRACTION = 0.66
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
}

/**
 * The friendship square (the owner's design): a 2 x 2 of four jigsaw
 * pieces, joined. Each piece is a real piece, oriented in a pinwheel: its
 * head reaches outward (top, right, bottom, left: the four arms) and its
 * cut opens on the inner seam, so four notches sit in the middle of the
 * square, waiting for the next pieces to snap in. Thin seams show that the
 * square is four pieces; the outer silhouette stays the calm square with
 * four arms. The piece spans 0.962 of the unit box.
 */
enum class Side { TOP, RIGHT, BOTTOM, LEFT }

internal const val BRAND_SPAN = 0.962

private fun addKnob(body: Area, mx: Double, my: Double, side: Side) {
    when (side) {
        Side.TOP -> {
            body.add(Area(Rectangle2D.Double(mx - 0.02, my - 0.055, 0.04, 0.065)))
            body.add(Area(Ellipse2D.Double(mx - 0.046, my - 0.085, 0.092, 0.092)))
        }
        Side.BOTTOM -> {
            body.add(Area(Rectangle2D.Double(mx - 0.02, my - 0.01, 0.04, 0.065)))
            body.add(Area(Ellipse2D.Double(mx - 0.046, my - 0.007, 0.092, 0.092)))
        }
        Side.LEFT -> {
            body.add(Area(Rectangle2D.Double(mx - 0.055, my - 0.02, 0.065, 0.04)))
            body.add(Area(Ellipse2D.Double(mx - 0.085, my - 0.046, 0.092, 0.092)))
        }
        Side.RIGHT -> {
            body.add(Area(Rectangle2D.Double(mx - 0.01, my - 0.02, 0.065, 0.04)))
            body.add(Area(Ellipse2D.Double(mx - 0.007, my - 0.046, 0.092, 0.092)))
        }
    }
}

private fun addCut(body: Area, mx: Double, my: Double, side: Side) {
    // The blank: a necked cut opening on the edge and biting inward.
    when (side) {
        Side.TOP -> {
            body.subtract(Area(Rectangle2D.Double(mx - 0.02, my - 0.0275, 0.04, 0.06)))
            body.subtract(Area(Ellipse2D.Double(mx - 0.035, my - 0.0025, 0.07, 0.07)))
        }
        Side.BOTTOM -> {
            body.subtract(Area(Rectangle2D.Double(mx - 0.02, my - 0.0325, 0.04, 0.06)))
            body.subtract(Area(Ellipse2D.Double(mx - 0.035, my - 0.0325, 0.07, 0.07)))
        }
        Side.LEFT -> {
            body.subtract(Area(Rectangle2D.Double(mx - 0.0275, my - 0.02, 0.06, 0.04)))
            body.subtract(Area(Ellipse2D.Double(mx - 0.0025, my - 0.035, 0.07, 0.07)))
        }
        Side.RIGHT -> {
            body.subtract(Area(Rectangle2D.Double(mx - 0.0325, my - 0.02, 0.06, 0.04)))
            body.subtract(Area(Ellipse2D.Double(mx - 0.0325, my - 0.035, 0.07, 0.07)))
        }
    }
}

/**
 * One joined piece: a quadrant of the square with rounded corners, a knob
 * reaching outward on [knob] and a cut biting inward on [notch].
 */
fun brandQuadrant(x0: Double, y0: Double, x1: Double, y1: Double, knob: Side, notch: Side): Area {
    val body = Area(RoundRectangle2D.Double(x0, y0, x1 - x0, y1 - y0, 0.045, 0.045))
    val knobMid = if (knob == Side.TOP || knob == Side.BOTTOM) (x0 + x1) / 2 else (y0 + y1) / 2
    val notchMid = if (notch == Side.TOP || notch == Side.BOTTOM) (x0 + x1) / 2 else (y0 + y1) / 2
    val edge = if (knob == Side.TOP || knob == Side.BOTTOM) (if (knob == Side.TOP) y0 else y1) else (if (knob == Side.LEFT) x0 else x1)
    when (knob) {
        Side.TOP -> addKnob(body, knobMid, y0, Side.TOP)
        Side.BOTTOM -> addKnob(body, knobMid, y1, Side.BOTTOM)
        Side.LEFT -> addKnob(body, x0, knobMid, Side.LEFT)
        Side.RIGHT -> addKnob(body, x1, knobMid, Side.RIGHT)
    }
    when (notch) {
        Side.TOP -> addCut(body, notchMid, y0, Side.TOP)
        Side.BOTTOM -> addCut(body, notchMid, y1, Side.BOTTOM)
        Side.LEFT -> addCut(body, x0, notchMid, Side.LEFT)
        Side.RIGHT -> addCut(body, x1, notchMid, Side.RIGHT)
    }
    return body
}

/**
 * The whole mark, drawn into [g]: four joined pieces in a pinwheel. The
 * piece spans 0.962 of the unit box; [span] is that span on the target.
 */
fun paintBrandPiece(g: Graphics2D, cx: Double, cy: Double, span: Double, argb: Int) {
    val scale = span / BRAND_SPAN
    val ox = cx - 0.5 * scale
    val oy = cy - 0.5 * scale
    g.transform(AffineTransform(scale, 0.0, 0.0, scale, ox, oy))
    g.color = Color(argb, true)
    // Pinwheel: head outward, cut inward, a quarter turn per piece.
    g.fill(brandQuadrant(0.10, 0.10, 0.4875, 0.4875, Side.TOP, Side.BOTTOM))
    g.fill(brandQuadrant(0.5125, 0.10, 0.90, 0.4875, Side.RIGHT, Side.LEFT))
    g.fill(brandQuadrant(0.5125, 0.5125, 0.90, 0.90, Side.BOTTOM, Side.TOP))
    g.fill(brandQuadrant(0.10, 0.5125, 0.4875, 0.90, Side.LEFT, Side.RIGHT))
    g.transform(AffineTransform(1.0 / scale, 0.0, 0.0, 1.0 / scale, -ox, -oy))
}

/** The legacy tile: teal rounded square, paper piece, for API 24-25 launchers. */
fun legacyIcon(sizePx: Int): BufferedImage {
    val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    g.color = Color(IconDesign.TEAL, true)
    // Java2D arc dimensions are the corner ellipse DIAMETER; the design
    // fraction is the radius, so double it. This keeps the tile identical to
    // the brand tile in Gallery.kt (RoundedCornerShape(percent = 22)).
    val d = sizePx * IconDesign.LEGACY_CORNER_FRACTION * 2.0
    g.fill(RoundRectangle2D.Double(0.0, 0.0, sizePx.toDouble(), sizePx.toDouble(), d, d))
    paintBrandPiece(g, sizePx / 2.0, sizePx / 2.0, sizePx * IconDesign.LEGACY_SPAN_FRACTION, IconDesign.PAPER)
    g.dispose()
    return image
}

/**
 * One adaptive layer: transparent canvas, piece only. Used for the
 * foreground (paper) and the monochrome sibling (white).
 */
fun adaptiveLayer(sizePx: Int, pieceArgb: Int): BufferedImage {
    val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    paintBrandPiece(
        g,
        sizePx / 2.0,
        sizePx / 2.0,
        sizePx * IconDesign.ADAPTIVE_SPAN_DP / IconDesign.ADAPTIVE_DP,
        pieceArgb,
    )
    g.dispose()
    return image
}

private fun begin(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    return g
}

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
    val bytes = ByteArrayOutputStream()
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
