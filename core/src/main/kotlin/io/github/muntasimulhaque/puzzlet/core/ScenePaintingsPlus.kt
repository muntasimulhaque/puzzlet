package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.cos
import kotlin.math.sin

/**
Four more paintings, added when the shelf grew to sixteen (D-072): a kite
on a breezy day, a windmill with tulips, a beach afternoon, and a toadstool
in the forest. Pure data in a unit square, same rules as their siblings:
inanimate subjects only, no faces, no eyes on objects, and every one of
them on a graded ground so no piece comes out blank.
*/


    // ---------------------------------------------------------------------
    // Kite: a breezy afternoon, a coral and honey diamond on its string,
    // a tail of bows, a small companion far off, hills below. A toy in
    // the wind; nobody holding it.
    // ---------------------------------------------------------------------
    internal fun kite(): SceneSpec {
        val skyTop = 0xFF5FB4DAL
        val skyLow = 0xFFDCF1F7L
        val halo = 0x59F6D06BL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val wind = 0x59FEFCF8L
        val hillFar = 0xFF5CA24DL
        val hillMid = 0xFF6FB863L
        val hillNear = 0xFF86CC72L
        val leaf = 0xFF4E8C46L
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val ink = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.70, skyTop, skyLow))
            add(CircleSpec(Vec2(0.15, 0.15), 0.115, halo))
            add(CircleSpec(Vec2(0.15, 0.15), 0.078, sun))
            addAll(cloud(Vec2(0.50, 0.10), 0.65, cloud))
            addAll(cloud(Vec2(0.86, 0.30), 0.55, cloud))
            addAll(cloud(Vec2(0.28, 0.30), 0.45, cloud))
            addAll(cloud(Vec2(0.10, 0.50), 0.5, cloud))
            // Wind streaks: quiet pills of pale white riding the breeze.
            add(RoundRectSpec(0.33, 0.185, 0.13, 0.016, 0.008, wind))
            add(RoundRectSpec(0.435, 0.455, 0.10, 0.014, 0.007, wind))
            add(RoundRectSpec(0.715, 0.545, 0.12, 0.014, 0.007, wind))
            addAll(rollingHills(1.02, 0.36, listOf(hillFar, hillMid, hillNear), 6, seed = 101))
            addAll(texture(0.04, 0.80, 0.92, 0.18, 12, 4, 0.012, leaf, seed = 103))
            // A bush in the near corner, so no piece of hill is bare.
            add(CircleSpec(Vec2(0.875, 0.895), 0.068, leaf))
            add(CircleSpec(Vec2(0.945, 0.915), 0.048, hillMid))
            // The string, from the crossing point down and out of the frame.
            add(PolygonSpec(listOf(Vec2(0.616, 0.316), Vec2(0.624, 0.324), Vec2(0.310, 1.0), Vec2(0.298, 1.0)), ink))
            addAll(kiteTail())
            addAll(smallKite())
            // The kite: two halves, then its spars over the join.
            add(PolygonSpec(listOf(Vec2(0.620, 0.175), Vec2(0.505, 0.320), Vec2(0.620, 0.465)), coral))
            add(PolygonSpec(listOf(Vec2(0.620, 0.175), Vec2(0.735, 0.320), Vec2(0.620, 0.465)), honey))
            add(RoundRectSpec(0.6165, 0.175, 0.007, 0.290, 0.003, ink))
            add(RoundRectSpec(0.505, 0.3165, 0.230, 0.007, 0.003, ink))
        }
        return SceneSpec("kite", shapes)
    }

    /** The kite's tail: three string legs, three bows, one tassel end. */
    private fun kiteTail(): List<SceneShape> {
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val teal = 0xFF0C7A64L
        val ink = 0xFF2E3A36L
        return buildList {
            add(PolygonSpec(listOf(Vec2(0.616, 0.462), Vec2(0.624, 0.464), Vec2(0.617, 0.528), Vec2(0.609, 0.526)), ink))
            add(PolygonSpec(listOf(Vec2(0.609, 0.526), Vec2(0.617, 0.528), Vec2(0.602, 0.582), Vec2(0.594, 0.579)), ink))
            add(PolygonSpec(listOf(Vec2(0.594, 0.579), Vec2(0.602, 0.582), Vec2(0.580, 0.628), Vec2(0.572, 0.625)), ink))
            for ((p, c, s) in listOf(
                Triple(Vec2(0.611, 0.547), coral, 0.030),
                Triple(Vec2(0.595, 0.598), honey, 0.027),
                Triple(Vec2(0.576, 0.644), teal, 0.024),
            )) {
                add(RoundRectSpec(p.x - s / 2, p.y - s / 2, s, s, 0.004, c, angleDeg = 45.0))
            }
            add(CircleSpec(Vec2(0.566, 0.672), 0.009, honey))
        }
    }

    /** The little kite far off, teal and paper, with two tail dots. */
    private fun smallKite(): List<SceneShape> = listOf(
        PolygonSpec(listOf(Vec2(0.885, 0.148), Vec2(0.843, 0.200), Vec2(0.885, 0.252)), 0xFF0C7A64L),
        PolygonSpec(listOf(Vec2(0.885, 0.148), Vec2(0.927, 0.200), Vec2(0.885, 0.252)), 0xFFFEFCF8L),
        RoundRectSpec(0.882, 0.148, 0.006, 0.104, 0.002, 0xFF2E3A36L),
        CircleSpec(Vec2(0.878, 0.272), 0.008, 0xFF0C7A64L),
        CircleSpec(Vec2(0.869, 0.294), 0.007, 0xFFF0B429L),
    )


    // ---------------------------------------------------------------------
    // Windmill: a cream mill with a teal roof and four honey sails on the
    // hill, tulips and dot flowers at its feet, a path to its door.
    // ---------------------------------------------------------------------
    internal fun windmill(): SceneSpec {
        val skyTop = 0xFF8FCBE0L
        val skyLow = 0xFFEAF4ECL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillFar = 0xFF5CA24DL
        val hillMid = 0xFF6FB863L
        val hillNear = 0xFF86CC72L
        val leaf = 0xFF4E8C46L
        val stone = 0xFFF2E8D4L
        val stoneShade = 0xFFE0D2B4L
        val roof = 0xFF0C7A64L
        val window = 0xFFF0B429L
        val sail = 0xFFF0B429L
        val ink = 0xFF2E3A36L
        val path = 0xFFE3D6B8L
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val paper = 0xFFFEFCF8L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.66, skyTop, skyLow))
            add(CircleSpec(Vec2(0.13, 0.14), 0.070, sun))
            addAll(cloud(Vec2(0.55, 0.12), 0.7, cloud))
            addAll(cloud(Vec2(0.82, 0.30), 0.5, cloud))
            addAll(cloud(Vec2(0.34, 0.28), 0.45, cloud))
            addAll(rollingHills(1.02, 0.40, listOf(hillFar, hillMid, hillNear), 6, seed = 111))
            addAll(texture(0.04, 0.78, 0.92, 0.20, 12, 4, 0.011, leaf, seed = 113))
            // The path from the door down to the near grass.
            add(PolygonSpec(listOf(Vec2(0.588, 0.848), Vec2(0.662, 0.848), Vec2(0.745, 1.0), Vec2(0.505, 1.0)), path))
            // The mill: tapered body, shaded side, teal roof, two windows.
            add(PolygonSpec(listOf(Vec2(0.545, 0.860), Vec2(0.575, 0.475), Vec2(0.675, 0.475), Vec2(0.705, 0.860)), stone))
            add(PolygonSpec(listOf(Vec2(0.652, 0.475), Vec2(0.675, 0.475), Vec2(0.705, 0.860), Vec2(0.681, 0.860)), stoneShade))
            add(PolygonSpec(listOf(Vec2(0.563, 0.475), Vec2(0.687, 0.475), Vec2(0.625, 0.392)), roof))
            add(CircleSpec(Vec2(0.628, 0.565), 0.024, window))
            add(CircleSpec(Vec2(0.628, 0.662), 0.020, window))
            add(RoundRectSpec(0.596, 0.772, 0.064, 0.088, 0.028, roof))
            // Four sails turning in front of the body, then the hub.
            for (a in listOf(45.0, 135.0, 225.0, 315.0)) {
                val rad = Math.toRadians(a)
                val cx = 0.625 + 0.095 * cos(rad)
                val cy = 0.420 + 0.095 * sin(rad)
                add(RoundRectSpec(cx - 0.0825, cy - 0.025, 0.165, 0.050, 0.006, sail, angleDeg = a))
            }
            add(CircleSpec(Vec2(0.625, 0.420), 0.024, ink))
            add(CircleSpec(Vec2(0.625, 0.420), 0.010, honey))
            // Tulips at the feet, and dot flowers beside the path.
            addAll(tulip(0.455, 0.935, coral))
            addAll(tulip(0.800, 0.905, honey))
            addAll(tulip(0.880, 0.960, coral))
            for ((p, c) in listOf(Vec2(0.235, 0.945) to coral, Vec2(0.320, 0.905) to honey, Vec2(0.945, 0.915) to honey)) {
                add(CircleSpec(p, 0.018, c))
                add(CircleSpec(p, 0.007, paper))
            }
        }
        return SceneSpec("windmill", shapes)
    }

    /** One tulip: stem, leaf, and the notched cup. */
    private fun tulip(x: Double, baseY: Double, color: Long): List<SceneShape> = listOf(
        RoundRectSpec(x - 0.004, baseY - 0.105, 0.008, 0.105, 0.003, 0xFF4E8C46L),
        EllipseSpec(Vec2(x - 0.030, baseY - 0.055), 0.024, 0.011, 0xFF4E8C46L, angleDeg = -28.0),
        PolygonSpec(
            listOf(
                Vec2(x - 0.020, baseY - 0.105), Vec2(x - 0.020, baseY - 0.140), Vec2(x - 0.007, baseY - 0.132),
                Vec2(x, baseY - 0.145), Vec2(x + 0.007, baseY - 0.132), Vec2(x + 0.020, baseY - 0.140),
                Vec2(x + 0.020, baseY - 0.105),
            ),
            color,
        ),
    )


    // ---------------------------------------------------------------------
    // Beach: a golden afternoon, a striped umbrella, a ball, a bucket and
    // spade, a calm sea with a tiny sail far off. No people, no gulls.
    // ---------------------------------------------------------------------
    internal fun beach(): SceneSpec {
        val skyTop = 0xFF6FB9D8L
        val skyLow = 0xFFE3F3F7L
        val halo = 0x59F6D06BL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val seaTop = 0xFF54A9CCL
        val seaLow = 0xFF2F7B98L
        val seaBand = 0xFF7FC4DCL
        val sparkle = 0x59FEFCF8L
        val sandTop = 0xFFF4DFA8L
        val sandLow = 0xFFDDBE7EL
        val sandDot = 0xFFC9A26AL
        val foam = 0xFFFEFCF8L
        val coral = 0xFFE4572EL
        val honey = 0xFFF0B429L
        val teal = 0xFF0C7A64L
        val tealDark = 0xFF0A6350L
        val ink = 0xFF2E3A36L
        val shade = 0x4D8A6A4BL

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.50, skyTop, skyLow))
            add(CircleSpec(Vec2(0.85, 0.13), 0.105, halo))
            add(CircleSpec(Vec2(0.85, 0.13), 0.072, sun))
            addAll(cloud(Vec2(0.16, 0.13), 0.7, cloud))
            addAll(cloud(Vec2(0.47, 0.26), 0.5, cloud))
            addAll(cloud(Vec2(0.72, 0.38), 0.42, cloud))
            // The sea, scalloped, with glitter on it.
            addAll(ground(0.0, 0.50, 1.0, 0.27, seaTop, seaLow))
            for (x in 0..16) add(CircleSpec(Vec2(0.02 + x * 0.062, 0.502), 0.030, seaBand))
            for (x in 0..13) add(CircleSpec(Vec2(0.04 + x * 0.078, 0.605), 0.026, seaTop))
            for (x in 0..11) add(CircleSpec(Vec2(0.03 + x * 0.092, 0.695), 0.022, seaBand))
            addAll(texture(0.03, 0.52, 0.94, 0.22, 10, 3, 0.008, sparkle, seed = 121))
            // The sand, and foam riding the shoreline seam.
            addAll(ground(0.0, 0.76, 1.0, 0.24, sandTop, sandLow))
            addAll(texture(0.03, 0.79, 0.94, 0.19, 14, 4, 0.009, sandDot, seed = 123))
            for (x in 0..12) add(CircleSpec(Vec2(0.02 + x * 0.085, 0.752), 0.020, foam))
            for (x in 0..11) add(CircleSpec(Vec2(0.06 + x * 0.09, 0.772), 0.012, foam))
            // Soft ground shadows, then the things that cast them.
            add(EllipseSpec(Vec2(0.478, 0.845), 0.115, 0.014, shade))
            add(EllipseSpec(Vec2(0.665, 0.922), 0.070, 0.013, shade))
            add(RoundRectSpec(0.229, 0.487, 0.012, 0.316, 0.005, ink))
            addAll(umbrellaCanopy())
            add(CircleSpec(Vec2(0.235, 0.480), 0.015, honey))
            addAll(beachBall())
            addAll(beachBucket())
            addAll(beachSpade())
            // A tiny sail far off, and two shells on the sand.
            add(PolygonSpec(listOf(Vec2(0.565, 0.505), Vec2(0.565, 0.552), Vec2(0.605, 0.552)), foam))
            add(PolygonSpec(listOf(Vec2(0.545, 0.552), Vec2(0.615, 0.552), Vec2(0.603, 0.572), Vec2(0.557, 0.572)), coral))
            add(RingSpec(Vec2(0.245, 0.905), 0.011, 0.009, 0.005, foam))
            add(RingSpec(Vec2(0.800, 0.935), 0.011, 0.009, 0.005, foam))
        }
        return SceneSpec("beach", shapes)
    }

    /** The umbrella canopy: six alternating wedges fanned below the apex. */
    private fun umbrellaCanopy(): List<SceneShape> {
        val coral = 0xFFE4572EL
        val paper = 0xFFFEFCF8L
        val apex = Vec2(0.235, 0.485)
        val r = 0.175
        fun rim(a: Double) = Vec2(apex.x + r * cos(Math.toRadians(a)), apex.y + r * sin(Math.toRadians(a)))
        return buildList {
            for (i in 0 until 6) {
                val a1 = i * 30.0
                add(
                    PolygonSpec(
                        listOf(apex, rim(a1), rim(a1 + 15.0), rim(a1 + 30.0)),
                        if (i % 2 == 0) coral else paper,
                    ),
                )
            }
        }
    }

    /** The beach ball: a paper disc with three gores, coral and honey. */
    private fun beachBall(): List<SceneShape> = listOf(
        CircleSpec(Vec2(0.665, 0.860), 0.058, 0xFFFEFCF8L),
        PolygonSpec(
            listOf(
                Vec2(0.665, 0.8025), Vec2(0.680, 0.812), Vec2(0.687, 0.836), Vec2(0.687, 0.884),
                Vec2(0.680, 0.908), Vec2(0.665, 0.9175), Vec2(0.650, 0.908), Vec2(0.643, 0.884),
                Vec2(0.643, 0.836), Vec2(0.650, 0.812),
            ),
            0xFFE4572EL,
        ),
        PolygonSpec(
            listOf(Vec2(0.703, 0.816), Vec2(0.712, 0.836), Vec2(0.712, 0.884), Vec2(0.703, 0.904), Vec2(0.694, 0.884), Vec2(0.694, 0.836)),
            0xFFF0B429L,
        ),
        PolygonSpec(
            listOf(Vec2(0.627, 0.816), Vec2(0.636, 0.836), Vec2(0.636, 0.884), Vec2(0.627, 0.904), Vec2(0.618, 0.884), Vec2(0.618, 0.836)),
            0xFFF0B429L,
        ),
    )

    /** The bucket: a handle ring, then the tapered body over its lower half. */
    private fun beachBucket(): List<SceneShape> = listOf(
        RingSpec(Vec2(0.44, 0.752), 0.040, 0.032, 0.006, 0xFF2E3A36L),
        PolygonSpec(listOf(Vec2(0.403, 0.752), Vec2(0.477, 0.752), Vec2(0.467, 0.838), Vec2(0.413, 0.838)), 0xFF0C7A64L),
        RoundRectSpec(0.398, 0.744, 0.084, 0.016, 0.007, 0xFF0A6350L),
        RoundRectSpec(0.408, 0.775, 0.054, 0.014, 0.004, 0xFFFEFCF8L),
    )

    /** The spade: honey grip and handle, a coral blade standing in the sand. */
    private fun beachSpade(): List<SceneShape> = listOf(
        RoundRectSpec(0.502, 0.648, 0.026, 0.014, 0.006, 0xFFF0B429L),
        RoundRectSpec(0.509, 0.660, 0.012, 0.125, 0.005, 0xFFF0B429L),
        PolygonSpec(
            listOf(Vec2(0.512, 0.780), Vec2(0.544, 0.780), Vec2(0.549, 0.800), Vec2(0.528, 0.826), Vec2(0.507, 0.800)),
            0xFFE4572EL,
        ),
    )


    // ---------------------------------------------------------------------
    // Mushroom: a mossy clearing under deep green shade, one big toadstool
    // with cream spots, a small one beside it, trunks, ferns and a stone.
    // ---------------------------------------------------------------------
    internal fun mushroom(): SceneSpec {
        val bgTop = 0xFF3E7A52L
        val bgLow = 0xFF8FC48AL
        val canopy = 0xFF2F5F40L
        val beam = 0x40F0B429L
        val trunk = 0xFF8F6A4BL
        val trunkDark = 0xFF7A5940L
        val mossTop = 0xFF9BD08AL
        val mossLow = 0xFF5FA054L
        val mossDot = 0xFF4E8C46L
        val cap = 0xFFD8462FL
        val capDark = 0xFFB93A25L
        val spot = 0xFFF7EEDAL
        val stem = 0xFFF0E4D2L
        val stemShade = 0xFFDFCFB4L
        val fern = 0xFF3F7A44L
        val flower = 0xFFF0B429L
        val stone = 0xFF9AA69BL
        val stoneLit = 0xFFB4BFB2L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.60, bgTop, bgLow))
            // Overhead foliage hanging into the frame, then two light shafts.
            add(EllipseSpec(Vec2(0.18, -0.02), 0.24, 0.11, canopy))
            add(EllipseSpec(Vec2(0.62, -0.05), 0.28, 0.12, canopy))
            add(EllipseSpec(Vec2(0.95, 0.00), 0.20, 0.10, canopy))
            add(PolygonSpec(listOf(Vec2(0.30, 0.0), Vec2(0.40, 0.0), Vec2(0.58, 0.62), Vec2(0.44, 0.62)), beam))
            add(PolygonSpec(listOf(Vec2(0.55, 0.0), Vec2(0.63, 0.0), Vec2(0.82, 0.55), Vec2(0.70, 0.55)), beam))
            // Two trunks at the edges, each with its shade side.
            add(RoundRectSpec(0.055, 0.10, 0.075, 0.50, 0.02, trunk))
            add(RoundRectSpec(0.105, 0.10, 0.020, 0.50, 0.0, trunkDark))
            add(RoundRectSpec(0.885, 0.06, 0.080, 0.56, 0.02, trunk))
            add(RoundRectSpec(0.885, 0.06, 0.020, 0.56, 0.0, trunkDark))
            addAll(ground(0.0, 0.58, 1.0, 0.42, mossTop, mossLow))
            addAll(texture(0.04, 0.64, 0.92, 0.32, 14, 6, 0.010, mossDot, seed = 131))
            // The big toadstool: stem, skirt, cap, gill shadow, spots.
            add(RoundRectSpec(0.44, 0.545, 0.115, 0.300, 0.035, stem))
            add(RoundRectSpec(0.522, 0.545, 0.033, 0.300, 0.015, stemShade))
            add(PolygonSpec(
                listOf(
                    Vec2(0.275, 0.585), Vec2(0.285, 0.505), Vec2(0.330, 0.440), Vec2(0.410, 0.405),
                    Vec2(0.4975, 0.395), Vec2(0.585, 0.405), Vec2(0.665, 0.440), Vec2(0.710, 0.505),
                    Vec2(0.720, 0.585),
                ),
                cap,
            ))
            add(PolygonSpec(listOf(Vec2(0.275, 0.585), Vec2(0.720, 0.585), Vec2(0.712, 0.606), Vec2(0.283, 0.606)), capDark))
            add(EllipseSpec(Vec2(0.4975, 0.605), 0.072, 0.014, stemShade))
            for ((p, r) in listOf(
                Vec2(0.395, 0.470) to 0.030, Vec2(0.545, 0.445) to 0.026, Vec2(0.630, 0.500) to 0.020,
                Vec2(0.330, 0.530) to 0.020, Vec2(0.475, 0.530) to 0.016, Vec2(0.590, 0.555) to 0.014,
            )) {
                add(CircleSpec(p, r, spot))
            }
            addAll(smallMushroom())
            // Ferns, grass blades, a stone, and honey flowers on the moss.
            for ((c, a) in listOf(Vec2(0.125, 0.845) to -30.0, Vec2(0.16, 0.835) to 0.0, Vec2(0.198, 0.845) to 30.0)) {
                add(EllipseSpec(c, 0.055, 0.016, fern, angleDeg = a))
            }
            for ((c, a) in listOf(Vec2(0.862, 0.882) to -25.0, Vec2(0.90, 0.872) to 5.0, Vec2(0.938, 0.882) to 35.0)) {
                add(EllipseSpec(c, 0.055, 0.016, fern, angleDeg = a))
            }
            for ((x, y) in listOf(0.28 to 0.965, 0.38 to 0.99, 0.62 to 0.975, 0.72 to 0.995, 0.86 to 0.975, 0.10 to 0.94)) {
                add(PolygonSpec(listOf(Vec2(x, y), Vec2(x + 0.014, y), Vec2(x + 0.007, y - 0.055)), fern))
            }
            add(EllipseSpec(Vec2(0.295, 0.905), 0.055, 0.030, stone))
            add(EllipseSpec(Vec2(0.285, 0.895), 0.030, 0.016, stoneLit))
            for ((p, r) in listOf(Vec2(0.50, 0.945) to 0.012, Vec2(0.345, 0.915) to 0.010, Vec2(0.685, 0.955) to 0.010)) {
                add(CircleSpec(p, r, flower))
            }
        }
        return SceneSpec("mushroom", shapes)
    }

    /** The small toadstool beside the big one. */
    private fun smallMushroom(): List<SceneShape> = listOf(
        RoundRectSpec(0.735, 0.645, 0.055, 0.155, 0.020, 0xFFF0E4D2L),
        RoundRectSpec(0.768, 0.645, 0.016, 0.155, 0.005, 0xFFDFCFB4L),
        PolygonSpec(
            listOf(
                Vec2(0.665, 0.675), Vec2(0.672, 0.625), Vec2(0.705, 0.598), Vec2(0.7625, 0.590),
                Vec2(0.820, 0.598), Vec2(0.853, 0.625), Vec2(0.860, 0.675),
            ),
            0xFFD8462FL,
        ),
        PolygonSpec(listOf(Vec2(0.665, 0.675), Vec2(0.860, 0.675), Vec2(0.856, 0.688), Vec2(0.669, 0.688)), 0xFFB93A25L),
        CircleSpec(Vec2(0.715, 0.635), 0.014, 0xFFF7EEDAL),
        CircleSpec(Vec2(0.782, 0.618), 0.012, 0xFFF7EEDAL),
        CircleSpec(Vec2(0.826, 0.645), 0.010, 0xFFF7EEDAL),
    )
