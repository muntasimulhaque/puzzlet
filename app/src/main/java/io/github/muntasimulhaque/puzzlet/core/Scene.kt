package io.github.muntasimulhaque.puzzlet.core

/**
 * Scene content: what the pictures are made of, as pure data. A scene is a
 * list of shapes in a unit square; the renderer scales it to any board size,
 * so one definition stays crisp from a small phone to a large tablet.
 *
 * Content policy lives here and nowhere else (AGENTS.md, hard constraints):
 * only inanimate subjects, no faces, no eyes on objects. Scene palettes are
 * content constants; UI chrome colors stay in the Compose theme.
 */
sealed interface SceneShape {
    /** sRGB ARGB, as a Long literal (0xFFrrggbb). */
    val argb: Long
}

data class CircleSpec(val center: Vec2, val radius: Double, override val argb: Long) : SceneShape

/** A true ellipse via two radii; [angleDeg] rotates it around its center. */
data class EllipseSpec(
    val center: Vec2,
    val rx: Double,
    val ry: Double,
    override val argb: Long,
    val angleDeg: Double = 0.0,
) : SceneShape

data class RoundRectSpec(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val cornerRadius: Double,
    override val argb: Long,
    val angleDeg: Double = 0.0,
) : SceneShape

data class PolygonSpec(val points: List<Vec2>, override val argb: Long) : SceneShape

/** A flat ring (a donut seen face-on), for planet rings and picture rings. */
data class RingSpec(
    val center: Vec2,
    val rx: Double,
    val ry: Double,
    val thickness: Double,
    override val argb: Long,
    val angleDeg: Double = 0.0,
) : SceneShape

data class SceneSpec(
    val id: String,
    val shapes: List<SceneShape>,
)

object Scenes {

    val all: List<SceneSpec> = listOf(sail(), rocket(), house(), lighthouse(), balloon(), train(), castle(), fruit()).map { it.withClues() }
    fun byId(id: String): SceneSpec = all.first { it.id == id }
}
