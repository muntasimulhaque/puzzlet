package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * Design: a real jigsaw piece carrying the finished delight. The piece
 * speaks the same joint language as the game's own cut (a mushroom knob:
 * neck narrower than head, one tab up, one socket right), and inside it
 * sails the little boat (paper sails, coral hull, honey sun). A child
 * sees a puzzle; a parent feels warmth. Rendered three ways: the legacy
 * tile for API 24-25, the adaptive foreground for API 26+ over the flat
 * teal background, and a white monochrome sibling (piece silhouette) for
 * Android 13+ themed icons.
 *
 * Colors here mirror app/src/main/res/values/colors.xml plus the scene
 * palette (coral, honey). Change both together, then run makeIcons and
 * commit the regenerated PNGs.
 */
object IconDesign {
    const val TEAL: Int = 0xFF0C7A64.toInt()
    const val DEEP: Int = 0xFF085949.toInt()
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val HONEY: Int = 0xFFF0B429.toInt()
    const val CORAL: Int = 0xFFE4572EL.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /** Macro bleed of the mark on the adaptive canvas; tab tip and hull stay in the 66 dp circle. */
    const val FG_SPAN_DP = 90.0
    /** Fraction of a full-art tile the mark spans. */
    const val TILE_SPAN = 0.98
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
    /** Store tile corner radius as a fraction of the tile. */
    const val STORE_CORNER_FRACTION = 0.19

    /** The piece body in tile unit space (0..1); corners die-cut sharp. */
    const val PX0 = 0.26
    const val PX1 = 0.78
    const val PY0 = 0.26
    const val PY1 = 0.78

    /**
     * The knob, in the exact proportions of core PieceCut: neck half
     * 0.34 of the knob height, head reach 0.95, control lifts 0.35 and
     * 0.45. Same numbers, same curves, no jitter (centred, deliberate).
     */
    const val KNOB_H = 0.10
    const val KNOB_NECK = 0.34
    const val KNOB_HEAD = 0.95
    const val TAB_MID = 0.50
    const val SOCK_MID = 0.55
}

/**
 * The piece silhouette, cut the way the game cuts: flat base lines joined
 * by two-cubic mushroom knobs, one tab up, one socket right. The knob
 * below is PieceCut.mushroom without the gameplay jitter: a base line,
 * then two cubics through a head that stands a full knob height past the
 * edge and reaches well past its neck, which is what makes the silhouette
 * a mushroom and not a bump.
 */
fun pieceArea(): Area {
    val d = IconDesign
    val path = Path2D.Double()
    // Top edge, left to right, the tab reaching up.
    path.moveTo(d.PX0, d.PY0)
    hKnob(path, d.PX0, d.PX1, d.PY0, d.TAB_MID, d.KNOB_H)
    path.lineTo(d.PX1, d.PY0)
    // Right edge, top to bottom, the socket opening inward.
    path.lineTo(d.PX1, d.SOCK_MID - d.KNOB_NECK * d.KNOB_H)
    vKnob(path, d.SOCK_MID, d.PX1, d.KNOB_H)
    path.lineTo(d.PX1, d.PY1)
    // Bottom and left close the body, flat like a corner piece.
    path.lineTo(d.PX0, d.PY1)
    path.closePath()
    return Area(path)
}

/** Horizontal knob from the current point to (x1, y); sign +1 reaches up. */
private fun hKnob(path: Path2D.Double, x0: Double, x1: Double, y: Double, mid: Double, kh: Double) {
    val d = IconDesign
    val neck = d.KNOB_NECK * kh
    val head = d.KNOB_HEAD * kh
    path.lineTo(mid - neck, y)
    path.curveTo(
        mid - neck + 0.10 * kh, y - 0.35 * kh,
        mid - head, y - 0.45 * kh,
        mid, y - kh,
    )
    path.curveTo(
        mid + head, y - 0.45 * kh,
        mid + neck - 0.10 * kh, y - 0.35 * kh,
        mid + neck, y,
    )
    path.lineTo(x1, y)
}

/** Vertical knob from the current point down to (x, ...); sign +1 reaches left. */
private fun vKnob(path: Path2D.Double, mid: Double, x: Double, kh: Double) {
    val d = IconDesign
    val neck = d.KNOB_NECK * kh
    val head = d.KNOB_HEAD * kh
    path.curveTo(
        x - 0.35 * kh, mid - neck + 0.10 * kh,
        x - 0.45 * kh, mid - head,
        x - kh, mid,
    )
    path.curveTo(
        x - 0.45 * kh, mid + head,
        x - 0.35 * kh, mid + neck - 0.10 * kh,
        x, mid + neck,
    )
}

/** Boat parts in tile unit space, drawn back to front inside the piece. */
private fun boatAreas(): List<Pair<String, Area>> {
    fun circle(cx: Double, cy: Double, r: Double): Area =
        Area(Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0))
    fun triangle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Area =
        Area(Path2D.Double().apply {
            moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); closePath()
        })
    fun quad(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Area =
        Area(Path2D.Double().apply {
            moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); lineTo(dx, dy); closePath()
        })
    return listOf(
        "sun" to circle(0.63, 0.42, 0.055),
        "jib" to triangle(0.485, 0.48, 0.485, 0.60, 0.40, 0.60),
        "main" to triangle(0.51, 0.44, 0.51, 0.60, 0.63, 0.60),
        "hull" to quad(0.36, 0.63, 0.64, 0.63, 0.59, 0.71, 0.41, 0.71),
        "water" to Area(Rectangle2D.Double(0.33, 0.745, 0.34, 0.022)),
    )
}

private fun partColor(part: String, mono: Boolean): Int {
    if (mono) return IconDesign.WHITE
    return when (part) {
        "sun" -> IconDesign.HONEY
        "hull" -> IconDesign.CORAL
        else -> IconDesign.PAPER
    }
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

/** One icon layer: flat tile, bare foreground, or white mono silhouette. */
internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val d = IconDesign
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = beginIcon(image)
    val span = if (layer == Layer.FOREGROUND) size * d.FG_SPAN_DP / d.ADAPTIVE_DP else size * d.TILE_SPAN
    val t = unitTransform(size, span)
    val mono = layer == Layer.MONO
    g.color = Color(if (mono) d.WHITE else d.TEAL, true)
    g.fill(pieceArea().createTransformedArea(t))
    if (!mono) {
        for ((part, area) in boatAreas()) {
            g.color = Color(partColor(part, false), true)
            g.fill(area.createTransformedArea(t))
        }
    }
    g.dispose()
    return image
}

/** The legacy tile: teal piece with its boat, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: the piece with its boat on transparency. */
fun adaptiveLayer(sizePx: Int, pieceArgb: Int): BufferedImage {
    // The monochrome sibling renders the piece silhouette in white; the
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
