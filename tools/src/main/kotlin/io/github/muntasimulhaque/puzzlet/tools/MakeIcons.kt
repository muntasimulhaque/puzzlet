package io.github.muntasimulhaque.puzzlet.tools

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

    /**
     * The seam, unit space: boundary x, hero knob cy, socket cy. Both
     * knobs share one size and one scale, and the three gaps read equal:
     * tile top to socket head, socket head to knob head, knob head down.
     */
    const val SEAM_X = 0.56
    // Green-approved bite numbers, frozen. The white hero is this exact
    // shape mirrored, congruent, so the pair matches by construction.
    const val BITE_W = 0.062
    const val BITE_D = 0.075
    const val BITE_R = 0.070
    /** Both knobs wear the bite shape at this scale: a little bigger. */
    const val KS = 1.15
    const val KNOB_Y = (2.0 + BITE_R * KS) / 3.0
    const val SOCKET_Y = (1.0 - BITE_R * KS) / 3.0
}

/**
 * The green socket, byte-identical to the approved take: channel mouth at
 * the seam, round chamber inside. Never touch without the owner pointing.
 */
fun socketBite(): Area {
    val t = AffineTransform.getTranslateInstance(IconDesign.SEAM_X, IconDesign.SOCKET_Y)
    t.concatenate(AffineTransform.getScaleInstance(IconDesign.KS, IconDesign.KS))
    return biteUnit().createTransformedArea(t)
}

/** The bite in local coords: mouth at origin opening east, chamber west. */
private fun biteUnit(): Area {
    val d = IconDesign
    val bite = Area()
    bite.add(Area(Rectangle2D.Double(-d.BITE_D, -d.BITE_W / 2.0, d.BITE_D + 0.012, d.BITE_W)))
    bite.add(Area(Ellipse2D.Double(-d.BITE_D - 0.015 - d.BITE_R, -d.BITE_R, d.BITE_R * 2.0, d.BITE_R * 2.0)))
    return bite
}

/** The white hero: the green bite mirrored east at the same scale. */
fun heroKnob(): Area {
    val d = IconDesign
    val t = AffineTransform.getTranslateInstance(d.SEAM_X, d.KNOB_Y)
    t.concatenate(AffineTransform.getScaleInstance(-d.KS, d.KS))
    return biteUnit().createTransformedArea(t)
}

/** The S region: paper field, green-shaped white hero out, green socket in. */
fun seamRegion(): Area {
    val d = IconDesign
    val region = Area(Rectangle2D.Double(-0.05, -0.05, d.SEAM_X + 0.05, 1.10))
    region.add(heroKnob())
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

/** One icon layer: flat tile, bare foreground, or white mono. */
internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val d = IconDesign
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = beginIcon(image)
    if (layer == Layer.TILE) {
        val tileShape = tileShapeFor(size, cornerFraction)
        g.color = Color(d.TEAL, true)
        g.fill(tileShape)
        g.clip = tileShape
    }
    val span = if (layer == Layer.FOREGROUND) size * d.FG_SPAN_DP / d.ADAPTIVE_DP else size * d.TILE_SPAN
    val t = unitTransform(size, span)
    val region = seamRegion().createTransformedArea(t)
    val fillArgb = if (layer == Layer.MONO) d.WHITE else d.PAPER
    // No shadow on white, per owner: the green carries none, so neither
    // does the white. The seam reads on paper against teal contrast alone.
    g.color = Color(fillArgb, true)
    g.fill(region)
    g.dispose()
    return image
}

/** The legacy tile: flat teal, paper seam, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: paper seam on transparency, no baked shadow. */
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
