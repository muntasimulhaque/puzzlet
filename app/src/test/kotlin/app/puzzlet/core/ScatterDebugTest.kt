package app.puzzlet.core

import org.junit.Test

class ScatterDebugTest {
    @Test
    fun debug() {
        // Mirrors the failing configuration: 4x3 on 900x1400, board 520.
        val p = createPuzzle("sail", 4, 3, Area(0.0, 0.0, 900.0, 1400.0), 520.0, 2L)
        println("tol=" + p.snapTolerance)
        for (piece in p.pieces) {
            println(
                "piece " + piece.id + " home=" + piece.homeCenter + " scat=" + piece.currentCenter +
                    " d=" + dist(piece.currentCenter, piece.homeCenter),
            )
        }
        val again = createPuzzle("sail", 4, 3, Area(0.0, 0.0, 900.0, 1400.0), 520.0, 2L)
        println("determinism=" + (again.pieces.map { it.current } == p.pieces.map { it.current }))
    }
}
