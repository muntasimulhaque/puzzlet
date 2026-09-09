package io.github.muntasimulhaque.puzzlet.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The ladder and the one opening-count rule (D-047, D-065): a parent's
 * pick outranks the walk, and where nobody has picked, wins walk the
 * picture through 4, 6, 9. The 4-that-opened-6 bug was a divergence
 * between the chooser's marked tile (read the pick or fall back to 4)
 * and the play path (read the pick or the ladder), so every test here
 * pins that the two read the same rule.
 */
class LadderTest {

    @Test
    fun `a fresh picture opens at four pieces`() {
        assertEquals(4, openingCountFor(emptyMap(), emptyMap(), "truck"))
    }

    @Test
    fun `one win walks a picture to six and another to nine`() {
        val wins = mapOf("truck" to 1)
        assertEquals(6, openingCountFor(emptyMap(), wins, "truck"))
        val two = mapOf("truck" to 2)
        assertEquals(9, openingCountFor(emptyMap(), two, "truck"))
    }

    @Test
    fun `wins stop walking past nine`() {
        assertEquals(9, openingCountFor(emptyMap(), mapOf("truck" to 7), "truck"))
    }

    @Test
    fun `a parent pick outranks the ladder and stays put`() {
        val chosen = mapOf("truck" to 16)
        assertEquals(16, openingCountFor(chosen, mapOf("truck" to 1), "truck"))
        assertEquals(16, openingCountFor(chosen, mapOf("truck" to 9), "truck"))
    }

    @Test
    fun `a pick outside the five counts is ignored, not trusted`() {
        // A stale or corrupt preference (a count the shelf does not offer)
        // must never reach the shelf line or the play path: both read the
        // ladder instead, so the marked tile and the deal stay one truth.
        assertEquals(4, openingCountFor(mapOf("truck" to 7), emptyMap(), "truck"))
        assertEquals(6, openingCountFor(mapOf("truck" to 0), mapOf("truck" to 1), "truck"))
        assertEquals(9, openingCountFor(mapOf("truck" to -3), mapOf("truck" to 2), "truck"))
    }

    @Test
    fun `one picture's win never moves another`() {
        assertEquals(4, openingCountFor(emptyMap(), mapOf("sail" to 2), "truck"))
    }

    @Test
    fun `stepForPieces maps the five counts and falls back to four`() {
        assertEquals(LadderStep(4, 2, 2), stepForPieces(4))
        assertEquals(LadderStep(6, 3, 2), stepForPieces(6))
        assertEquals(LadderStep(9, 3, 3), stepForPieces(9))
        assertEquals(LadderStep(12, 4, 3), stepForPieces(12))
        assertEquals(LadderStep(16, 4, 4), stepForPieces(16))
        assertEquals(LadderStep(4, 2, 2), stepForPieces(23))
        assertEquals(LadderStep(4, 2, 2), stepForPieces(0))
    }
}
