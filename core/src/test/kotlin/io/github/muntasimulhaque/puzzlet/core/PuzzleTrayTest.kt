package io.github.muntasimulhaque.puzzlet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tray's own contracts (D-037): a waiting piece's scaled body sits
 * wholly inside the tray, pieces never overlap each other, the tray scale
 * stays grabbable, the seating is deterministic, and nothing ever drags a
 * waiting piece off its seat. Run across every difficulty and the screen
 * shapes the store ships on, because the tray must work everywhere.
 */
class PuzzleTrayTest {

    private val difficulties = listOf(2 to 2, 3 to 2, 3 to 3, 4 to 3, 4 to 4, 5 to 4, 6 to 4)
    private val fields = listOf(
        Area(0.0, 0.0, 1080.0, 1752.0),  // phone
        Area(0.0, 0.0, 800.0, 800.0),    // square
        Area(0.0, 0.0, 1200.0, 700.0),   // short landscape
        Area(0.0, 0.0, 2560.0, 1624.0),  // 10-inch tablet
    )
    private val cap = 1470.0

    /** The piece as the child actually sees it in the tray: seat plus scaled half-size. */
    private fun visual(piece: Piece, scale: Double): Area {
        val seat = piece.currentCenter
        val half = piece.size * (scale / 2.0)
        return Area(seat.x - half.x, seat.y - half.y, half.x * 2.0, half.y * 2.0)
    }

    @Test
    fun `every waiting piece sits wholly inside the tray, on every screen shape`() {
        for (seed in 1L..10L) for ((rows, cols) in difficulties) for (field in fields) {
            val p = createPuzzle("sail", rows, cols, field, cap, seed)
            val eps = 1e-6
            for (piece in p.pieces) {
                val v = visual(piece, p.trayScale)
                assertTrue(
                    "seed=$seed ${rows}x$cols f=${field.w.toInt()}x${field.h.toInt()} piece ${piece.id} pokes out",
                    v.x >= p.tray.x - eps && v.y >= p.tray.y - eps &&
                        v.maxX <= p.tray.maxX + eps && v.maxY <= p.tray.maxY + eps,
                )
            }
        }
    }

    @Test
    fun `no two tray pieces overlap, on any screen shape`() {
        for (seed in 1L..10L) for ((rows, cols) in difficulties) for (field in fields) {
            val p = createPuzzle("rocket", rows, cols, field, cap, seed)
            val bodies = p.pieces.map { visual(it, p.trayScale) }
            for (i in bodies.indices) for (j in i + 1 until bodies.size) {
                val a = bodies[i]
                val b = bodies[j]
                val w = minOf(a.maxX, b.maxX) - maxOf(a.x, b.x)
                val h = minOf(a.maxY, b.maxY) - maxOf(a.y, b.y)
                assertTrue(
                    "seed=$seed ${rows}x$cols pieces $i and $j overlap",
                    w <= 1e-6 || h <= 1e-6,
                )
            }
        }
    }

    @Test
    fun `clamping never moves a waiting piece off its seat`() {
        for (seed in 1L..10L) for ((rows, cols) in difficulties) for (field in fields) {
            val p = createPuzzle("sail", rows, cols, field, cap, seed)
            for (piece in p.pieces) {
                assertTrue(
                    "seed=$seed ${rows}x$cols piece ${piece.id} off its seat",
                    dist(piece.currentCenter, p.seats[piece.id]) < 1e-6,
                )
            }
            val r = restart(p)
            for (piece in r.pieces) {
                assertTrue(
                    "restart seed=$seed ${rows}x$cols piece ${piece.id} off its seat",
                    dist(piece.currentCenter, p.seats[piece.id]) < 1e-6,
                )
            }
        }
    }

    @Test
    fun `the tray scale stays in a grabbable range`() {
        for ((rows, cols) in difficulties) for (field in fields) {
            val p = createPuzzle("house", rows, cols, field, cap, 3L)
            assertTrue(
                "scale ${p.trayScale} out of range at ${rows}x$cols",
                p.trayScale in 0.24..1.0,
            )
        }
    }

