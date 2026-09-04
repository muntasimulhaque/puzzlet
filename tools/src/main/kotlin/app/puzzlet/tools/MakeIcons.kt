package app.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
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
 * Design (AGENTS.md, Design seeds): a lagoon-teal tile with a warm-paper
 * jigsaw piece. One glyph, one geometry, rendered three ways: the legacy
 * tile for API 24-25, the adaptive foreground for API 26+, and a white
 * monochrome sibling for Android 13+ themed icons. The app's brand screen
 * reuses the committed foreground PNG, so the mark has one source of truth.
 *
 * Colors here mirror app/src/main/res/values/colors.xml. Change both
 * together, then run makeIcons and commit the regenerated PNGs.
 */
object IconDesign {
    const val TEAL: Int = 0xFF0C7A64.toInt()
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /** Glyph box inside the adaptive canvas; fits the 66 dp safe circle. */
    const val ADAPTIVE_GLYPH_DP = 44.0
    /** Glyph box as a fraction of the legacy tile. */
    const val LEGACY_GLYPH_FRACTION = 0.60
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
}

/**
 * The jigsaw piece, in a unit box: a rounded body with a knob on top, a knob
 * on the right and a blank notch in the bottom, one flat edge. The necks are
 * narrower than the knobs, which is what makes it read as a puzzle piece
 * rather than a sticker.
 */
fun puzzlePiece(): Area {
    val body = Area(RoundRectangle2D.Double(0.16, 0.32, 0.52, 0.52, 0.10, 0.10))
    val topNeck = Area(Rectangle2D.Double(0.37, 0.20, 0.10, 0.13))
    val topKnob = Area(Ellipse2D.Double(0.305, 0.07, 0.23, 0.23))
    val rightNeck = Area(Rectangle2D.Double(0.67, 0.53, 0.13, 0.10))
    val rightKnob = Area(Ellipse2D.Double(0.70, 0.465, 0.23, 0.23))
    val bottomNotch = Area(Ellipse2D.Double(0.305, 0.725, 0.23, 0.23))
    val piece = Area(body)
    piece.add(topNeck)
    piece.add(topKnob)
    piece.add(rightNeck)
    piece.add(rightKnob)
    piece.subtract(bottomNotch)
    return piece
}

/** The legacy tile: teal rounded square, paper glyph, for API 24-25 launchers. */
fun legacyIcon(sizePx: Int): BufferedImage {
    val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    g.color = Color(IconDesign.TEAL, true)
    // Java2D arc dimensions are the corner ellipse DIAMETER; the design
    // fraction is the radius, so double it. This keeps the tile identical to
    // the brand tile in Brand.kt (RoundedCornerShape(percent = 22)).
    val d = sizePx * IconDesign.LEGACY_CORNER_FRACTION * 2.0
    g.fill(RoundRectangle2D.Double(0.0, 0.0, sizePx.toDouble(), sizePx.toDouble(), d, d))
    paintGlyph(g, sizePx.toDouble(), IconDesign.LEGACY_GLYPH_FRACTION, IconDesign.PAPER)
    g.dispose()
    return image
}

/**
 * One adaptive layer: transparent canvas, glyph only. Used for the
 * foreground (paper) and the monochrome sibling (white).
 */
fun adaptiveLayer(sizePx: Int, glyphArgb: Int): BufferedImage {
    val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    paintGlyph(g, sizePx.toDouble(), IconDesign.ADAPTIVE_GLYPH_DP / IconDesign.ADAPTIVE_DP, glyphArgb)
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

/** Draw the piece scaled and centered into a square of [canvas] units. */
private fun paintGlyph(g: Graphics2D, canvas: Double, glyphFraction: Double, argb: Int) {
    val piece = puzzlePiece()
    val bounds = piece.bounds2D
    val box = canvas * glyphFraction
    val scale = box / maxOf(bounds.width, bounds.height)
    val tx = (canvas - bounds.width * scale) / 2.0 - bounds.x * scale
    val ty = (canvas - bounds.height * scale) / 2.0 - bounds.y * scale
    g.transform(AffineTransform(scale, 0.0, 0.0, scale, tx, ty))
    g.color = Color(argb, true)
    g.fill(piece)
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
