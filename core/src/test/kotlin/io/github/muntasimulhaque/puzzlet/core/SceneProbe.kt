package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A point sampler over scene data, used by the one rule every shipped
 * picture must pass: no piece of any picture, at any count, may be a single
 * flat colour (AGENTS.md, D-050). A child cannot place a blank piece, so
 * the test looks at the picture the way the cut will cut it and counts
 * colour, not intentions.
 */

/** The colour on top at a point in the unit square; null where nothing is. */
internal fun colourAt(spec: SceneSpec, x: Double, y: Double): Long? {
    var found: Long? = null
    for (shape in spec.shapes) {
        if (covers(shape, x, y)) found = shape.argb
    }
    return found
}

private fun covers(shape: SceneShape, x: Double, y: Double): Boolean = when (shape) {
    is CircleSpec -> hypot(x - shape.center.x, y - shape.center.y) <= shape.radius
    is EllipseSpec -> inEllipse(x, y, shape.center, shape.rx, shape.ry, shape.angleDeg)
    is RoundRectSpec -> inRoundRect(x, y, shape)
    is PolygonSpec -> polygonContains(shape.points, x, y)
    is RingSpec -> {
        val shrink = ((shape.rx - shape.thickness) / shape.rx).coerceIn(0.25, 0.9)
        val innerRx = shape.rx * shrink
        val innerRy = shape.ry * shrink
        inEllipse(x, y, shape.center, shape.rx, shape.ry, shape.angleDeg) &&
            !inEllipse(x, y, shape.center, innerRx, innerRy, shape.angleDeg)
    }
}

private fun inEllipse(
    x: Double,
    y: Double,
    center: Vec2,
    rx: Double,
    ry: Double,
    angleDeg: Double,
): Boolean {
    if (rx <= 0.0 || ry <= 0.0) return false
    val dx = x - center.x
    val dy = y - center.y
    val a = Math.toRadians(-angleDeg)
    val px = dx * cos(a) - dy * sin(a)
    val py = dx * sin(a) + dy * cos(a)
    return (px * px) / (rx * rx) + (py * py) / (ry * ry) <= 1.0
}

private fun inRoundRect(x: Double, y: Double, s: RoundRectSpec): Boolean {
    if (s.w <= 0.0 || s.h <= 0.0) return false
    val cx = s.x + s.w / 2.0
    val cy = s.y + s.h / 2.0
    val dx = x - cx
    val dy = y - cy
    val a = Math.toRadians(-s.angleDeg)
    val px = dx * cos(a) - dy * sin(a)
    val py = dx * sin(a) + dy * cos(a)
    val r = s.cornerRadius.coerceAtMost(minOf(s.w, s.h) / 2.0)
    val ox = px.absoluteValue - (s.w / 2.0 - r)
    val oy = py.absoluteValue - (s.h / 2.0 - r)
    return when {
        ox <= 0.0 -> py.absoluteValue <= s.h / 2.0
        oy <= 0.0 -> px.absoluteValue <= s.w / 2.0
        else -> ox * ox + oy * oy <= r * r
    }
}

/** A flattened piece outline in piece-local coordinates. */
internal fun outlinePoints(shape: PieceShape, perCurve: Int = 8): List<Vec2> {
    val out = ArrayList<Vec2>()
    for (seg in shape.segments) {
        for (i in 0 until perCurve) {
            val t = i.toDouble() / perCurve
            out.add(cubicAt(seg, t))
        }
    }
    return out
}

private fun cubicAt(c: Cubic, t: Double): Vec2 {
    val u = 1.0 - t
    val a = u * u * u
    val b = 3.0 * u * u * t
    val d = 3.0 * u * t * t
    val e = t * t * t
    return Vec2(
        a * c.p0.x + b * c.c1.x + d * c.c2.x + e * c.p1.x,
        a * c.p0.y + b * c.c1.y + d * c.c2.y + e * c.p1.y,
    )
}

internal fun polygonContains(poly: List<Vec2>, x: Double, y: Double): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private val Double.absoluteValue get() = if (this < 0.0) -this else this
