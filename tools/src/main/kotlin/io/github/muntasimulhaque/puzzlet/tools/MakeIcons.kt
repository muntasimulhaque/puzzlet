package io.github.muntasimulhaque.puzzlet.tools

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.exp
import kotlin.system.exitProcess

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * Design (the owner's pick, S): one seam, two tones. A paper field fills
 * the left of the tile and a hero knob reaches right into the deep, while
 * a socket bite opens above it. Knob and socket, the whole joint language,
 * nothing else. Rendered three ways: the legacy tile for API 24-25, the
 * adaptive foreground for API 26+ over the flat teal background, and a
 * white monochrome sibling for Android 13+ themed icons.
 *
 * Colors here mirror app/src/main/res/values/colors.xml. Change both
 * together, then run makeIcons and commit the regenerated PNGs.
 */
object IconDesign {
    const val TEAL: Int = 0xFF0C7A64.toInt()
    const val DEEP: Int = 0xFF085949.toInt()
    const val PAPER: Int = 0xFFFAF6EF.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /** Macro bleed of the mark on the adaptive canvas; knob and socket stay in the 66 dp circle. */
    const val FG_SPAN_DP = 96.0
    /** Fraction of a full-art tile the mark spans. */
    const val TILE_SPAN = 0.98
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
    /** Store tile corner radius as a fraction of the tile. */
    const val STORE_CORNER_FRACTION = 0.19

    /** The seam, unit space: boundary x, hero knob cy, socket cy. */
    const val SEAM_X = 0.56
    const val KNOB_Y = 0.52
    const val SOCKET_Y = 0.24
    const val NECK_W = 0.088
    const val NECK_L = 0.115
    const val HEAD_R = 0.135
    const val BITE_W = 0.062
    const val BITE_D = 0.075
    const val BITE_R = 0.070
}

/** The socket bite alone, for shading after the region is filled. */
fun socketBite(): Area {
    val d = IconDesign
    val bite = Area()
    bite.add(Area(Rectangle2D.Double(d.SEAM_X - d.BITE_D, d.SOCKET_Y - d.BITE_W / 2.0, d.BITE_D + 0.012, d.BITE_W)))
    bite.add(Area(Ellipse2D.Double(d.SEAM_X - d.BITE_D - 0.015 - d.BITE_R, d.SOCKET_Y - d.BITE_R, d.BITE_R * 2.0, d.BITE_R * 2.0)))
    return bite
}

/** The S region: paper field, hero knob, minus the socket bite. */
fun seamRegion(): Area {
    val d = IconDesign
    val region = Area(Rectangle2D.Double(-0.05, -0.05, d.SEAM_X + 0.05, 1.10))
    val n = d.NECK_W / 2.0
    region.add(Area(Rectangle2D.Double(d.SEAM_X - 0.005, d.KNOB_Y - n, d.NECK_L + 0.005, d.NECK_W)))
    val hx = d.SEAM_X + d.NECK_L - d.HEAD_R * 0.55
    region.add(Area(Ellipse2D.Double(hx - d.HEAD_R, d.KNOB_Y - d.HEAD_R, d.HEAD_R * 2.0, d.HEAD_R * 2.0)))
    region.subtract(socketBite())
    return region
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

private fun softBlur(src: BufferedImage, radius: Int): BufferedImage {
    val size = radius * 2 + 1
    val data = FloatArray(size * size)
    var sum = 0.0
    for (y in 0 until size) for (x in 0 until size) {
        val dx = (x - radius).toDouble()
        val dy = (y - radius).toDouble()
        val v = exp(-(dx * dx + dy * dy) / (2.0 * radius * radius / 4.0))
        data[y * size + x] = v.toFloat()
        sum += v
    }
    for (i in data.indices) data[i] = (data[i] / sum).toFloat()
    return ConvolveOp(Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null).filter(src, null)
}

/** Map unit space onto the tile: unit centre to tile centre, span across. */
private fun tileShapeFor(size: Int, cornerFraction: Double): RoundRectangle2D.Double {
    val corner = size * cornerFraction * 2.0
    return RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), corner, corner)
}

private fun unitTransform(size: Int, span: Double): AffineTransform {
    val t = AffineTransform.getTranslateInstance(size / 2.0 - 0.5 * span, size / 2.0 - 0.5 * span)
    t.concatenate(AffineTransform.getScaleInstance(span, span))
    return t
}

/** One icon layer: full art tile, bare foreground, or white mono. */
internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val d = IconDesign
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = beginIcon(image)
    if (layer == Layer.TILE) {
        val tileShape = tileShapeFor(size, cornerFraction)
        g.paint = GradientPaint(0f, 0f, Color(d.TEAL), 0f, size.toFloat(), Color(d.DEEP))
        g.fill(tileShape)
        g.clip = tileShape
    }
    val span = if (layer == Layer.FOREGROUND) size * d.FG_SPAN_DP / d.ADAPTIVE_DP else size * d.TILE_SPAN
    val t = unitTransform(size, span)
    val region = seamRegion().createTransformedArea(t)
    val tileArea = if (layer == Layer.TILE) Area(tileShapeFor(size, cornerFraction)) else null
    val fillArgb = if (layer == Layer.MONO) d.WHITE else d.PAPER
    if (layer != Layer.MONO) {
        val shade = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val gs = beginIcon(shade)
        gs.color = Color(d.DEEP, true)
        gs.fill(region)
        gs.dispose()
        val keep = g.composite
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
        g.drawImage(softBlur(shade, maxOf(2, size / 56)), size / 64, size / 72, null)
        g.composite = keep
    }
    g.color = Color(fillArgb, true)
    g.fill(region)
    if (layer != Layer.MONO) {
        g.stroke = BasicStroke(size * 0.008f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(d.DEEP, true)
        g.draw(region)
        val bite = socketBite().createTransformedArea(t)
        val keepClip = g.clip
        g.clip = bite
        g.paint = GradientPaint(0f, 0f, Color(8, 89, 73, 110), 0f, size * 0.30f, Color(8, 89, 73, 0))
        g.fillRect(0, 0, size, size)
        g.clip = keepClip
        val keepLight = g.clip
        val lightClip = Area(region)
        if (tileArea != null) lightClip.intersect(tileArea)
        g.clip = lightClip
        g.paint = GradientPaint(0f, 0f, Color(255, 255, 255, 26), 0f, size * 0.5f, Color(255, 255, 255, 0))
        g.fillRect(0, 0, size, size)
        g.clip = keepLight
    }
    g.dispose()
    return image
}

/** The legacy tile: full art, teal gradient, paper seam, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: paper seam on transparency, shadow baked in. */
fun adaptiveLayer(sizePx: Int, pieceArgb: Int): BufferedImage {
    // The monochrome sibling renders the same silhouette in white; the
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
