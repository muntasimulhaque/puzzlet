package io.github.muntasimulhaque.puzzlet.core

import org.junit.Test

class ScratchTest {
    @Test
    fun printScales() {
        val fields = listOf(
            "phone" to Area(0.0, 0.0, 1080.0, 1752.0),
            "square" to Area(0.0, 0.0, 800.0, 800.0),
            "short" to Area(0.0, 0.0, 1200.0, 700.0),
            "tablet" to Area(0.0, 0.0, 2560.0, 1624.0),
        )
        for ((name, field) in fields) {
            for ((rows, cols) in listOf(2 to 2, 3 to 2, 3 to 3, 4 to 3, 4 to 4, 6 to 4)) {
                val p = createPuzzle("sail", rows, cols, field, 1470.0, 7L)
                val cellW = p.pieces.maxOf { it.size.x }
                val cellH = p.pieces.maxOf { it.size.y }
                val byRow = p.seats.groupBy { (it.y * 100).toLong() }.toSortedMap()
                val shape = byRow.values.joinToString(" | ") { r ->
                    r.map { (it.x / 10).toInt() }.sorted().joinToString(",")
                }
                println(
                    "$name ${rows}x$cols n=${rows * cols} s=%.3f cell=%.0fx%.0f tray=%.0fx%.0f rows=${byRow.size} x/10: $shape".format(
                        p.trayScale, cellW, cellH, p.tray.w, p.tray.h,
                    ),
                )
            }
        }
    }
}
