package io.github.muntasimulhaque.puzzlet.tools

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * Design: the gather. Three chunky pieces (sky, coral, gold) closing in
 * on the honey home piece with its sockets open, the moment before the
 * click. One hero colour per piece, generous air around the field, flat
 * fills only: drawn small on purpose so no launcher mask ever truncates
 * a piece. Rendered three ways: the legacy tile for API 24-25 (paper
 * tile, gather on top), the adaptive foreground for API 26+ (gather on
 * transparency over the paper background, wholly inside the 66 dp mask
 * circle), and a white monochrome sibling (gather silhouette) for
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

    /** The four gather pieces: sky and coral above, gold and honey home below. */
    const val SKY: Int = 0xFF6BB7D6.toInt()
    const val CORAL: Int = 0xFFE4572E.toInt()
    const val HONEY_LIGHT: Int = 0xFFF6C84F.toInt()
    const val HONEY: Int = 0xFFF0B429.toInt()

    /** The densities the house ships, in scale order. */
    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    /** Legacy tile base size, dp. */
    const val LEGACY_DP = 48.0
    /** Adaptive layer canvas, dp (the 108 dp full-bleed square). */
    const val ADAPTIVE_DP = 108.0
    /**
     * Adaptive foreground inset as a canvas fraction: the whole gather
     * sits inside the 66 dp mask circle with room to spare, so no
     * launcher shape ever truncates a piece.
     */
    const val FG_INSET = 0.27
    /** Fraction of a full-art tile the gather field spans: small, corners safe. */
    const val TILE_SPAN = 0.88
    /**
     * Store tile field fraction: the store corner bites deeper, so the
     * field tucks slightly larger into the surviving corner tips.
     */
    const val STORE_SPAN = 0.90
    /** Legacy tile corner radius as a fraction of the tile. */
    const val LEGACY_CORNER_FRACTION = 0.22
    /** Store tile corner radius as a fraction of the tile. */
    const val STORE_CORNER_FRACTION = 0.19

    /** Wanderer gap as a share of the piece side. */
    const val GAP_FRAC = 0.17

    /**
     * Knob profile on a unit edge: stem half-width, head centre height,
     * head radius. Shared with the candidate renderer, one source.
     */
    const val KNOB_STEM = 0.15
    const val KNOB_HEAD_C = 0.085
    const val KNOB_HEAD_R = 0.155
}

internal enum class Layer { TILE, FOREGROUND, MONO }

/**
 * One icon layer: paper tile with the gather, bare gather, or white
 * gather. The gather painter owns every pixel; fills of plain paths
 * only, no strokes, no booleans, so every JDK pins the same bytes.
 */
internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val d = IconDesign
    return when (layer) {
        Layer.TILE -> {
            // Legacy and store tiles share this branch; the corner
            // fraction tells them apart, each with its fitted span.
            val span = if (cornerFraction == d.LEGACY_CORNER_FRACTION) d.TILE_SPAN else d.STORE_SPAN
            Gather.paint(
                size,
                tile = true,
                groundArgb = d.PAPER,
                cornerFraction = cornerFraction,
                insetFrac = (1.0 - span) / 2.0,
            )
        }
        Layer.FOREGROUND -> Gather.paint(
            size,
            tile = false,
            groundArgb = 0,
            insetFrac = d.FG_INSET,
        )
        Layer.MONO -> Gather.paint(
            size,
            tile = false,
            groundArgb = 0,
            mono = true,
            insetFrac = d.FG_INSET,
        )
    }
}

/** The legacy tile: paper tile with the gather, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: the gather on transparency. */
fun adaptiveLayer(sizePx: Int, pieceArgb: Int): BufferedImage {
    // The monochrome sibling renders the gather silhouette in white; the
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
