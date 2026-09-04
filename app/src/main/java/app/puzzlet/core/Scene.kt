package app.puzzlet.core

import kotlin.math.cos
import kotlin.math.sin

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

    val all: List<SceneSpec> = listOf(sail(), rocket(), house())
    fun byId(id: String): SceneSpec = all.first { it.id == id }

    // ---------------------------------------------------------------------
    // Sailboat: sun, clouds, scalloped sea, a little boat. Nothing aboard.
    // ---------------------------------------------------------------------
    private fun sail(): SceneSpec {
        val sky = 0xFFA9D9E8L
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val seaDeep = 0xFF3E93B8L
        val seaMid = 0xFF54A9CCL
        val seaLight = 0xFF74BBDDL
        val hull = 0xFFE4572EL
        val sail = 0xFFFEFCF8L
        val mast = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            // Sun with eight rounded rays.
            val sunC = Vec2(0.82, 0.17)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val rayC = Vec2(sunC.x + 0.132 * cos(a), sunC.y + 0.132 * sin(a))
                add(RoundRectSpec(rayC.x - 0.019, rayC.y - 0.009, 0.038, 0.018, 0.009, sun, angleDeg = i * 45.0))
            }
            add(CircleSpec(sunC, 0.085, sun))
            addAll(cloud(Vec2(0.20, 0.17), 1.0, cloud))
            addAll(cloud(Vec2(0.47, 0.30), 0.75, cloud))
            addAll(cloud(Vec2(0.66, 0.09), 0.6, cloud))
            add(RoundRectSpec(0.0, 0.62, 1.0, 0.38, 0.0, seaDeep))
            // Two scalloped wave rows: bumps of lighter blue riding the sea.
            for (x in 0..16) add(CircleSpec(Vec2(0.02 + x * 0.065, 0.62), 0.034, seaMid))
            for (x in 0..13) add(CircleSpec(Vec2(0.04 + x * 0.078, 0.74), 0.030, seaLight))
            // Foam flecks.
            add(CircleSpec(Vec2(0.14, 0.68), 0.011, cloud))
            add(CircleSpec(Vec2(0.78, 0.71), 0.011, cloud))
            add(CircleSpec(Vec2(0.60, 0.86), 0.012, cloud))
            // The boat: hull, mast, two sails, flag.
            add(PolygonSpec(listOf(Vec2(0.38, 0.66), Vec2(0.62, 0.66), Vec2(0.56, 0.77), Vec2(0.44, 0.77)), hull))
            add(RoundRectSpec(0.492, 0.36, 0.016, 0.30, 0.006, mast))
            add(PolygonSpec(listOf(Vec2(0.525, 0.40), Vec2(0.525, 0.645), Vec2(0.70, 0.645)), sail))
            add(PolygonSpec(listOf(Vec2(0.475, 0.45), Vec2(0.475, 0.645), Vec2(0.33, 0.645)), sun))
            add(PolygonSpec(listOf(Vec2(0.50, 0.355), Vec2(0.50, 0.40), Vec2(0.455, 0.378)), hull))
        }
        return SceneSpec("sail", shapes)
    }

    private fun cloud(c: Vec2, s: Double, argb: Long): List<SceneShape> = listOf(
        RoundRectSpec(c.x - 0.10 * s, c.y - 0.012 * s, 0.20 * s, 0.052 * s, 0.026 * s, argb),
        CircleSpec(Vec2(c.x - 0.05 * s, c.y - 0.022 * s), 0.045 * s, argb),
        CircleSpec(Vec2(c.x + 0.03 * s, c.y - 0.030 * s), 0.055 * s, argb),
        CircleSpec(Vec2(c.x + 0.083 * s, c.y - 0.012 * s), 0.036 * s, argb),
    )

    // ---------------------------------------------------------------------
    // Rocket: night sky, sparkles, a cratered moon, a ringed planet, and a
    // rocket with its flame. No crew, no creatures, just machines and sky.
    // ---------------------------------------------------------------------
    private fun rocket(): SceneSpec {
        val night = 0xFF3A4A8CL
        val paper = 0xFFFEFCF8L
        val honey = 0xFFF0B429L
        val moon = 0xFFF2ECDDL
        val crater = 0xFFDDD2BCL
        val coral = 0xFFE4572EL
        val window = 0xFF7FB8D4L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, night))
            // Sparkle stars, four-pointed, plus a few round dots.
            val sparkles = listOf(
                Vec2(0.10, 0.42) to 0.020, Vec2(0.26, 0.12) to 0.014, Vec2(0.36, 0.62) to 0.017,
                Vec2(0.14, 0.78) to 0.022, Vec2(0.62, 0.16) to 0.013, Vec2(0.72, 0.52) to 0.019,
                Vec2(0.88, 0.66) to 0.022, Vec2(0.90, 0.10) to 0.014, Vec2(0.58, 0.90) to 0.016,
                Vec2(0.30, 0.92) to 0.013, Vec2(0.68, 0.74) to 0.012, Vec2(0.94, 0.44) to 0.016,
            )
            for ((p, r) in sparkles) add(PolygonSpec(starPoints(p, r, r * 0.38, 4, 0.0), honey))
            for (p in listOf(Vec2(0.20, 0.60), Vec2(0.44, 0.14), Vec2(0.78, 0.34), Vec2(0.06, 0.22), Vec2(0.84, 0.88), Vec2(0.50, 0.80))) {
                add(CircleSpec(p, 0.007, paper))
            }
            // Moon with craters.
            add(CircleSpec(Vec2(0.20, 0.20), 0.105, moon))
            add(CircleSpec(Vec2(0.163, 0.163), 0.021, crater))
            add(CircleSpec(Vec2(0.238, 0.215), 0.027, crater))
            add(CircleSpec(Vec2(0.185, 0.258), 0.016, crater))
            // Ringed planet.
            add(CircleSpec(Vec2(0.82, 0.28), 0.095, coral))
            add(RingSpec(Vec2(0.82, 0.28), 0.145, 0.046, 0.030, honey, angleDeg = -18.0))
            // Rocket: flame, fins, body, nose, window.
            add(PolygonSpec(listOf(Vec2(0.465, 0.66), Vec2(0.535, 0.66), Vec2(0.50, 0.79)), honey))
            add(PolygonSpec(listOf(Vec2(0.482, 0.66), Vec2(0.518, 0.66), Vec2(0.50, 0.735)), coral))
            add(PolygonSpec(listOf(Vec2(0.44, 0.575), Vec2(0.385, 0.68), Vec2(0.44, 0.66)), coral))
            add(PolygonSpec(listOf(Vec2(0.56, 0.575), Vec2(0.615, 0.68), Vec2(0.56, 0.66)), coral))
            add(RoundRectSpec(0.44, 0.30, 0.12, 0.37, 0.02, paper))
            add(PolygonSpec(listOf(Vec2(0.44, 0.325), Vec2(0.56, 0.325), Vec2(0.50, 0.205)), honey))
            add(CircleSpec(Vec2(0.50, 0.40), 0.047, paper))
            add(CircleSpec(Vec2(0.50, 0.40), 0.033, window))
        }
        return SceneSpec("rocket", shapes)
    }

    // ---------------------------------------------------------------------
    // House: a hill, a cottage with a round window, a tree, flowers.
    // ---------------------------------------------------------------------
    private fun house(): SceneSpec {
        val sky = 0xFFBEE7DFL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillBack = 0xFF68B055L
        val hillFront = 0xFF82C86EL
        val wall = 0xFFFEFCF8L
        val roof = 0xFFE4572EL
        val chimney = 0xFFE3D9C6L
        val door = 0xFF0C7A64L
        val window = 0xFFF0B429L
        val trunk = 0xFFA67B5BL
        val leafA = 0xFF5FA054L
        val leafB = 0xFF6FB863L
        val flowerA = 0xFFE4572EL
        val flowerB = 0xFFF0B429L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            add(CircleSpec(Vec2(0.14, 0.15), 0.080, sun))
            addAll(cloud(Vec2(0.72, 0.13), 0.7, cloud))
            add(EllipseSpec(Vec2(0.18, 1.02), 0.55, 0.42, hillBack))
            add(EllipseSpec(Vec2(0.62, 1.08), 0.72, 0.48, hillFront))
            // Tree behind the hill line.
            add(RoundRectSpec(0.185, 0.60, 0.035, 0.15, 0.01, trunk))
            add(CircleSpec(Vec2(0.202, 0.545), 0.085, leafA))
            add(CircleSpec(Vec2(0.145, 0.588), 0.060, leafB))
            add(CircleSpec(Vec2(0.258, 0.588), 0.058, leafB))
            // Chimney first so the roof covers its foot.
            add(RoundRectSpec(0.605, 0.435, 0.045, 0.09, 0.008, chimney))
            add(RoundRectSpec(0.40, 0.55, 0.24, 0.21, 0.012, wall))
            add(PolygonSpec(listOf(Vec2(0.355, 0.555), Vec2(0.685, 0.555), Vec2(0.52, 0.40)), roof))
            add(RoundRectSpec(0.475, 0.63, 0.065, 0.13, 0.02, door))
            add(CircleSpec(Vec2(0.565, 0.625), 0.042, wall))
            add(CircleSpec(Vec2(0.565, 0.625), 0.030, window))
            // Flowers: a petal dot with a paper centre.
            val flowers = listOf(Vec2(0.32, 0.79), Vec2(0.41, 0.87), Vec2(0.70, 0.82), Vec2(0.80, 0.72), Vec2(0.60, 0.92))
            for ((i, p) in flowers.withIndex()) {
                add(CircleSpec(p, 0.020, if (i % 2 == 0) flowerA else flowerB))
                add(CircleSpec(p, 0.008, wall))
            }
        }
        return SceneSpec("house", shapes)
    }
}
