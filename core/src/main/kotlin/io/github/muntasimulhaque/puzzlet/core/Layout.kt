package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.abs
import kotlin.math.ln
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
    pieces <= 9 -> 0.36
    pieces <= 12 -> 0.40
    else -> 0.44
}

/** The board side: generous, never into the tray, capped for tablets. */
fun boardSideFor(fieldW: Double, fieldH: Double, trayH: Double, capPx: Double): Double =
    minOf(fieldW * 0.92, (fieldH - trayH) * 0.94, capPx)

/** Where pieces wait: one scale for the whole tray and a seat centre per piece. */
data class TrayPack(val scale: Double, val seats: List<Vec2>)

/** The cell of every seat is the same size, so the gaps read as one grid. */
private const val TRAY_GAP = 0.13
/** Breathing room at the tray's sides and its top and bottom (D-043). */
private const val TRAY_MARGIN_X = 0.045
private const val TRAY_PAD_Y = 0.07
/** A tray piece is never drawn larger than the piece it becomes on the board. */
private const val MAX_TRAY_SCALE = 1.0

/** One arrangement of cells: how many across, how many down, at what scale. */
data class TrayGrid(val cols: Int, val rows: Int, val scale: Double)

/** The one cell of the grid: the largest piece, so nothing can touch. */
private fun cellOf(sizes: List<Vec2>): Vec2 =
    Vec2(sizes.maxOf { it.x }, sizes.maxOf { it.y })

/** The gap between cells, one value for both directions. */
private fun gapOf(sizes: List<Vec2>): Double {
    val cell = cellOf(sizes)
    return TRAY_GAP * maxOf(cell.x, cell.y)
}

/**
 * The arrangement this tray wants, whatever order the pieces wait in. The
 * same sizes always give the same grid, which is what lets the field decide
 * how tall the shelf is before it knows the jumble (and keeps the cut
 * stable across jumbles, AGENTS.md, D-041).
 */
fun trayGridFor(tray: Area, sizes: List<Vec2>): TrayGrid {
    require(sizes.isNotEmpty()) { "A tray needs at least one piece" }
    val cell = cellOf(sizes)
    val gap = gapOf(sizes)
    val usableW = tray.w * (1.0 - 2 * TRAY_MARGIN_X)
    val usableH = tray.h * (1.0 - 2 * TRAY_PAD_Y)
    return bestGrid(sizes.size, cell.x, cell.y, gap, usableW, usableH)
}

/** How much height an arrangement fills at its own scale. */
fun trayGridHeight(grid: TrayGrid, sizes: List<Vec2>): Double {
    val cellH = sizes.maxOf { it.y }
    val gap = gapOf(sizes)
    return grid.rows * cellH * grid.scale + (grid.rows - 1) * gap * grid.scale
}

/**
 * The tray after the pack is known: as tall as the pieces need, with their
 * padding, and never taller than the share the ladder asked for. A snug
 * shelf hands the rest of the field back to the board, so a wide tablet
 * plays a bigger picture instead of a deeper empty strip.
 */
fun snugTrayHeight(fieldH: Double, share: Double, used: Double): Double {
    val wanted = used / (1.0 - 2 * TRAY_PAD_Y)
    return wanted.coerceIn(fieldH * 0.16, share)
}

/**
 * The order pieces wait in: shuffled from the seed and never left in
 * serial (D-041). The tray is a jumble, like a bought puzzle tipped from
 * its box, but it opens on an even grid (D-046): every seat is the same
 * distance from its neighbours, so the shelf reads as a set, not a spill.
 */
fun shuffledTrayOrder(count: Int, seed: Long): List<Int> {
    if (count <= 1) return List(count) { it }
    val order = List(count) { it }.shuffled(Random(seed)).toMutableList()
    for (i in 0 until count) {
        if (order[i] == i) {
            val j = (i + 1) % count
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }
    }
    if (order == List(count) { it }) {
        val first = order.removeAt(0)
        order.add(first)
    }
    return order
}

/**
 * Where the pieces wait: one even grid of cells, each piece centred in its
 * own cell, the same gap across and down (D-046). The cell is the largest
 * piece's box, so no two pieces can ever touch, and the scale is the most
 * generous that fits the chosen arrangement inside the tray.
 */
fun trayPack(tray: Area, sizes: List<Vec2>, seed: Long): TrayPack {
    require(sizes.isNotEmpty()) { "A tray needs at least one piece" }
    val order = shuffledTrayOrder(sizes.size, seed)
    val cellW = sizes.maxOf { it.x }
    val cellH = sizes.maxOf { it.y }
    val gap = gapOf(sizes)
    val grid = trayGridFor(tray, sizes)
    val s = grid.scale
    val stepX = (cellW + gap) * s
    val stepY = (cellH + gap) * s
    val gridW = grid.cols * cellW * s + (grid.cols - 1) * gap * s
    val gridH = grid.rows * cellH * s + (grid.rows - 1) * gap * s
    val x0 = tray.x + (tray.w - gridW) / 2.0
    val y0 = tray.y + (tray.h - gridH) / 2.0
    val seats = MutableList(sizes.size) { Vec2(0.0, 0.0) }
    for ((slot, idx) in order.withIndex()) {
        val col = slot % grid.cols
        val row = slot / grid.cols
        // A short last row stays centred under the full rows above it.
        val inRow = minOf(grid.cols, sizes.size - row * grid.cols)
        val rowShift = (grid.cols - inRow) * stepX / 2.0
        seats[idx] = Vec2(
            x0 + rowShift + col * stepX + cellW * s / 2.0,
            y0 + row * stepY + cellH * s / 2.0,
        )
    }
    return TrayPack(s, seats)
}

/**
 * The arrangement that gives the biggest pieces. Ties go to the grid whose
 * shape is closest to the tray's own, so a wide tray opens wide and a tall
 * tray opens tall instead of collapsing into one column.
 */
private fun bestGrid(
    count: Int,
    cellW: Double,
    cellH: Double,
    gap: Double,
    usableW: Double,
    usableH: Double,
): TrayGrid {
    val wanted = if (usableH > 0.0) usableW / usableH else 1.0
    var best = TrayGrid(1, count, 0.0)
    for (cols in 1..count) {
        val rows = (count + cols - 1) / cols
        val spanW = cols * cellW + (cols - 1) * gap
        val spanH = rows * cellH + (rows - 1) * gap
        val s = minOf(usableW / spanW, usableH / spanH, MAX_TRAY_SCALE)
        val shape = abs(ln((spanW / spanH) / wanted))
        if (s > best.scale + 1e-9) {
            best = TrayGrid(cols, rows, s)
        } else if (s > best.scale - 1e-9) {
            val bestShape = run {
                val bc = best.cols
                val br = best.rows
                val bw = bc * cellW + (bc - 1) * gap
                val bh = br * cellH + (br - 1) * gap
                abs(ln((bw / bh) / wanted))
            }
            if (shape < bestShape) best = TrayGrid(cols, rows, s)
        }
    }
    return best
}
