package app.puzzlet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PuzzleCoreTest {

    private val difficulties = listOf(2 to 2, 3 to 2, 3 to 3, 4 to 3, 4 to 4, 5 to 4, 6 to 4)

    private fun Cubic.pts() = listOf(p0, c1, c2, p1)

    private fun ptsNear(a: List<Vec2>, b: List<Vec2>, eps: Double = 1e-6): Boolean =
        a.zip(b).all { (p, q) -> abs(p.x - q.x) < eps && abs(p.y - q.y) < eps }

    private fun Cubic.near(o: Cubic) = ptsNear(pts(), o.pts())

    /** Piece segments back in absolute board coordinates. */
    private fun absolute(piece: PieceShape, cellX: Double, cellY: Double): List<Cubic> =
        piece.segments.map {
            it.copy(
                p0 = it.p0 + Vec2(cellX + piece.offsetInCell.x, cellY + piece.offsetInCell.y),
                c1 = it.c1 + Vec2(cellX + piece.offsetInCell.x, cellY + piece.offsetInCell.y),
                c2 = it.c2 + Vec2(cellX + piece.offsetInCell.x, cellY + piece.offsetInCell.y),
                p1 = it.p1 + Vec2(cellX + piece.offsetInCell.x, cellY + piece.offsetInCell.y),
            )
        }

    private fun cut(rows: Int, cols: Int, seed: Long = 7L) =
        PieceCut.generate(rows, cols, 600.0, 600.0, seed)

    @Test
    fun `every outline closes, chains cleanly, and stays finite, for every seed and difficulty`() {
        for (seed in 1L..25L) for ((rows, cols) in difficulties) {
            val cut = cut(rows, cols, seed)
            for (shape in cut.shapes) {
                val chain = shape.segments
                assertTrue(chain.size >= 4)
                for (i in 0 until chain.size) {
                    val seg = chain[i]
                    for (p in seg.pts()) {
                        assertTrue("Non-finite point at seed=$seed r$rows c$cols", p.x.isFinite() && p.y.isFinite())
                    }
                    val next = chain[(i + 1) % chain.size]
                    assertTrue("Broken joint at seed=$seed r$rows c$cols", dist(seg.p1, next.p0) < 1e-6)
                }
                assertTrue("Outline does not close at seed=$seed r$rows c$cols", dist(chain.last().p1, chain.first().p0) < 1e-6)
                assertTrue("Empty bbox", shape.size.x > 0 && shape.size.y > 0)
            }
        }
    }

    @Test
    fun `same seed cuts the same board twice, identically`() {
        assertEquals(cut(4, 3), cut(4, 3))
        assertNotEquals(cut(4, 3), cut(4, 3, seed = 8L))
    }

    @Test
    fun `shared edges are the same curve reversed, for both directions`() {
        val rows = 4
        val cols = 3
        val cut = cut(rows, cols)
        fun segCount(isOuter: Boolean) = if (isOuter) 1 else 4

        for (r in 0 until rows) for (c in 0 until cols) {
            val cellX = c * cut.cellW
            val cellY = r * cut.cellH
            fun topN(row: Int) = segCount(row == 0)
            fun rightN(col: Int) = segCount(col == cols - 1)
            fun bottomN(row: Int) = segCount(row == rows - 1)
            fun leftN(col: Int) = segCount(col == 0)

            // The piece below shares this piece's bottom edge with its own top.
            if (r < rows - 1) {
                val below = absolute(cut.shapes[(r + 1) * cols + c], cellX, cellY + cut.cellH)
                val mine = absolute(cut.shapes[r * cols + c], cellX, cellY)
                val myBottom = mine.drop(topN(r) + rightN(c)).take(bottomN(r))
                val itsTop = below.take(topN(r + 1))
                assertEquals(myBottom.size, itsTop.size)
                for (i in myBottom.indices) {
                    val reversedMine = PieceCut.reversed(myBottom[i])
                    assertTrue("h edge r$r c$c seg$i", reversedMine.near(itsTop[myBottom.size - 1 - i]))
                }
            }
            // The piece to the right shares this piece's right edge with its own left.
            if (c < cols - 1) {
                val right = absolute(cut.shapes[r * cols + c + 1], cellX + cut.cellW, cellY)
                val mine = absolute(cut.shapes[r * cols + c], cellX, cellY)
                val myRight = mine.drop(topN(r)).take(rightN(c))
                val itsLeft = right.drop(topN(r) + rightN(c + 1) + bottomN(r)).take(leftN(c + 1))
                assertEquals(myRight.size, itsLeft.size)
                for (i in myRight.indices) {
                    val reversedMine = PieceCut.reversed(myRight[i])
                    assertTrue("v edge r$r c$c seg$i", reversedMine.near(itsLeft[myRight.size - 1 - i]))
                }
            }
        }
    }

    @Test
    fun `knobs rise about one knob height above their base line`() {
        // Piece segments are stored in bbox-local coordinates, so measure the
        // rise of the top edge's knob relative to its own base line.
        val cut = cut(3, 3)
        val topEdge = cut.shapes[4].segments.take(4) // centre piece: base, knob L, knob R, base
        val ys = topEdge.flatMap { it.pts() }.map { it.y }
        val rise = ys.max() - ys.min()
        assertTrue("Knob rise $rise should be near knobH ${cut.knobH}",
            rise in 0.7 * cut.knobH..1.3 * cut.knobH)
    }

    @Test
    fun `drop snaps within tolerance, and the last piece completes the puzzle`() {
        val p0 = createPuzzle("sail", 2, 2, Area(0.0, 0.0, 800.0, 800.0), 600.0, 42L)
        val tol = p0.snapTolerance
        var p = p0
        for (id in 0 until 3) {
            p = drag(p, id, p.piece(id)!!.home + Vec2(tol * 0.4, 0.0))
            p = drop(p, id)
            assertTrue("piece $id should snap", p.piece(id)!!.placed)
            assertFalse(p.completed)
        }
        // Far drop: clamped into the field, still far from home, no snap.
        p = drag(p, 3, p.piece(3)!!.home + Vec2(tol * 3.0, tol * 3.0))
        p = drop(p, 3)
        assertFalse(p.piece(3)!!.placed)
        assertFalse(p.completed)
        // And now it goes home.
        p = drag(p, 3, p.piece(3)!!.home)
        p = drop(p, 3)
        assertTrue(p.piece(3)!!.placed)
        assertTrue(p.completed)
        assertEquals(p0.pieces.size, p.placedCount)
    }

    @Test
    fun `placed pieces are immune and grab lifts to the top`() {
        var p = createPuzzle("sail", 2, 2, Area(0.0, 0.0, 800.0, 800.0), 600.0, 42L)
        p = drag(p, 0, p.piece(0)!!.home)
        p = drop(p, 0)
        val afterDrop = p
        // Grabbing a placed piece changes nothing.
        assertEquals(afterDrop, grab(afterDrop, 0))
        // Dragging a placed piece changes nothing.
        assertEquals(afterDrop, drag(afterDrop, 0, Vec2(10.0, 10.0)))
        // Grabbing an unplaced piece moves it to the end of the list (topmost).
        p = grab(p, 1)
        assertEquals(1, p.pieces.last().id)
        assertEquals(0, p.pieces.first().id)
    }

    @Test
    fun `drag clamps the piece inside the field`() {
        var p = createPuzzle("sail", 2, 2, Area(0.0, 0.0, 800.0, 800.0), 600.0, 42L)
        val size = p.piece(0)!!.size
        p = drag(p, 0, Vec2(-500.0, -500.0))
        val topLeft = p.piece(0)!!.current
        assertTrue(topLeft.x >= 0 - 1e-9 && topLeft.y >= 0 - 1e-9)
        assertTrue(topLeft.x + size.x <= 800.0 + 1e-9 && topLeft.y + size.y <= 800.0 + 1e-9)
    }

    @Test
    fun `scatter stays inside the field and is deterministic`() {
        val p = createPuzzle("rocket", 6, 4, Area(0.0, 0.0, 800.0, 1200.0), 500.0, 11L)
        for (piece in p.pieces) {
            assertTrue(piece.currentCenter.x in 0.0..800.0)
            assertTrue(piece.currentCenter.y in 0.0..1200.0)
        }
        val again = createPuzzle("rocket", 6, 4, Area(0.0, 0.0, 800.0, 1200.0), 500.0, 11L)
        assertEquals(p.pieces.map { it.current }, again.pieces.map { it.current })
    }

    @Test
    fun `relayout keeps placed progress and re-seats placed pieces`() {
        var p = createPuzzle("house", 3, 3, Area(0.0, 0.0, 800.0, 800.0), 500.0, 5L)
        for (id in 0 until 3) {
            p = drag(p, id, p.piece(id)!!.home)
            p = drop(p, id)
        }
        p = relayout(p, Area(0.0, 0.0, 1200.0, 800.0), 480.0)
        assertEquals(3, p.placedCount)
        assertFalse(p.completed)
        for (id in 0 until 3) {
            val piece = p.piece(id)!!
            assertTrue(piece.placed)
            assertEquals(piece.home, piece.current)
        }
        for (piece in p.pieces) {
            assertTrue(piece.home.x >= p.board.x - 1e-9 && piece.home.y >= p.board.y - 1e-9)
            assertTrue(piece.home.x <= p.board.maxX + 1e-9 && piece.home.y <= p.board.maxY + 1e-9)
        }
    }

    @Test
    fun `restart clears progress and rescatters`() {
        var p = createPuzzle("house", 3, 3, Area(0.0, 0.0, 800.0, 800.0), 500.0, 5L)
        p = drag(p, 0, p.piece(0)!!.home)
        p = drop(p, 0)
        p = restart(p)
        assertEquals(0, p.placedCount)
        assertFalse(p.completed)
        assertTrue(p.pieces.none { it.placed })
    }

    @Test
    fun `scatter never seats a piece beside its own slot, when there is room`() {
        for (seed in 1L..20L) for ((rows, cols) in listOf(3 to 3, 4 to 3, 5 to 4, 6 to 4)) {
            val p = createPuzzle("sail", rows, cols, Area(0.0, 0.0, 900.0, 1400.0), 520.0, seed)
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
    fun `restore re-seats saved pieces and keeps the rest on their scatter seats`() {
        val p = restorePuzzle("sail", 3, 3, setOf(0, 4), Area(0.0, 0.0, 800.0, 800.0), 500.0, 3L)
        assertEquals(2, p.placedCount)
        assertFalse(p.completed)
        assertTrue(p.piece(0)!!.placed)
        assertTrue(p.piece(4)!!.placed)
        assertFalse(p.piece(1)!!.placed)
        assertEquals(p.piece(0)!!.home, p.piece(0)!!.current)
        val fresh = createPuzzle("sail", 3, 3, Area(0.0, 0.0, 800.0, 800.0), 500.0, 3L)
        for (piece in p.pieces.filter { !it.placed }) {
            assertEquals(fresh.piece(piece.id)!!.current, piece.current)
        }
        // A full restore completes the picture.
        val all = restorePuzzle("sail", 2, 2, (0 until 4).toSet(), Area(0.0, 0.0, 800.0, 800.0), 600.0, 3L)
        assertTrue(all.completed)
    }

    @Test
    fun `pieceAt picks the topmost unplaced piece and never a placed one`() {
        var p = createPuzzle("sail", 2, 2, Area(0.0, 0.0, 800.0, 800.0), 600.0, 9L)
        p = drag(p, 0, p.piece(0)!!.home)
        p = drop(p, 0)
        p = grab(p, 1)
        val held = p.piece(1)!!
        assertEquals(1, pieceAt(p, held.currentCenter, 10.0)?.id)
        // A placed piece is never grabbed, however large the radius.
        val placed = p.piece(0)!!
        for (radius in listOf(1.0, 100.0, 5000.0)) {
            val hit = pieceAt(p, placed.currentCenter, radius)
            assertTrue("placed piece must not be hit", hit?.id != 0)
        }
    }

    @Test
    fun `scenes are sane, inanimate, and finite`() {
        assertEquals(Scenes.all.size, Scenes.all.map { it.id }.distinct().size)
        for (scene in Scenes.all) {
            assertTrue(scene.shapes.isNotEmpty())
            for (shape in scene.shapes) {
                // Translucency is allowed (light beams, halos); invisibility
                // is not: anything under 25 percent alpha is a mistake.
                assertTrue("Bad alpha in ${scene.id}", shape.argb ushr 24 >= 0x40L)
                val pts: List<Vec2> = when (shape) {
                    is CircleSpec -> listOf(shape.center)
                    is EllipseSpec -> listOf(shape.center)
                    is RoundRectSpec -> listOf(Vec2(shape.x, shape.y))
                    is PolygonSpec -> shape.points
                    is RingSpec -> listOf(shape.center)
                }
                for (p in pts) assertTrue("Non-finite in ${scene.id}", p.x.isFinite() && p.y.isFinite())
                if (shape is CircleSpec) assertTrue(shape.radius > 0)
                if (shape is RingSpec) assertTrue(shape.rx > shape.thickness)
                if (shape is PolygonSpec) assertTrue(shape.points.size >= 3)
                if (shape is EllipseSpec) assertTrue(shape.rx > 0 && shape.ry > 0)
            }
        }
        assertEquals(8, Scenes.all.size)
    }
}
