package io.github.muntasimulhaque.puzzlet.core

import kotlin.random.Random

/**
 * Grounds: the graded washes a picture sits on, and the small texture that
 * keeps a big one from being a blank.
 *
 * One flat rectangle is a wash, and a piece cut out of a wash is a piece
 * with nothing on it: nothing to recognise, nothing to think with, which is
 * exactly the puzzle a three-year-old cannot solve (AGENTS.md, D-039 and
 * D-050). So every picture here sits on a ground that grades from one tone
 * to another, which reads as printed colour to the eye and gives each piece
 * a little positional truth: the top of a sky is not the bottom of it.
 * [texture] adds the rest, an even scatter of small inanimate marks over a
 * region, the way a picture book fills a page with more than one thing.
 */

/** Bands from [top] to [bottom] across a region: a gradient as plain shapes. */
internal fun ground(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    top: Long,
    bottom: Long,
    steps: Int = 12,
): List<SceneShape> {
    if (h <= 0.0 || w <= 0.0) return emptyList()
    val band = h / steps
    return List(steps) { i ->
        val t = i.toDouble() / (steps - 1).coerceAtLeast(1)
        // A hair of overlap, so no hairline of paper shows between bands.
        RoundRectSpec(x, y + i * band, w, band + 0.0015, 0.0, mixArgb(top, bottom, t))
    }
}

/**
 * An even scatter of small marks over a region, jittered from a seed so the
 * same picture always speckles the same way. Even first, so it reads as
 * texture rather than as noise: a jittered grid, not a random spill.
 */
internal fun texture(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    cols: Int,
    rows: Int,
    radius: Double,
    argb: Long,
    seed: Int,
    jitter: Double = 0.34,
): List<SceneShape> {
    if (w <= 0.0 || h <= 0.0 || cols <= 0 || rows <= 0) return emptyList()
    val rnd = Random(seed)
    val stepX = w / cols
    val stepY = h / rows
    return buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = x + (c + 0.5) * stepX + (rnd.nextDouble() - 0.5) * stepX * jitter
                val cy = y + (r + 0.5) * stepY + (rnd.nextDouble() - 0.5) * stepY * jitter
                add(CircleSpec(Vec2(cx, cy), radius, argb))
            }
        }
    }
}

/**
 * A row of rolling hills: overlapping caps in two or three greens, jittered
 * from a seed. A single flat hill is a blank piece waiting to happen, and a
 * row of caps gives every piece at the bottom of the picture an edge of its
 * own to recognise, which is the whole point of the ground.
 */
internal fun rollingHills(
    baseY: Double,
    tall: Double,
    tones: List<Long>,
    count: Int,
    seed: Int,
): List<SceneShape> {
    if (count <= 0 || tones.isEmpty()) return emptyList()
    val rnd = Random(seed)
    return List(count) { i ->
        val cx = (i + 0.5) / count + (rnd.nextDouble() - 0.5) * 0.06
        val cy = baseY + rnd.nextDouble() * 0.05
        val rx = (1.0 / count) * (1.15 + rnd.nextDouble() * 0.5)
        val ry = tall * (0.82 + rnd.nextDouble() * 0.36)
        EllipseSpec(Vec2(cx, cy), rx, ry, tones[i % tones.size])
    }
}

/** Blend two sRGB ARGB longs, straight through per channel. */
internal fun mixArgb(from: Long, to: Long, t: Double): Long {
    val k = t.coerceIn(0.0, 1.0)
    fun ch(shift: Int): Long {
        val a = (from ushr shift) and 0xFFL
        val b = (to ushr shift) and 0xFFL
        return (a + ((b - a) * k).toLong()).coerceIn(0L, 255L)
    }
    return (from and 0xFF000000L) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