    @Test
    fun `tray seats never start a piece beside its own slot, structurally`() {
        for (seed in 1L..20L) for ((rows, cols) in difficulties) for (field in fields) {
            val p = createPuzzle("sail", rows, cols, field, cap, seed)
            for (piece in p.pieces) {
                val d = dist(piece.currentCenter, piece.homeCenter)
                assertTrue(
                    "seed=$seed ${rows}x$cols piece ${piece.id} starts ${d.toInt()}px from home (tol ${p.snapTolerance.toInt()})",
                    d > p.snapTolerance,
                )
            }
        }
    }

    @Test
    fun `same seed seats identically, a new seed shuffles the shelf`() {
        val a = createPuzzle("rocket", 5, 4, fields[0], cap, 11L)
        val b = createPuzzle("rocket", 5, 4, fields[0], cap, 11L)
        assertEquals(a.seats, b.seats)
        val c = createPuzzle("rocket", 5, 4, fields[0], cap, 12L)
        assertTrue("a new seed should shuffle the shelf", a.seats != c.seats)
    }

    @Test
    fun `the tray never opens in serial`() {
        for (seed in 1L..40L) for ((rows, cols) in difficulties) {
            val n = rows * cols
            val order = shuffledTrayOrder(n, seed)
            val sorted = List(n) { it }
            assertTrue(
                "seed=$seed ${rows}x$cols opens serial",
                order != sorted,
            )
            assertTrue(
                "seed=$seed ${rows}x$cols leaves a piece in place",
                order.indices.none { order[it] == it },
            )
        }
    }

    @Test
    fun `same cut with new seats keeps shapes but jumbles the shelf`() {
        val field = fields[0]
        val a = createPuzzle("sail", 4, 3, field, cap, 7L, 21L)
        val b = createPuzzle("sail", 4, 3, field, cap, 7L, 22L)
        assertEquals(a.pieces.map { it.shape.size }, b.pieces.map { it.shape.size })
        assertEquals(a.pieces.map { it.home }, b.pieces.map { it.home })
        assertTrue("new seats should jumble", a.seats != b.seats)
        val again = createPuzzle("sail", 4, 3, field, cap, 7L, 21L)
        assertEquals(a.seats, again.seats)
    }

    @Test
    fun `redeal keeps the cut, jumbles seats, and clears progress`() {
        var p = createPuzzle("house", 3, 3, fields[1], cap, 5L, 31L)
        p = drag(p, 0, p.piece(0)!!.home)
        p = drop(p, 0)
        assertTrue(p.piece(0)!!.placed)
        val dealt = redeal(p, 32L)
        assertEquals(0, dealt.placedCount)
        assertTrue(dealt.pieces.none { it.placed })
        assertEquals(p.pieces.map { it.shape.size }, dealt.pieces.map { it.shape.size })
        assertEquals(p.pieces.map { it.home }, dealt.pieces.map { it.home })
        assertTrue("redeal should jumble", dealt.seats != p.seats)
        for (piece in dealt.pieces) {
            assertTrue(
                "redealt piece off seat",
                dist(piece.currentCenter, dealt.seats[piece.id]) < 1e-6,
            )
        }
    }

    @Test
    fun `pieceAt is nearest-within-reach and honours the scale hint`() {
        val p = createPuzzle("sail", 2, 2, Area(0.0, 0.0, 800.0, 800.0), 600.0, 9L)
        val a = p.piece(0)!!
        // Dead centre: even a zero reach grabs it.
        assertEquals(0, pieceAt(p, a.currentCenter, 1.0, 0.0)?.id)
        // Off-centre beyond a hinted reach: no grab.
        val offset = a.currentCenter + Vec2(a.halfDiagonal * 0.9, 0.0)
        assertNull(pieceAt(p, offset, 1.0, 0.0))
        // At full scale the piece's own half-diagonal is the reach.
        assertNotNull(pieceAt(p, offset, 1.0))
    }
}
