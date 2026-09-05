package io.github.muntasimulhaque.puzzlet.core

import kotlin.random.Random

/**
 * The play field's two zones: a tray of waiting pieces on top, the picture
 * assembling below. Pieces wait at tray scale and grow to board scale in
 * hand, so the child's question is the same one a real jigsaw asks: which
 * piece, and where does it go. All pure math, no Android imports, so tests
 * and the screenshot harness agree with the app on every number.
 */

/** The tray's vertical share of the field; bigger ladders get a deeper tray. */
fun trayHeightFor(fieldH: Double, pieces: Int): Double = fieldH * when {
    pieces <= 6 -> 0.32
    pieces <= 12 -> 0.36
    else -> 0.38
}

/** The board side: generous, never into the tray, capped for tablets. */
fun boardSideFor(fieldW: Double, fieldH: Double, trayH: Double, capPx: Double): Double =
    minOf(fieldW * 0.92, (fieldH - trayH) * 0.94, capPx)

/** Where pieces wait: one scale for the whole tray and a seat centre per piece. */
data class TrayPack(val scale: Double, val seats: List<Vec2>)

/**
 * Shelf rows of pieces, bottom-aligned like toys on a shelf, each row
 * centred, the whole pack centred in the tray. The piece order is shuffled
 * from the seed (same picture and difficulty, same seating, every time),
 * and the scale is the largest that fits every row inside the tray: a
 * bounded binary search over a greedy pack, so it can never loop forever.
 */
fun trayPack(tray: Area, sizes: List<Vec2>, seed: Long): TrayPack {
    require(sizes.isNotEmpty()) { "A tray needs at least one piece" }
    val order = sizes.indices.shuffled(Random(seed))

    fun gap(s: Double) = 10.0 * s
    fun rowGap(s: Double) = 6.0 * s
    val marginX = tray.w * 0.02
    val vPad = tray.h * 0.10

    // Greedy rows at this scale; null when some single row cannot fit.
    fun rows(s: Double): List<List<Int>>? {
        if (s <= 0.0) return null
        val out = ArrayList<List<Int>>()
        var current = ArrayList<Int>()
        var x = tray.x + marginX
        for (idx in order) {
            val w = sizes[idx].x * s
            if (current.isNotEmpty() && x + w > tray.x + tray.w - marginX) {
                out.add(current)
                current = ArrayList()
                x = tray.x + marginX
            }
            current.add(idx)
            x += w + gap(s)
        }
        if (current.isNotEmpty()) out.add(current)
        for (row in out) {
            val rowW = row.sumOf { sizes[it].x * s } + gap(s) * (row.size - 1)
            if (rowW > tray.w - 2 * marginX) return null
        }
        return out
    }

    fun height(rs: List<List<Int>>, s: Double): Double =
        rs.sumOf { row -> row.maxOf { sizes[it].y } * s } + rowGap(s) * (rs.size - 1)

    val usableH = tray.h - 2 * vPad
    var lo = 0.24
    var hi = 1.0
    val scale = rows(hi)?.let { if (height(it, hi) <= usableH) hi else null } ?: run {
        repeat(20) {
            val mid = (lo + hi) / 2.0
            val rs = rows(mid)
            if (rs != null && height(rs, mid) <= usableH) lo = mid else hi = mid
        }
        lo
    }
    val rs = rows(scale)
    if (rs == null) {
        // A tray too small even at the floor: one piece per row, centred;
        // defensive, and the field clamp catches any residue.
        var y = tray.y + vPad
        val seats = order.map { idx ->
            val c = Vec2(tray.x + tray.w / 2.0, y + sizes[idx].y * 0.24 / 2.0)
            y += sizes[idx].y * 0.24
            c
        }
        return TrayPack(0.24, seats)
    }
    val seats = MutableList(sizes.size) { Vec2(0.0, 0.0) }
    var y = tray.y + vPad
    for (row in rs) {
        val rowH = row.maxOf { sizes[it].y } * scale
        val rowW = row.sumOf { sizes[it].x * scale } + gap(scale) * (row.size - 1)
        var x = tray.x + (tray.w - rowW) / 2.0
        for (idx in row) {
            val s = sizes[idx]
            seats[idx] = Vec2(x + s.x * scale / 2.0, y + rowH - s.y * scale / 2.0)
            x += s.x * scale + gap(scale)
        }
        y += rowH + rowGap(scale)
    }
    return TrayPack(scale, seats)
}
