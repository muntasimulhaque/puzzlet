package repro

// Temporary compile experiment; deleted once the puzzle core compiles.
data class V(val x: Double, val y: Double) {
    operator fun plus(o: V) = V(x + o.x, y + o.y)
    operator fun times(k: Double) = V(x * k, y * k)
}

data class Cub(val p0: V, val c1: V, val c2: V, val p1: V)

object Repro {
    fun gen(rows: Int, cols: Int): List<List<Cub>> {
        val cellW = 1.0 / cols
        val hEdges = Array(rows + 1) { arrayOfNulls<List<Cub>>(cols) }
        for (r in 1 until rows) for (c in 0 until cols) {
            hEdges[r][c] = listOf(Cub(V(0.0, 0.0), V(0.1, 0.0), V(0.2, 0.0), V(1.0, 0.0)))
        }
        val flatH = listOf(Cub(V(0.0, 0.0), V(0.3, 0.0), V(0.6, 0.0), V(1.0, 0.0)))
        val shapes = ArrayList<List<Cub>>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            val x0 = c * cellW
            val topEdge: List<Cub> = hEdges[r][c] ?: flatH
            shapes.add(topEdge.map { it.shifted(x0, 0.0) })
        }
        return shapes
    }
}

private fun Cub.shifted(dx: Double, dy: Double) =
    Cub(p0 + V(dx, 0.0), c1 + V(dx, 0.0), c2 + V(dx, 0.0), p1 + V(dx, 0.0))
