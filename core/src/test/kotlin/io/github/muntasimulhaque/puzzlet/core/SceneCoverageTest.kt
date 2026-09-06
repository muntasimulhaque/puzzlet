package io.github.muntasimulhaque.puzzlet.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The no-blank-piece law, checked the way the game cuts (AGENTS.md, D-050).
 *
 * For every shipped picture at every count the shelf offers, each piece is
 * sampled across its own outline and the colours are counted. A piece whose
 * most common colour covers more than [FLAT_LIMIT] of it is a flat piece:
 * nothing to recognise, nothing to think with. Big empty skies are the
 * usual culprit, which is why every picture carries a graded ground and
 * small inanimate texture instead of one flat wash.
 */
class SceneCoverageTest {

    private val samplesPerSide = 9

    /** The most a single colour may cover of any one piece. */
    private val flatLimit = 0.82

    @Test
    fun `no piece of any picture is one flat colour, at any count`() {
        val flat = ArrayList<String>()
        for (scene in Scenes.all) {
            for (step in STEPS) {
                val cut = PieceCut.generate(
                    step.rows, step.cols, 1.0, 1.0, cutSeedFor(scene.id, step.rows, step.cols),
                )
                for (row in 0 until step.rows) {
                    for (col in 0 until step.cols) {
                        val index = row * step.cols + col
                        val shape = cut.shapes[index]
                        val origin = Vec2(col * cut.cellW, row * cut.cellH) + shape.offsetInCell
                        val share = flatShare(scene, shape, origin)
                        if (share > flatLimit) {
                            flat += "${scene.id} ${step.pieces}: piece $index ${(share * 100).toInt()}%"
                        }
                    }
                }
            }
        }
        assertTrue(
            "flat pieces (${flat.size}): " + flat.joinToString(", "),
            flat.isEmpty(),
        )
    }

    /** How much of this piece is covered by its own most common colour. */
    private fun flatShare(scene: SceneSpec, shape: PieceShape, origin: Vec2): Double {
        val poly = outlinePoints(shape).map { it + origin }
        val counts = HashMap<Long, Int>()
        var inside = 0
        for (iy in 0 until samplesPerSide) {
            for (ix in 0 until samplesPerSide) {
                val x = origin.x + shape.size.x * (ix + 0.5) / samplesPerSide
                val y = origin.y + shape.size.y * (iy + 0.5) / samplesPerSide
                if (!polygonContains(poly, x, y)) continue
                inside++
                val argb = colourAt(scene, x, y) ?: continue
                counts[argb] = (counts[argb] ?: 0) + 1
            }
        }
        if (inside == 0) return 1.0
        return (counts.values.maxOrNull() ?: 0).toDouble() / inside.toDouble()
    }
}
