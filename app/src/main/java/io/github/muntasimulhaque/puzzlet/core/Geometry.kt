package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The engine's own geometry: tiny, pure, allocation-cheap. No Android and no
 * Compose types cross this package (AGENTS.md, Architecture), so every rule
 * here is testable on the plain JVM.
 */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Double) = Vec2(x * k, y * k)
    fun distanceTo(o: Vec2) = hypot(x - o.x, y - o.y)
}

/** An axis-aligned rectangle, as position plus size. Pure; no Compose types. */
data class Area(val x: Double, val y: Double, val w: Double, val h: Double) {
    val minX get() = x
    val minY get() = y
    val maxX get() = x + w
    val maxY get() = y + h
    val centerX get() = x + w / 2.0
    val centerY get() = y + h / 2.0

    fun contains(p: Vec2) = p.x >= x && p.x <= maxX && p.y >= y && p.y <= maxY
}

fun dist(a: Vec2, b: Vec2) = a.distanceTo(b)

/** Clamp a bounding box's top-left so the whole box stays inside [bounds]. */
fun clampBoxTopLeft(topLeft: Vec2, boxW: Double, boxH: Double, bounds: Area): Vec2 {
    val x = topLeft.x.coerceIn(bounds.x, (bounds.x + bounds.w - boxW).coerceAtLeast(bounds.x))
    val y = topLeft.y.coerceIn(bounds.y, (bounds.y + bounds.h - boxH).coerceAtLeast(bounds.y))
    return Vec2(x, y)
}

/**
 * The points of an n-point star polygon. Used by scene art (sparkles) and by
 * the celebration confetti; pure math so both share one source of truth.
 */
fun starPoints(
    center: Vec2,
    rOuter: Double,
    rInner: Double,
    points: Int,
    rotationDeg: Double = 0.0,
): List<Vec2> {
    require(points >= 3) { "A star needs at least 3 points" }
    val rot = Math.toRadians(rotationDeg)
    val step = Math.PI / points
    return List(points * 2) { i ->
        val r = if (i % 2 == 0) rOuter else rInner
        val a = rot + i * step
        Vec2(center.x + r * cos(a), center.y + r * sin(a))
    }
}

