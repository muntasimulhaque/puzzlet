package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The soft wash a picture's accent turns into for the coin that holds the
 * finished picture up. The accent is the picture's own colour (a real shape
 * colour from its palette); the wash keeps that colour's hue but pins its
 * lightness and chroma to one band, so a bold or dark accent (the night
 * sky, the toadstool cap) and a pale one (the mint scoop) both come out
 * equally gentle, and every on state belongs to the same family (D-080).
 *
 * The band is measured, not eyeballed, in CIE LCh: the house lesson from
 * the mark colours (D-052) is that two colours are not two colours until
 * they are measured. L* 88 sits one clear step below the card (L* 99.4)
 * and the chroma 13 gives a wash that reads without shouting. Hue is the
 * only thing the picture contributes, which is exactly the point: the
 * button changes with the picture, the gentleness never does.
 */
const val WASH_LIGHTNESS = 88.0
const val WASH_CHROMA = 13.0

/** One colour in CIE L*a*b*, with the polar pair the wash cares about. */
internal data class Lab(val l: Double, val a: Double, val b: Double) {
    val chroma: Double get() = hypot(a, b)
    /** Hue in degrees, 0 at red and rising through yellow, green, blue. */
    val hue: Double get() = ((Math.toDegrees(atan2(b, a)) % 360.0) + 360.0) % 360.0
}

/** The accent's wash: same hue, the one band, opaque sRGB. */
fun softWash(accent: Long): Long {
    val lab = labOf(accent)
    val hue = Math.toRadians(lab.hue)
    val a = WASH_CHROMA * cos(hue)
    val b = WASH_CHROMA * sin(hue)

    val fy = (WASH_LIGHTNESS + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0
    fun inverse(t: Double): Double =
        if (t > 6.0 / 29.0) t * t * t else 3.0 * (6.0 / 29.0).pow(2.0) * (t - 4.0 / 29.0)

    val x = 0.95047 * inverse(fx)
    val y = inverse(fy)
    val z = 1.08883 * inverse(fz)

    val r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
    val g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
    val bl = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

    return 0xFF000000L or
        (channelOf(r).toLong() shl 16) or
        (channelOf(g).toLong() shl 8) or
        channelOf(bl).toLong()
}

/** sRGB to Lab through linear light and D65. */
internal fun labOf(argb: Long): Lab {
    val r = toLinear(((argb shr 16) and 0xFF).toInt())
    val g = toLinear(((argb shr 8) and 0xFF).toInt())
    val b = toLinear((argb and 0xFF).toInt())

    val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
    val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
    fun forward(t: Double): Double =
        if (t > 0.008856) t.pow(1.0 / 3.0) else 7.787 * t + 16.0 / 116.0

    val fx = forward(x)
    val fy = forward(y)
    val fz = forward(z)
    return Lab(
        l = 116.0 * fy - 16.0,
        a = 500.0 * (fx - fy),
        b = 200.0 * (fy - fz),
    )
}

private fun toLinear(v: Int): Double {
    val c = v.coerceIn(0, 255) / 255.0
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

private fun channelOf(v: Double): Int {
    val c = v.coerceIn(0.0, 1.0)
    val s = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055
    return (s * 255.0).roundToInt().coerceIn(0, 255)
}
