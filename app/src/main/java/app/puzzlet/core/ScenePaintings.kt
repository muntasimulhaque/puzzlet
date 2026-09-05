package app.puzzlet.core

import kotlin.math.cos
import kotlin.math.sin

/**
The first four paintings: what the pictures are made of, as pure data in a
unit square. Content rules live with them (AGENTS.md, hard constraints):
inanimate subjects only, no faces, no eyes on objects.
*/

    // ---------------------------------------------------------------------
    // Sailboat: sun, clouds, scalloped sea, a little boat. Nothing aboard.
    // ---------------------------------------------------------------------
    internal fun sail(): SceneSpec {
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

    internal fun cloud(c: Vec2, s: Double, argb: Long): List<SceneShape> = listOf(
        RoundRectSpec(c.x - 0.10 * s, c.y - 0.012 * s, 0.20 * s, 0.052 * s, 0.026 * s, argb),
        CircleSpec(Vec2(c.x - 0.05 * s, c.y - 0.022 * s), 0.045 * s, argb),
        CircleSpec(Vec2(c.x + 0.03 * s, c.y - 0.030 * s), 0.055 * s, argb),
        CircleSpec(Vec2(c.x + 0.083 * s, c.y - 0.012 * s), 0.036 * s, argb),
    )


    // ---------------------------------------------------------------------
    // Rocket: night sky, sparkles, a cratered moon, a ringed planet, and a
    // rocket with its flame. No crew, no creatures, just machines and sky.
    // ---------------------------------------------------------------------
    internal fun rocket(): SceneSpec {
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
    internal fun lighthouse(): SceneSpec {
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
    internal fun balloon(): SceneSpec {
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
