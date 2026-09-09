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
 * The edge is a real die-cut edge, measured, not invented (D-074, and the
 * knob's one home is the mark's own profile): gently bowed base lines into
 * a shoulder flare, a concave taper to a narrow neck, then a round head
 * that overhangs the neck. Position, size, lean and bow are jittered from
 * the seed, so the same puzzle always cuts the same way while different
 * seeds feel hand-cut.
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

    /** The knob's rise as a share of the smaller cell side. */
    private const val KNOB_FRAC = 0.28
    /** How much a knob may differ in size from the board's one knob height. */
    private const val KNOB_JITTER = 0.20
    /** How far the knob may sit off the middle of its edge. */
    private const val MID_JITTER = 0.06
    /** How far the head may lean off the neck, in knob heights. */
    private const val LEAN = 0.10
    /** How much a knob may be slimmer or chunkier than the profile. */
    private const val WIDTH_JITTER = 0.06
    /** The gentle bow of a base line, as a share of its own run. */
    private const val BOW_FRAC = 0.012
    /** The shortest base line allowed between a corner and a knob. */
    private const val MIN_BASE = 0.10
    /** The half-width of the knob's footprint, in knob heights. */
    private const val JOINT_HALF = 0.706

    /**
     * The joint: one real die-cut knob in knob-height units, across
     * positive outward, symmetric about the head. The numbers are the
     * mark's own profile (tools/MarkPiece.kt, traced from real die-cut
     * pieces in D-074), with the base line flattened: the shoulders
     * flare to 1.10 wide, a concave taper falls to the neck (0.53 at
     * 0.36), the round head swells to 1.05 at 0.65 and domes closed.
     * Six cubics, footprint -0.706..0.706.
     */
    internal val JOINT: List<Cubic> = listOf(
        Cubic(Vec2(-0.706, 0.000), Vec2(-0.504, 0.025), Vec2(-0.303, 0.151), Vec2(-0.261, 0.370)),
        Cubic(Vec2(-0.261, 0.370), Vec2(-0.345, 0.496), Vec2(-0.479, 0.529), Vec2(-0.529, 0.664)),
        Cubic(Vec2(-0.529, 0.664), Vec2(-0.429, 0.950), Vec2(-0.218, 1.000), Vec2(0.000, 1.000)),
        Cubic(Vec2(0.000, 1.000), Vec2(0.218, 1.000), Vec2(0.429, 0.950), Vec2(0.529, 0.664)),
        Cubic(Vec2(0.529, 0.664), Vec2(0.479, 0.529), Vec2(0.345, 0.496), Vec2(0.261, 0.370)),
        Cubic(Vec2(0.261, 0.370), Vec2(0.303, 0.151), Vec2(0.504, 0.025), Vec2(0.706, 0.000)),
    )

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
        val knobH = KNOB_FRAC * min(cellW, cellH)

        // Interior edges, generated once each, stored RELATIVE to their start
        // corner: hEdges[r][c] spans (0..cellW, 0) for the line between rows
        // r-1 and r; vEdges[r][c] spans (0, 0..cellH) between cols c-1 and c.
        // Assembly shifts each side onto its own cell corner. Outer edges
        // stay flat: the picture's border is straight, as a bought puzzle's.
        val flatH = listOf(flatLine(Vec2(0.0, 0.0), Vec2(cellW, 0.0)))
        val flatV = listOf(flatLine(Vec2(0.0, 0.0), Vec2(0.0, cellH)))
        val hEdges: Array<Array<List<Cubic>>> = Array(rows + 1) { Array(cols) { flatH } }
        val vEdges: Array<Array<List<Cubic>>> = Array(rows) { Array(cols + 1) { flatV } }
        for (r in 1 until rows) for (c in 0 until cols) {
            hEdges[r][c] = hEdge(0.0, cellW, 0.0, knobH, rnd)
        }
        for (r in 0 until rows) for (c in 1 until cols) {
            vEdges[r][c] = vEdge(0.0, cellH, 0.0, knobH, rnd)
        }

        val shapes = ArrayList<PieceShape>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            shapes.add(shapeAt(r, c, cellW, cellH, hEdges, vEdges))
        }
        return Cut(shapes, cellW, cellH, knobH)
    }

    /** One piece, chained from its four shared edges and moved to its bbox corner. */
    private fun shapeAt(
        r: Int,
        c: Int,
        cellW: Double,
        cellH: Double,
        hEdges: Array<Array<List<Cubic>>>,
        vEdges: Array<Array<List<Cubic>>>,
    ): PieceShape {
        val x0 = c * cellW
        val y0 = r * cellH
        val chain = ArrayList<Cubic>(32)
        // Top, left to right.
        for (seg in hEdges[r][c]) chain.add(seg.shifted(x0, y0))
        // Right, top to bottom.
        for (seg in vEdges[r][c + 1]) chain.add(seg.shifted(x0 + cellW, y0))
        // Bottom, right to left.
        for (seg in hEdges[r + 1][c].reversed()) chain.add(seg.reversed().shifted(x0, y0 + cellH))
        // Left, bottom to top.
        for (seg in vEdges[r][c].reversed()) chain.add(seg.reversed().shifted(x0, y0))

        val closed = closeAndCheck(chain)
        val bounds = boundsOf(closed)
        val local = closed.map { it.shifted(-bounds.minX, -bounds.minY) }
        return PieceShape(
            segments = local,
            size = Vec2(bounds.w, bounds.h),
            offsetInCell = Vec2(bounds.minX - x0, bounds.minY - y0),
        )
    }

    /** Horizontal edge from (x0, y) to (x1, y); a tab bumps toward -y. */
    private fun hEdge(x0: Double, x1: Double, y: Double, knobH: Double, rnd: Random): List<Cubic> =
        edge(x1 - x0, knobH, rnd) { along, across -> Vec2(x0 + along, y - across) }

    /** Vertical edge from (x, y0) to (x, y1); a tab bumps toward -x. */
    private fun vEdge(y0: Double, y1: Double, x: Double, knobH: Double, rnd: Random): List<Cubic> =
        edge(y1 - y0, knobH, rnd) { along, across -> Vec2(x - across, y0 + along) }

    /**
     * One interior edge, built in edge-local coordinates: [along] runs from
     * the start corner, [across] is positive outward. Two bowed base lines
     * carry the shared knob, whose sign, height, width, lean, seat and bows
     * all come from the seed, so both neighbours see one identical curve.
     */
    private fun edge(len: Double, knobH: Double, rnd: Random, map: (Double, Double) -> Vec2): List<Cubic> {
        val sign = if (rnd.nextBoolean()) 1.0 else -1.0
        val kh = knobH * (1.0 + (rnd.nextDouble() - 0.5) * KNOB_JITTER)
        val width = 1.0 + (rnd.nextDouble() - 0.5) * WIDTH_JITTER
        val lean = (rnd.nextDouble() - 0.5) * 2.0 * LEAN
        val half = JOINT_HALF * kh * width
        val want = (0.5 + (rnd.nextDouble() - 0.5) * 2.0 * MID_JITTER) * len
        val lo = half + MIN_BASE * len
        val hi = len - half - MIN_BASE * len
        val mid = if (lo <= hi) want.coerceIn(lo, hi) else len / 2.0
        val bowA = (rnd.nextDouble() - 0.5) * 2.0 * BOW_FRAC
        val bowB = (rnd.nextDouble() - 0.5) * 2.0 * BOW_FRAC
        val out = ArrayList<Cubic>(JOINT.size + 2)
        out += bowedBase(0.0, mid - half, bowA, map)
        for (seg in JOINT) {
            out += Cubic(
                p0 = map(along(seg.p0, mid, kh, width, lean), across(seg.p0, kh, sign)),
                c1 = map(along(seg.c1, mid, kh, width, lean), across(seg.c1, kh, sign)),
                c2 = map(along(seg.c2, mid, kh, width, lean), across(seg.c2, kh, sign)),
                p1 = map(along(seg.p1, mid, kh, width, lean), across(seg.p1, kh, sign)),
            )
        }
        out += bowedBase(mid + half, len, bowB, map)
        return out
    }

    /** Map a profile point: [u] along the edge, [v] outward, both in knob heights. */
    private fun along(p: Vec2, mid: Double, kh: Double, width: Double, lean: Double) =
        mid + (p.x + lean * p.y) * kh * width

    private fun across(p: Vec2, kh: Double, sign: Double) = p.y * kh * sign

    /** A base line that bows a hair, so no edge reads as machine straight. */
    private fun bowedBase(a: Double, b: Double, bend: Double, map: (Double, Double) -> Vec2): Cubic {
        val run = b - a
        return Cubic(
            p0 = map(a, 0.0),
            c1 = map(a + run / 3.0, bend * run),
            c2 = map(a + run * 2.0 / 3.0, bend * run),
            p1 = map(b, 0.0),
        )
    }

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
