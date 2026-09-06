package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private fun Cubic.reversed() = Cubic(p1, c2, c1, p0)

private fun Cubic.shifted(dx: Double, dy: Double) =
    Cubic(p0 + Vec2(dx, dy), c1 + Vec2(dx, dy), c2 + Vec2(dx, dy), p1 + Vec2(dx, dy))

private fun flatLine(a: Vec2, b: Vec2): Cubic = Cubic(
    p0 = a,
    c1 = a + (b - a) * (1.0 / 3.0),
    c2 = a + (b - a) * (2.0 / 3.0),
    p1 = b,
)

/**
 * The jigsaw cut: turns a rows-by-columns board into piece outlines.
 *
 * Every interior edge is generated once and shared by its two pieces: the
 * right neighbour sees the same curve reversed. That construction makes the
 * pieces complementary by design, and the test suite proves it per edge
 * rather than trusting it.
 *
 * Edge shape: a base line, then a two-cubic mushroom knob whose neck is
 * narrower than its head. Position and size are jittered from the seed, so
 * the same puzzle always cuts the same way (AGENTS.md, The game) while
 * different seeds feel hand-cut.
 */
data class Cubic(val p0: Vec2, val c1: Vec2, val c2: Vec2, val p1: Vec2)

/** One piece's outline in piece-local coordinates; origin at the bbox corner. */
data class PieceShape(
    val segments: List<Cubic>,
    val size: Vec2,
    /** Where the bbox corner sits relative to the cell's top-left corner. */
    val offsetInCell: Vec2,
)

object PieceCut {

    /** Proportions of the knob, as fractions of the knob height. */
    private const val NECK_HALF = 0.34
    private const val HEAD_REACH = 0.95
    private const val KNOB_JITTER = 0.24
    private const val MID_JITTER = 0.05

    data class Cut(
        val shapes: List<PieceShape>,
        val cellW: Double,
        val cellH: Double,
        val knobH: Double,
    )

