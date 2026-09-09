package io.github.muntasimulhaque.puzzlet.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The piece's shape against the real world: the joint must read as a real
 * die-cut knob (D-076). The reference numbers are cross-section widths of
 * the mark's own contour (tools/MarkPiece.kt, itself traced from real
 * die-cut pieces in D-074), so the icon and the game stay one family.
 */
class PieceProfileTest {

    private fun Cubic.pts() = listOf(p0, c1, c2, p1)

    @Test
    fun `a tab rises about one knob height above its base line`() {
        // The centre piece of a 3x3 cut has four interior edges, so its top
        // edge is the first JOINT.size + 2 segments of the chain. Signs are
        // seeded, so walk seeds until the top edge is a tab, then measure
        // the rise of the whole edge over its own base line.
        var tabs = 0
        for (seed in 1L..30L) {
            val cut = PieceCut.generate(3, 3, 600.0, 600.0, seed)
            val edge = cut.shapes[4].segments.take(PieceCut.JOINT.size + 2)
            val baseY = edge.first().p0.y
            val rise = baseY - edge.flatMap { it.pts() }.minOf { it.y }
            if (rise > 0.5 * cut.knobH) {
                assertTrue(
                    "rise $rise should be near knobH ${cut.knobH}",
                    rise in 0.7 * cut.knobH..1.3 * cut.knobH,
                )
                tabs++
            }
        }
        assertTrue("no top tab found in 30 seeds", tabs > 0)
    }

    /** The outline's horizontal extent at height [v], in knob heights. */
    private fun profileWidth(profile: List<Cubic>, v: Double): Double {
        fun at(seg: Cubic, t: Double): Vec2 {
            val m = 1.0 - t
            val a = m * m * m
            val b = 3.0 * m * m * t
            val c = 3.0 * m * t * t
            val d = t * t * t
            return Vec2(
                a * seg.p0.x + b * seg.c1.x + c * seg.c2.x + d * seg.p1.x,
                a * seg.p0.y + b * seg.c1.y + c * seg.c2.y + d * seg.p1.y,
            )
        }
        val xs = ArrayList<Double>()
        for (seg in profile) {
            var prevT = 0.0
            var prevY = seg.p0.y - v
            for (i in 1..600) {
                val t = i / 600.0
                val y = at(seg, t).y - v
                if (prevY * y < 0.0) {
                    var lo = prevT
                    var hi = t
                    repeat(50) {
                        val m = (lo + hi) / 2.0
                        if ((at(seg, m).y - v) * prevY <= 0.0) hi = m else lo = m
                    }
                    xs.add(at(seg, (lo + hi) / 2.0).x)
                }
                prevT = t
                prevY = y
            }
        }
        return if (xs.size < 2) 0.0 else xs.max() - xs.min()
    }

    @Test
    fun `the knob is a real die-cut knob, measured`() {
        // Cross-section widths of one joint at heights v in knob heights,
        // measured from the mark's contour. The real signature: shoulders
        // flare, a concave taper narrows to the neck, the round head
        // overhangs the neck, then the dome closes.
        val expected = listOf(
            0.04 to 1.10, 0.12 to 0.83, 0.20 to 0.67, 0.28 to 0.58,
            0.36 to 0.53, 0.44 to 0.64, 0.52 to 0.83, 0.60 to 0.99,
            0.65 to 1.05, 0.73 to 1.00, 0.86 to 0.83, 0.94 to 0.62,
        )
        for ((v, want) in expected) {
            val got = profileWidth(PieceCut.JOINT, v)
            assertTrue("width at v=$v was $got, expected $want", abs(got - want) < 0.02)
        }
        // The signature itself, not just the samples: the neck is the
        // narrowest point and the head overhangs it by nearly twice.
        val shoulder = profileWidth(PieceCut.JOINT, 0.04)
        val neck = profileWidth(PieceCut.JOINT, 0.36)
        val head = profileWidth(PieceCut.JOINT, 0.65)
        assertTrue("neck $neck is not under the shoulder $shoulder", neck < shoulder)
        assertTrue("head $head is not overhanging the neck $neck", head > 1.8 * neck)
    }
}
