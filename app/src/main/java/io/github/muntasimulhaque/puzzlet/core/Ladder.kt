package io.github.muntasimulhaque.puzzlet.core

/**
 * The auto ladder: the child never picks a piece count. A new picture
 * opens at 4, a win deals 6, another win deals 9, and 9 is the ceiling.
 * Nine is where the tray still packs every piece large and grabbable;
 * past it the pieces shrink into clutter, which serves nobody.
 */
data class LadderStep(val pieces: Int, val rows: Int, val cols: Int)

val LADDER = listOf(
    LadderStep(4, 2, 2),
    LadderStep(6, 3, 2),
    LadderStep(9, 3, 3),
)

/** Wins so far choose the step; extra wins hold at the ceiling. */
fun stepFor(wins: Int): LadderStep = LADDER[wins.coerceIn(0, LADDER.size - 1)]
