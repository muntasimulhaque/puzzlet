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

    val all: List<SceneSpec> = listOf(sail(), rocket(), house(), lighthouse(), balloon(), train(), castle(), fruit())
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
    // Lighthouse: dawn sky, scalloped sea, a striped tower on the rocks with
    // its beams sweeping, and a tiny boat far off. No crew, no birds.
    // ---------------------------------------------------------------------
    private fun lighthouse(): SceneSpec {
        val sky = 0xFFC9E4F0L
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val sea = 0xFF4A9DBEL
        val seaLight = 0xFF6FBAD8L
        val rock = 0xFF6E7F76L
        val tower = 0xFFFEFCF8L
        val band = 0xFFE4572EL
        val beam = 0x66F0B429L
        val lamp = 0xFFF6D06BL
        val trim = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            add(CircleSpec(Vec2(0.14, 0.15), 0.075, sun))
            addAll(cloud(Vec2(0.30, 0.22), 0.65, cloud))
            addAll(cloud(Vec2(0.78, 0.12), 0.75, cloud))
            // Light beams sweep from the lamp, wide and calm.
            add(PolygonSpec(listOf(Vec2(0.72, 0.31), Vec2(0.24, 0.18), Vec2(0.24, 0.46)), beam))
            add(PolygonSpec(listOf(Vec2(0.72, 0.31), Vec2(0.98, 0.14), Vec2(0.98, 0.44)), beam))
            add(RoundRectSpec(0.0, 0.68, 1.0, 0.32, 0.0, sea))
            for (x in 0..13) add(CircleSpec(Vec2(0.04 + x * 0.08, 0.68), 0.032, seaLight))
            // The little boat far off, sails first, then a dot of hull.
            add(PolygonSpec(listOf(Vec2(0.185, 0.74), Vec2(0.185, 0.83), Vec2(0.27, 0.83)), cloud))
            add(PolygonSpec(listOf(Vec2(0.16, 0.83), Vec2(0.30, 0.83), Vec2(0.27, 0.88), Vec2(0.19, 0.88)), band))
            // Rocks, then the tower rising from them.
            add(PolygonSpec(
                listOf(Vec2(0.58, 0.90), Vec2(0.66, 0.78), Vec2(0.80, 0.74), Vec2(0.98, 0.80), Vec2(1.0, 0.94), Vec2(1.0, 1.0), Vec2(0.60, 1.0)),
                rock,
            ))
            add(PolygonSpec(listOf(Vec2(0.66, 0.78), Vec2(0.70, 0.34), Vec2(0.74, 0.34), Vec2(0.78, 0.78)), tower))
            add(PolygonSpec(listOf(Vec2(0.674, 0.64), Vec2(0.686, 0.52), Vec2(0.754, 0.52), Vec2(0.766, 0.64)), band))
            add(PolygonSpec(listOf(Vec2(0.684, 0.46), Vec2(0.693, 0.38), Vec2(0.747, 0.38), Vec2(0.756, 0.46)), band))
            // Lamp room: rail, light, roof.
            add(RoundRectSpec(0.695, 0.30, 0.05, 0.045, 0.006, trim))
            add(CircleSpec(Vec2(0.72, 0.315), 0.028, lamp))
            add(PolygonSpec(listOf(Vec2(0.688, 0.30), Vec2(0.752, 0.30), Vec2(0.72, 0.235)), band))
        }
        return SceneSpec("lighthouse", shapes)
    }

    // ---------------------------------------------------------------------
    // Balloon: a warm morning sky, a striped balloon with its basket and
    // ropes, a small companion far off, and a green hill with round trees.
    // ---------------------------------------------------------------------
    private fun balloon(): SceneSpec {
        val sky = 0xFFF8DCC0L
        val halo = 0x59F6D06BL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val teal = 0xFF0C7A64L
        val rope = 0xFF2E3A36L
        val basket = 0xFFA67B5BL
        val hill = 0xFF82C86EL
        val hillBack = 0xFF68B055L
        val leaf = 0xFF5FA054L
        val leafB = 0xFF6FB863L
        val trunk = 0xFF8F6A4BL

        fun balloonShape(c: Vec2, r: Double, left: Long, mid: Long, right: Long): List<SceneShape> = listOf(
            EllipseSpec(Vec2(c.x - r * 0.62, c.y), r * 0.66, r * 0.64, left),
            EllipseSpec(Vec2(c.x + r * 0.62, c.y), r * 0.66, r * 0.64, right),
            EllipseSpec(Vec2(c.x, c.y), r * 0.78, r * 0.92, mid),
        )

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            add(CircleSpec(Vec2(0.82, 0.16), 0.115, halo))
            add(CircleSpec(Vec2(0.82, 0.16), 0.080, sun))
            addAll(cloud(Vec2(0.18, 0.16), 0.9, cloud))
            addAll(cloud(Vec2(0.62, 0.34), 0.55, cloud))
            addAll(cloud(Vec2(0.86, 0.55), 0.45, cloud))
            // The little companion balloon, far away.
            addAll(balloonShape(Vec2(0.80, 0.30), 0.055, teal, honey, teal))
            add(RoundRectSpec(0.792, 0.365, 0.016, 0.014, 0.004, basket))
            // The big balloon: envelope, skirt, ropes, basket.
            addAll(balloonShape(Vec2(0.40, 0.32), 0.155, honey, coral, honey))
            add(PolygonSpec(listOf(Vec2(0.36, 0.435), Vec2(0.44, 0.435), Vec2(0.425, 0.475), Vec2(0.375, 0.475)), coral))
            add(RoundRectSpec(0.373, 0.475, 0.008, 0.055, 0.003, rope))
            add(RoundRectSpec(0.419, 0.475, 0.008, 0.055, 0.003, rope))
            add(RoundRectSpec(0.358, 0.53, 0.084, 0.06, 0.012, basket))
            // The hill it drifts over, with round trees.
            add(EllipseSpec(Vec2(0.14, 1.04), 0.60, 0.44, hillBack))
            add(EllipseSpec(Vec2(0.72, 1.10), 0.78, 0.50, hill))
            add(RoundRectSpec(0.24, 0.86, 0.026, 0.09, 0.008, trunk))
            add(CircleSpec(Vec2(0.253, 0.82), 0.062, leaf))
            add(CircleSpec(Vec2(0.212, 0.85), 0.042, leafB))
            add(RoundRectSpec(0.62, 0.92, 0.022, 0.075, 0.007, trunk))
            add(CircleSpec(Vec2(0.631, 0.885), 0.052, leafB))
        }
        return SceneSpec("balloon", shapes)
    }

    // ---------------------------------------------------------------------
    // Train: a bright morning, two wagons behind a little red engine, smoke
    // puffing, rails and sleepers for texture. No driver, no animals.
    // ---------------------------------------------------------------------
    private fun train(): SceneSpec {
        val sky = 0xFFBFDFE8L
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillBack = 0xFF8FCB7AL
        val hillFront = 0xFF7BBF68L
        val engine = 0xFFE4572EL
        val cab = 0xFFC4502CL
        val ink = 0xFF2E3A36L
        val teal = 0xFF0C7A64L
        val honey = 0xFFF0B429L
        val ballast = 0xFFB9A58CL
        val rail = 0xFF4A524EL
        val sleeper = 0xFF8F6A4BL

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            add(CircleSpec(Vec2(0.85, 0.13), 0.065, sun))
            addAll(cloud(Vec2(0.22, 0.15), 0.7, cloud))
            addAll(cloud(Vec2(0.55, 0.25), 0.5, cloud))
            add(EllipseSpec(Vec2(0.15, 1.02), 0.50, 0.34, hillBack))
            add(EllipseSpec(Vec2(0.75, 1.06), 0.60, 0.38, hillFront))
            // Smoke, rising and widening.
            add(CircleSpec(Vec2(0.355, 0.455), 0.026, cloud))
            add(CircleSpec(Vec2(0.385, 0.415), 0.032, cloud))
            add(CircleSpec(Vec2(0.425, 0.372), 0.038, cloud))
            // The engine: chimney, cab, boiler, dome.
            add(RoundRectSpec(0.335, 0.52, 0.045, 0.09, 0.006, ink))
            add(RoundRectSpec(0.320, 0.50, 0.075, 0.028, 0.008, ink))
            add(RoundRectSpec(0.44, 0.50, 0.13, 0.26, 0.01, cab))
            add(CircleSpec(Vec2(0.505, 0.565), 0.032, cloud))
            add(RoundRectSpec(0.285, 0.60, 0.17, 0.155, 0.025, engine))
            add(CircleSpec(Vec2(0.375, 0.615), 0.030, honey))
            // Wagons.
            add(RoundRectSpec(0.58, 0.615, 0.15, 0.135, 0.012, teal))
            add(RoundRectSpec(0.76, 0.615, 0.15, 0.135, 0.012, honey))
            // Wheels.
            for (x in listOf(0.315, 0.395, 0.475, 0.545)) {
                add(CircleSpec(Vec2(x, 0.775), 0.030, ink))
                add(CircleSpec(Vec2(x, 0.775), 0.012, honey))
            }
            for (x in listOf(0.62, 0.69, 0.80, 0.87)) add(CircleSpec(Vec2(x, 0.775), 0.024, ink))
            // The track: ballast, sleepers, two rails.
            add(RoundRectSpec(0.0, 0.80, 1.0, 0.09, 0.0, ballast))
            for (i in 0 until 12) add(RoundRectSpec(0.02 + i * 0.083, 0.828, 0.045, 0.032, 0.004, sleeper))
            add(RoundRectSpec(0.0, 0.786, 1.0, 0.014, 0.0, rail))
            add(RoundRectSpec(0.0, 0.878, 1.0, 0.014, 0.0, rail))
        }
        return SceneSpec("train", shapes)
    }

    // ---------------------------------------------------------------------
    // Castle: a slate dusk with a crescent moon, lit windows, coral roofs
    // and flags, a path home. No dragons, no knights, just stone and light.
    // ---------------------------------------------------------------------
    private fun castle(): SceneSpec {
        val sky = 0xFF7C8BB8L
        val moon = 0xFFFEFCF8L
        val star = 0xFFF6D06BL
        val hillBack = 0xFF4E7A50L
        val hillFront = 0xFF5E8F5CL
        val stone = 0xFFD8D2C4L
        val roof = 0xFFE4572EL
        val window = 0xFFF0B429L
        val gate = 0xFF0C7A64L
        val path = 0xFFC9C3B4L
        val ink = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, sky))
            // Crescent moon: paper disc, then the sky over its shoulder.
            add(CircleSpec(Vec2(0.20, 0.16), 0.070, moon))
            add(CircleSpec(Vec2(0.232, 0.145), 0.058, sky))
            for ((p, r) in listOf(Vec2(0.35, 0.10) to 0.012, Vec2(0.55, 0.16) to 0.010, Vec2(0.10, 0.30) to 0.011)) {
                add(PolygonSpec(starPoints(p, r, r * 0.38, 4, 0.0), star))
            }
            add(EllipseSpec(Vec2(0.50, 0.94), 0.75, 0.38, hillBack))
            add(EllipseSpec(Vec2(0.50, 1.08), 0.80, 0.42, hillFront))
            // Towers with crenellations and coral cones.
            add(RoundRectSpec(0.335, 0.36, 0.095, 0.40, 0.0, stone))
            add(RoundRectSpec(0.57, 0.36, 0.095, 0.40, 0.0, stone))
            for (x in listOf(0.335, 0.3715, 0.408)) add(RoundRectSpec(x, 0.334, 0.022, 0.026, 0.0, stone))
            for (x in listOf(0.57, 0.6065, 0.643)) add(RoundRectSpec(x, 0.334, 0.022, 0.026, 0.0, stone))
            add(PolygonSpec(listOf(Vec2(0.325, 0.36), Vec2(0.44, 0.36), Vec2(0.3825, 0.26)), roof))
            add(PolygonSpec(listOf(Vec2(0.56, 0.36), Vec2(0.675, 0.36), Vec2(0.6175, 0.26)), roof))
            // The keep, its flag, the lit windows, the gate, the path.
            add(RoundRectSpec(0.445, 0.46, 0.11, 0.30, 0.0, stone))
            for (x in listOf(0.445, 0.478, 0.511)) add(RoundRectSpec(x, 0.434, 0.020, 0.026, 0.0, stone))
            add(RoundRectSpec(0.497, 0.395, 0.008, 0.045, 0.002, ink))
            add(PolygonSpec(listOf(Vec2(0.505, 0.40), Vec2(0.505, 0.435), Vec2(0.545, 0.4175)), roof))
            for (p in listOf(Vec2(0.3825, 0.46), Vec2(0.3825, 0.54), Vec2(0.6175, 0.46), Vec2(0.6175, 0.54))) {
                add(CircleSpec(p, 0.020, window))
            }
            for (p in listOf(Vec2(0.475, 0.52), Vec2(0.525, 0.52))) add(CircleSpec(p, 0.018, window))
            add(RoundRectSpec(0.46, 0.62, 0.08, 0.13, 0.04, gate))
            add(PolygonSpec(listOf(Vec2(0.462, 0.75), Vec2(0.538, 0.75), Vec2(0.58, 1.0), Vec2(0.42, 1.0)), path))
            for ((p, c) in listOf(Vec2(0.30, 0.80) to window, Vec2(0.68, 0.84) to roof, Vec2(0.24, 0.88) to roof)) {
                add(CircleSpec(p, 0.012, c))
            }
        }
        return SceneSpec("castle", shapes)
    }

    // ---------------------------------------------------------------------
    // Fruit: a wooden table, a white plate, an apple, an orange, a pear,
    // green grapes, a watermelon wedge, a small cup. Warm and quiet.
    // ---------------------------------------------------------------------
    private fun fruit(): SceneSpec {
        val wall = 0xFFF0E3CEL
        val wood = 0xFFE3C99AL
        val plate = 0xFFFEFCF8L
        val plateRim = 0xFFE3DCC9L
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val stem = 0xFF6B4A32L
        val leaf = 0xFF5FA054L
        val leafB = 0xFF6FB863L
        val dimple = 0xFFE09F1FL
        val pear = 0xFFB8C24EL
        val grape = 0xFF9BC95CL
        val rind = 0xFF5FA054L
        val flesh = 0xFFE4572EL
        val seed = 0xFF2E3A36L
        val cup = 0xFF0C7A64L

        val shapes = buildList<SceneShape> {
            add(RoundRectSpec(0.0, 0.0, 1.0, 1.0, 0.0, wall))
            add(RoundRectSpec(0.0, 0.30, 1.0, 0.70, 0.0, wood))
            add(CircleSpec(Vec2(0.50, 0.60), 0.335, plate))
            add(RingSpec(Vec2(0.50, 0.60), 0.335, 0.335, 0.016, plateRim))
            // Apple.
            add(CircleSpec(Vec2(0.375, 0.50), 0.075, coral))
            add(RoundRectSpec(0.371, 0.405, 0.010, 0.028, 0.004, stem))
            add(EllipseSpec(Vec2(0.402, 0.408), 0.020, 0.011, leaf, angleDeg = -25.0))
            // Orange.
            add(CircleSpec(Vec2(0.585, 0.515), 0.070, honey))
            add(EllipseSpec(Vec2(0.615, 0.452), 0.018, 0.010, leafB, angleDeg = 20.0))
            for (p in listOf(Vec2(0.573, 0.500), Vec2(0.598, 0.538), Vec2(0.615, 0.498))) {
                add(CircleSpec(p, 0.006, dimple))
            }
            // Pear: two circles, one silhouette.
            add(CircleSpec(Vec2(0.285, 0.635), 0.055, pear))
            add(CircleSpec(Vec2(0.285, 0.565), 0.040, pear))
            add(RoundRectSpec(0.281, 0.522, 0.009, 0.026, 0.004, stem))
            // Grapes: a little green pyramid.
            for (p in listOf(Vec2(0.700, 0.545), Vec2(0.740, 0.545), Vec2(0.680, 0.585), Vec2(0.720, 0.585), Vec2(0.760, 0.585), Vec2(0.700, 0.625), Vec2(0.740, 0.625), Vec2(0.720, 0.660))) {
                add(CircleSpec(p, 0.026, grape))
            }
            add(RoundRectSpec(0.716, 0.505, 0.009, 0.028, 0.004, stem))
            // Watermelon wedge: dome of rind, dome of flesh, three seeds.
            add(PolygonSpec(listOf(Vec2(0.60, 0.70), Vec2(0.615, 0.655), Vec2(0.66, 0.625), Vec2(0.74, 0.615), Vec2(0.82, 0.625), Vec2(0.865, 0.655), Vec2(0.88, 0.70)), rind))
            add(PolygonSpec(listOf(Vec2(0.635, 0.70), Vec2(0.647, 0.668), Vec2(0.682, 0.646), Vec2(0.74, 0.638), Vec2(0.798, 0.646), Vec2(0.833, 0.668), Vec2(0.845, 0.70)), flesh))
            for (p in listOf(Vec2(0.712, 0.672), Vec2(0.768, 0.672), Vec2(0.740, 0.696))) {
                add(CircleSpec(p, 0.006, seed))
            }
            // A small cup of something warm.
            add(RingSpec(Vec2(0.215, 0.428), 0.030, 0.026, 0.011, cup))
            add(RoundRectSpec(0.13, 0.38, 0.075, 0.10, 0.02, cup))
        }
        return SceneSpec("fruit", shapes)
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
