package io.github.muntasimulhaque.puzzlet.core

/**
 * The piece counts. Five of them, the sizes a real jigsaw comes in for a
 * small child: 4, then 6, then 9, then 12, then 16. A parent picks one on
 * the shelf; the ladder below only decides where a picture opens before
 * anybody has chosen (D-047). Sixteen is the ceiling: past it the pieces
 * stop being chunks a three-year-old can grab and start being confetti.
 */
data class LadderStep(val pieces: Int, val rows: Int, val cols: Int)

val STEPS: List<LadderStep> = listOf(
    LadderStep(4, 2, 2),
    LadderStep(6, 3, 2),
    LadderStep(9, 3, 3),
    LadderStep(12, 4, 3),
    LadderStep(16, 4, 4),
)

/** The counts the shelf shows, smallest first. */
val PIECE_COUNTS: List<Int> = STEPS.map { it.pieces }

/** The gentle walk: a first picture opens at 4, a win deals 6, another 9. */
private val AUTO = STEPS.take(3)

/** Wins so far choose the step, until a parent chooses for themselves. */
fun stepFor(wins: Int): LadderStep = AUTO[wins.coerceIn(0, AUTO.size - 1)]

/** The step for a count the shelf offers; unknown counts fall back to 4. */
fun stepForPieces(pieces: Int): LadderStep =
    STEPS.firstOrNull { it.pieces == pieces } ?: STEPS.first()