    fun generate(rows: Int, cols: Int, boardW: Double, boardH: Double, seed: Long): Cut {
        require(rows >= 2 && cols >= 2) { "A jigsaw needs at least a 2x2 cut" }
        require(boardW > 0 && boardH > 0) { "Board must have positive size" }
        val rnd = Random(seed)
        val cellW = boardW / cols
        val cellH = boardH / rows
        // One absolute knob height for the whole board: every knob reads at
        // the same physical scale, whatever the cell aspect ratio.
        val knobH = 0.30 * min(cellW, cellH)

        // Interior edges, generated once each, stored RELATIVE to their start
        // corner: hEdges[r][c] spans (0..cellW, 0) for the line between rows
        // r-1 and r; vEdges[r][c] spans (0, 0..cellH) between cols c-1 and c.
        // Assembly shifts each side onto its own cell corner.
        val hEdges: Array<Array<List<Cubic>?>> = Array(rows + 1) { arrayOfNulls(cols) }
        val vEdges: Array<Array<List<Cubic>?>> = Array(rows) { arrayOfNulls(cols + 1) }
        for (r in 1 until rows) for (c in 0 until cols) {
            hEdges[r][c] = hEdge(0.0, cellW, 0.0, knobH, rnd)
        }
        for (r in 0 until rows) for (c in 1 until cols) {
            vEdges[r][c] = vEdge(0.0, cellH, 0.0, knobH, rnd)
        }
        val flatH = listOf(flatLine(Vec2(0.0, 0.0), Vec2(cellW, 0.0)))
        val flatV = listOf(flatLine(Vec2(0.0, 0.0), Vec2(0.0, cellH)))

        val shapes = ArrayList<PieceShape>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            val x0 = c * cellW
            val y0 = r * cellH
            val chain = ArrayList<Cubic>(24)
            // Top, left to right.
            val topEdge: List<Cubic> = hEdges[r][c] ?: flatH
            for (seg in topEdge) chain.add(seg.shifted(x0, y0))
            // Right, top to bottom.
            val rightEdge: List<Cubic> = vEdges[r][c + 1] ?: flatV
            for (seg in rightEdge) chain.add(seg.shifted(x0 + cellW, y0))
            // Bottom, right to left.
            val bottomEdge: List<Cubic> = hEdges[r + 1][c] ?: flatH
            for (seg in bottomEdge.reversed()) chain.add(seg.reversed().shifted(x0, y0 + cellH))
            // Left, bottom to top.
            val leftEdge: List<Cubic> = vEdges[r][c] ?: flatV
            for (seg in leftEdge.reversed()) chain.add(seg.reversed().shifted(x0, y0))

            val closed = closeAndCheck(chain)
            val bounds = boundsOf(closed)
            val local = closed.map { it.shifted(-bounds.minX, -bounds.minY) }
            shapes.add(
                PieceShape(
                    segments = local,
                    size = Vec2(bounds.w, bounds.h),
                    offsetInCell = Vec2(bounds.minX - x0, bounds.minY - y0),
                )
            )
        }
        return Cut(shapes, cellW, cellH, knobH)
    }

    /** Horizontal edge from (x0, y) to (x1, y); sign +1 bumps toward -y. */
    private fun hEdge(x0: Double, x1: Double, y: Double, knobH: Double, rnd: Random): List<Cubic> {
        val len = x1 - x0
        val sign = if (rnd.nextBoolean()) 1.0 else -1.0
        val kh = knobH * (1.0 + (rnd.nextDouble() - 0.5) * KNOB_JITTER)
        val mid = (x0 + (0.5 + (rnd.nextDouble() - 0.5) * 2 * MID_JITTER) * len)
            .coerceIn(x0 + 0.24 * len, x1 - 0.24 * len)
        val bases = listOf(
            flatLine(Vec2(x0, y), Vec2(mid - NECK_HALF * kh, y)),
            flatLine(Vec2(mid + NECK_HALF * kh, y), Vec2(x1, y)),
        )
        return listOf(bases[0]) + mushroom(mid, y, kh, sign, horizontal = true) + listOf(bases[1])
    }

    /** Vertical edge from (x, y0) to (x, y1); sign +1 bumps toward -x. */
    private fun vEdge(y0: Double, y1: Double, x: Double, knobH: Double, rnd: Random): List<Cubic> {
        val len = y1 - y0
        val sign = if (rnd.nextBoolean()) 1.0 else -1.0
        val kh = knobH * (1.0 + (rnd.nextDouble() - 0.5) * KNOB_JITTER)
        val mid = (y0 + (0.5 + (rnd.nextDouble() - 0.5) * 2 * MID_JITTER) * len)
            .coerceIn(y0 + 0.24 * len, y1 - 0.24 * len)
        val bases = listOf(
            flatLine(Vec2(x, y0), Vec2(x, mid - NECK_HALF * kh)),
            flatLine(Vec2(x, mid + NECK_HALF * kh), Vec2(x, y1)),
        )
        return listOf(bases[0]) + mushroom(mid, x, kh, sign, horizontal = false) + listOf(bases[1])
    }

    /**
     * The knob: two cubics from neck-left to neck-right through the head.
     * In "along/across" coordinates across = the bump direction; sign +1
     * bumps toward negative across. The head controls reach [HEAD_REACH] x
     * kh beyond the edge, well past the neck, which is what makes the
     * silhouette a mushroom and not a bump.
     */
    private fun mushroom(mid: Double, base: Double, kh: Double, sign: Double, horizontal: Boolean): List<Cubic> {
        val neck = NECK_HALF * kh
        fun pt(along: Double, across: Double): Vec2 =
            if (horizontal) Vec2(along, base + across * sign) else Vec2(base + across * sign, along)

        val neckL = pt(mid - neck, 0.0)
        val top = pt(mid, -kh)
        val neckR = pt(mid + neck, 0.0)
        val left = Cubic(
            p0 = neckL,
            c1 = pt(mid - neck + 0.10 * kh, -0.35 * kh),
            c2 = pt(mid - HEAD_REACH * kh, -0.45 * kh),
            p1 = top,
        )
        val right = Cubic(
            p0 = top,
            c1 = pt(mid + HEAD_REACH * kh, -0.45 * kh),
            c2 = pt(mid + neck - 0.10 * kh, -0.35 * kh),
            p1 = neckR,
        )
        return listOf(left, right)
    }

    private fun flatLine(a: Vec2, b: Vec2): Cubic = Cubic(
        p0 = a,
        c1 = a + (b - a) * (1.0 / 3.0),
        c2 = a + (b - a) * (2.0 / 3.0),
        p1 = b,
    )

    fun reversed(edge: Cubic) = edge.reversed()

    /**
     * Chain the per-edge runs into one closed outline: joints must meet, and
     * the last point must return to the first. A failure here is a generator
     * bug, so it is a require, not a silent accept.
     */
    private fun closeAndCheck(chain: List<Cubic>): List<Cubic> {
        val joined = ArrayList<Cubic>(chain.size)
        var expectedStart = chain.first().p0
        for (seg in chain) {
            if (joined.isEmpty()) {
                joined.add(seg)
            } else {
                require(dist(seg.p0, expectedStart) < 1e-6) { "Broken joint in piece outline" }
                joined.add(seg)
            }
            expectedStart = seg.p1
        }
        require(dist(expectedStart, joined.first().p0) < 1e-6) { "Piece outline does not close" }
        return joined
    }

    private fun boundsOf(segments: List<Cubic>): Area {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        fun take(p: Vec2) {
            minX = min(minX, p.x); maxX = max(maxX, p.x)
            minY = min(minY, p.y); maxY = max(maxY, p.y)
        }
        segments.forEach { take(it.p0); take(it.c1); take(it.c2); take(it.p1) }
        return Area(minX, minY, maxX - minX, maxY - minY)
    }
}
