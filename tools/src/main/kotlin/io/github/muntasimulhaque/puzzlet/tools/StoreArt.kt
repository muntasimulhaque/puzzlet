package io.github.muntasimulhaque.puzzlet.tools

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The store art's shared pieces: the mark on transparency, clean Baloo
 * lettering cropped to its ink, the ink measurement, and the one banner
 * law every composition obeys.
 *
 * The law: all ink sits inside a center box inset 18 percent from the
 * sides and 16 percent from the top and bottom. That box clears Play's
 * own cutoff zones (the console example measures 15 percent on all four
 * sides), the card crop the Play app took on the owner's phone (about
 * 16:9, six and a half percent a side), the harshest common card crop
 * (4:3, which takes 17.4 percent off each side), and a 3:1 wide banner
 * trim (15.9 percent off top and bottom). The ground is brand teal to
 * every edge, so a rounded corner crop only rounds teal.
 */

/** The feature graphic's canvas, the Play required 1024 x 500. */
const val STORE_W = 1024
const val STORE_H = 500
const val STORE_GROUND = 0xFF0C7A64.toInt()
const val STORE_PAPER = 0xFFFAF6EF.toInt()
const val STORE_SOFT = 0xE6FAF6EF.toInt()
const val STORE_NAME = "Puzzlet"
const val STORE_LINE = "A calm jigsaw for small hands."
const val STORE_EXTRA = "baloo2_extrabold.ttf"
const val STORE_BOLD = "baloo2_bold.ttf"

/** The safe box: 18 percent in from the sides, 16 from the top and bottom. */
const val SAFE_X = 0.18
const val SAFE_Y = 0.16

/** The safe box as left, right, top, bottom. */
fun safeBox(): IntArray = intArrayOf(
    ceil(STORE_W * SAFE_X).toInt(),
    (STORE_W * (1.0 - SAFE_X)).toInt(),
    ceil(STORE_H * SAFE_Y).toInt(),
    (STORE_H * (1.0 - SAFE_Y)).toInt(),
)

/**
 * The mark on transparency, sized so its ink spans about [art] pixels
 * wide. The painted mark's ink measures about 0.55 of its canvas plus the
 * soft shadow, so it renders a little large and crops to the ink.
 */
fun markArt(art: Double): BufferedImage =
    cropToInk(markForeground((art / 0.53).roundToInt()))

/** One line of clean Baloo text, cropped to its ink. */
fun textArt(rootDir: File, text: String, file: String, size: Float, argb: Int): BufferedImage {
    val canvas = BufferedImage(2048, 700, BufferedImage.TYPE_INT_ARGB)
    val g = graphics(canvas)
    drawCleanString(g, text, file, size, argb, 100f, 450f, rootDir)
    g.dispose()
    val ink = cropToInk(canvas)
    check(ink.width < canvas.width - 4 && ink.height < canvas.height - 4) { "text ran off its canvas: $text" }
    return ink
}

/**
 * The take's lockup scaled to fill the cutoff-safe box and centered on it,
 * over the brand teal. The scale takes the tighter of the two axes, so one
 * side of the box may keep some air: that is the design breathing, never a
 * crop risk. Output is opaque (the Play requirement), and the teal runs to
 * every edge, so a rounded corner crop only rounds teal.
 */
fun composeBanner(layer: BufferedImage): BufferedImage {
    val (x0, x1, y0, y1) = safeBox().toList()
    val bounds = inkRect(layer)
    val sx = (x1 - x0 + 1).toDouble() / (bounds[1] - bounds[0] + 1).toDouble()
    val sy = (y1 - y0 + 1).toDouble() / (bounds[3] - bounds[2] + 1).toDouble()
    // A hair inside the box, so antialiasing and rounding cannot push a
    // pixel of ink past the law.
    val scaled = scale(layer, min(sx, sy) * 0.99)
    val sb = inkRect(scaled)
    val dx = (STORE_W / 2 - (sb[0] + sb[1]) / 2).coerceIn(x0 - sb[0], x1 - sb[1])
    val dy = (STORE_H / 2 - (sb[2] + sb[3]) / 2).coerceIn(y0 - sb[2], y1 - sb[3])
    val image = BufferedImage(STORE_W, STORE_H, BufferedImage.TYPE_INT_RGB)
    val g = graphics(image)
    g.color = Color(STORE_GROUND, true)
    g.fill(Rectangle2D.Double(0.0, 0.0, STORE_W.toDouble(), STORE_H.toDouble()))
    g.drawImage(scaled, dx, dy, null)
    g.dispose()
    return image
}

/** Every visible pixel inside the cutoff-safe box, or a loud failure. */
fun checkInsideBox(image: BufferedImage, what: String) {
    val (x0, x1, y0, y1) = safeBox().toList()
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            if (differs(image.getRGB(x, y)) && (x < x0 || x > x1 || y < y0 || y > y1)) {
                error("$what escapes the cutoff-safe box at ($x, $y)")
            }
        }
    }
}

/** True when a pixel is visibly more than the ground, not an AA blend. */
fun differs(rgb: Int): Boolean {
    val dr = abs(((rgb shr 16) and 0xFF) - ((STORE_GROUND shr 16) and 0xFF))
    val dg = abs(((rgb shr 8) and 0xFF) - ((STORE_GROUND shr 8) and 0xFF))
    val db = abs((rgb and 0xFF) - (STORE_GROUND and 0xFF))
    return maxOf(dr, dg, db) > 24
}

/** The ink box of an image on transparency: left, right, top, bottom. */
fun inkRect(image: BufferedImage): IntArray {
    var x0 = image.width
    var y0 = image.height
    var x1 = -1
    var y1 = -1
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            if (((image.getRGB(x, y) ushr 24) and 0xFF) > 2) {
                if (x < x0) x0 = x
                if (x > x1) x1 = x
                if (y < y0) y0 = y
                if (y > y1) y1 = y
            }
        }
    }
    return intArrayOf(x0, x1, y0, y1)
}

/** The ink box of an opaque image on the teal ground: left, right, top, bottom. */
fun bannerInkRect(image: BufferedImage): IntArray {
    var x0 = image.width
    var y0 = image.height
    var x1 = -1
    var y1 = -1
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            if (differs(image.getRGB(x, y))) {
                if (x < x0) x0 = x
                if (x > x1) x1 = x
                if (y < y0) y0 = y
                if (y > y1) y1 = y
            }
        }
    }
    return intArrayOf(x0, x1, y0, y1)
}

private fun cropToInk(image: BufferedImage): BufferedImage {
    val (x0, x1, y0, y1) = inkRect(image).toList()
    check(x1 >= x0 && y1 >= y0) { "cropToInk found nothing to crop" }
    return image.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1)
}

private fun scale(image: BufferedImage, k: Double): BufferedImage {
    val out = transparentCanvas()
    val g = graphics(out)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.drawImage(image, 0, 0, (image.width * k).roundToInt(), (image.height * k).roundToInt(), null)
    g.dispose()
    return out
}

fun transparentCanvas(): BufferedImage =
    BufferedImage(STORE_W, STORE_H, BufferedImage.TYPE_INT_ARGB)

fun graphics(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    return g
}

/** Draw [image] centered on ([cx], [cy]). */
fun drawAt(g: Graphics2D, image: BufferedImage, cx: Int, cy: Int) {
    g.drawImage(image, cx - image.width / 2, cy - image.height / 2, null)
}
