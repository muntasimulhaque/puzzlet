package io.github.muntasimulhaque.puzzlet.core

/**
Four more paintings, added when the shelf grew to twelve (D-049): a truck,
an airplane, flowers on a sill, an ice cream. Pure data in a unit square,
inanimate subjects only, no faces and no eyes, and every one of them on a
graded ground so no piece comes out blank.
*/


    // ---------------------------------------------------------------------
    // Truck: a little truck on a road, hills behind, dashes under the wheels.
    // Nothing aboard, nobody at the wheel.
    // ---------------------------------------------------------------------
    internal fun truck(): SceneSpec {
        val skyTop = 0xFF7FC2DDL
        val skyLow = 0xFFDDF0F3L
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillFar = 0xFF5CA24DL
        val hillMid = 0xFF6FB863L
        val hillNear = 0xFF86CC72L
        val roadTop = 0xFFA0A6AAL
        val roadLow = 0xFF6E7478L
        val cab = 0xFFE4572EL
        val bed = 0xFF0C7A64L
        val cargo = 0xFFE3C99AL
        val crate = 0xFFC9A26AL
        val ink = 0xFF2E3A36L
        val glass = 0xFF7FB8D4L
        val dash = 0xFFF0B429L
        val shade = 0x4D2E3A36L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.58, skyTop, skyLow))
            add(CircleSpec(Vec2(0.14, 0.15), 0.075, sun))
            addAll(cloud(Vec2(0.62, 0.14), 0.7, cloud))
            addAll(cloud(Vec2(0.34, 0.30), 0.45, cloud))
            addAll(rollingHills(1.00, 0.40, listOf(hillFar, hillMid, hillNear), 6, seed = 71))
            // Verge, then the road with its dashes.
            addAll(ground(0.0, 0.66, 1.0, 0.08, hillNear, hillMid, steps = 3))
            addAll(ground(0.0, 0.74, 1.0, 0.26, roadTop, roadLow))
            addAll(texture(0.02, 0.76, 0.96, 0.22, 10, 3, 0.010, roadLow, seed = 73))
            for (i in 0 until 6) add(RoundRectSpec(0.02 + i * 0.17, 0.855, 0.10, 0.022, 0.006, dash))
            // The truck: its shadow, the bed, the cab, two wheels.
            add(EllipseSpec(Vec2(0.51, 0.795), 0.34, 0.032, shade))
            add(RoundRectSpec(0.20, 0.585, 0.365, 0.155, 0.012, bed))
            add(RoundRectSpec(0.235, 0.545, 0.12, 0.055, 0.006, cargo))
            add(RoundRectSpec(0.375, 0.535, 0.14, 0.065, 0.006, crate))
            add(RoundRectSpec(0.58, 0.545, 0.20, 0.195, 0.02, cab))
            add(RoundRectSpec(0.605, 0.565, 0.088, 0.065, 0.010, glass))
            add(RoundRectSpec(0.706, 0.565, 0.055, 0.065, 0.010, glass))
            add(RoundRectSpec(0.762, 0.655, 0.042, 0.062, 0.008, ink))
            add(CircleSpec(Vec2(0.783, 0.700), 0.014, dash))
            for (x in listOf(0.33, 0.70)) {
                add(CircleSpec(Vec2(x, 0.755), 0.055, ink))
                add(CircleSpec(Vec2(x, 0.755), 0.022, cloud))
            }
        }
        return SceneSpec("truck", shapes)
    }


    // ---------------------------------------------------------------------
    // Airplane: a calm morning sky, a plane crossing it with its trail, and
    // clouds at every corner. No pilot, no birds, no creatures.
    // ---------------------------------------------------------------------
    internal fun plane(): SceneSpec {
        val skyTop = 0xFF4FA8D6L
        val skyLow = 0xFFDCEFF6L
        val sun = 0xFFF0B429L
        val halo = 0x59F6D06BL
        val cloud = 0xFFFEFCF8L
        val paper = 0xFFFEFCF8L
        val coral = 0xFFE4572EL
        val glass = 0xFF7FB8D4L
        val ink = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 1.0, skyTop, skyLow))
            add(CircleSpec(Vec2(0.83, 0.15), 0.115, halo))
            add(CircleSpec(Vec2(0.83, 0.15), 0.078, sun))
            addAll(cloud(Vec2(0.17, 0.20), 0.8, cloud))
            addAll(cloud(Vec2(0.52, 0.11), 0.5, cloud))
            addAll(cloud(Vec2(0.24, 0.78), 0.7, cloud))
            addAll(cloud(Vec2(0.82, 0.70), 0.6, cloud))
            addAll(cloud(Vec2(0.62, 0.86), 0.45, cloud))
            // The trail behind it, fading as it goes.
            add(CircleSpec(Vec2(0.30, 0.545), 0.032, cloud))
            add(CircleSpec(Vec2(0.235, 0.515), 0.024, cloud))
            add(CircleSpec(Vec2(0.185, 0.492), 0.017, cloud))
            // The plane: tail, wings, body, nose, windows, stripe.
            add(PolygonSpec(listOf(Vec2(0.375, 0.535), Vec2(0.305, 0.44), Vec2(0.395, 0.44)), coral))
            add(PolygonSpec(listOf(Vec2(0.455, 0.545), Vec2(0.545, 0.545), Vec2(0.505, 0.735)), paper))
            add(PolygonSpec(listOf(Vec2(0.455, 0.535), Vec2(0.520, 0.535), Vec2(0.487, 0.415)), cloud))
            add(RoundRectSpec(0.36, 0.520, 0.30, 0.078, 0.038, paper))
            add(CircleSpec(Vec2(0.66, 0.559), 0.039, paper))
            add(RoundRectSpec(0.375, 0.578, 0.285, 0.016, 0.008, coral))
            for (x in listOf(0.44, 0.50, 0.56, 0.62)) add(CircleSpec(Vec2(x, 0.545), 0.014, glass))
            add(CircleSpec(Vec2(0.652, 0.556), 0.017, ink))
        }
        return SceneSpec("plane", shapes)
    }


    // ---------------------------------------------------------------------
    // Flowers: three pots on a sunny sill. Plants, pots and paint; nothing
    // with a face and nothing that moves.
    // ---------------------------------------------------------------------
    internal fun flowers(): SceneSpec {
        val wallTop = 0xFFDCE6E6L
        val wallLow = 0xFFF6F1E6L
        val sillTop = 0xFFE0BE87L
        val sillLow = 0xFFC79A62L
        val belowTop = 0xFFE4D6BEL
        val belowLow = 0xFFCDC0A4L
        val pot = 0xFFD08A5BL
        val potRim = 0xFFE2A47AL
        val soil = 0xFF6B4A32L
        val leaf = 0xFF5FA054L
        val leafB = 0xFF6FB863L
        val stem = 0xFF4E8C46L
        val petalA = 0xFFE4572EL
        val petalB = 0xFFF0B429L
        val petalC = 0xFFE4572EL
        val heart = 0xFFF6D06BL

        fun bloom(c: Vec2, r: Double, petal: Long): List<SceneShape> = buildList {
            add(RoundRectSpec(c.x - 0.006, c.y, 0.012, r * 2.6, 0.004, stem))
            add(EllipseSpec(Vec2(c.x - 0.062, c.y + r * 1.6), 0.040, 0.020, leaf, angleDeg = -18.0))
            add(EllipseSpec(Vec2(c.x + 0.062, c.y + r * 1.9), 0.036, 0.018, leafB, angleDeg = 18.0))
            for (i in 0 until 6) {
                val a = Math.toRadians(i * 60.0)
                add(CircleSpec(Vec2(c.x + r * 0.86 * kotlin.math.cos(a), c.y + r * 0.86 * kotlin.math.sin(a)), r * 0.62, petal))
            }
            add(CircleSpec(c, r * 0.66, heart))
        }

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.58, wallTop, wallLow))
            addAll(texture(0.03, 0.04, 0.94, 0.50, 7, 6, 0.011, wallLow, seed = 83))
            addAll(ground(0.0, 0.74, 1.0, 0.26, belowTop, belowLow))
            // The sill, with its front edge in shadow.
            addAll(ground(0.0, 0.60, 1.0, 0.14, sillTop, sillLow, steps = 4))
            add(RoundRectSpec(0.0, 0.72, 1.0, 0.022, 0.0, sillLow))
            // Three pots, each with its own flower.
            for ((x, petal, r) in listOf(
                Triple(0.22, petalA, 0.070), Triple(0.50, petalB, 0.082), Triple(0.78, petalC, 0.066),
            )) {
                add(RoundRectSpec(x - 0.075, 0.505, 0.15, 0.105, 0.010, pot))
                add(RoundRectSpec(x - 0.088, 0.492, 0.176, 0.034, 0.008, potRim))
                add(EllipseSpec(Vec2(x, 0.498), 0.078, 0.020, soil))
                addAll(bloom(Vec2(x, 0.40), r, petal))
            }
        }
        return SceneSpec("flowers", shapes)
    }


    // ---------------------------------------------------------------------
    // Ice cream: one tall cone and a little dish, on a table. Sweet, still,
    // and every piece of it something a child can name.
    // ---------------------------------------------------------------------
    internal fun icecream(): SceneSpec {
        val wallTop = 0xFFC9E4DCL
        val wallLow = 0xFFEDF7F2L
        val tableTop = 0xFFEDD3A6L
        val tableLow = 0xFFCEA96BL
        val grain = 0xFFC39C63L
        val cone = 0xFFE8B563L
        val coneDark = 0xFFD19A47L
        val scoopA = 0xFFE4572EL
        val scoopB = 0xFFFDF3E0L
        val scoopC = 0xFF9BD9C4L
        val cherry = 0xFFD83A2BL
        val stem = 0xFF6B4A32L
        val dish = 0xFFFEFCF8L
        val shade = 0x4D8A6A4BL

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.56, wallTop, wallLow))
            addAll(texture(0.03, 0.03, 0.94, 0.50, 7, 6, 0.011, wallLow, seed = 97))
            addAll(ground(0.0, 0.56, 1.0, 0.44, tableTop, tableLow))
            for (y in listOf(0.62, 0.74, 0.86, 0.95)) add(RoundRectSpec(0.0, y, 1.0, 0.009, 0.0, grain))
            // The dish on the left, with one small scoop.
            add(EllipseSpec(Vec2(0.19, 0.775), 0.135, 0.048, shade))
            add(EllipseSpec(Vec2(0.19, 0.770), 0.130, 0.045, dish))
            add(CircleSpec(Vec2(0.19, 0.735), 0.052, scoopB))
            add(CircleSpec(Vec2(0.165, 0.715), 0.020, scoopA))
            // The cone: waffle, then three scoops, then a cherry.
            add(PolygonSpec(listOf(Vec2(0.425, 0.615), Vec2(0.585, 0.615), Vec2(0.505, 0.90)), cone))
            // Waffle: four lines across the cone, narrowing as it does.
            for (y in listOf(0.68, 0.735, 0.79, 0.845)) {
                val half = 0.08 * (0.90 - y) / 0.285
                add(
                    PolygonSpec(
                        listOf(
                            Vec2(0.505 - half, y), Vec2(0.505 + half, y + 0.006),
                            Vec2(0.505 + half, y + 0.013), Vec2(0.505 - half, y + 0.007),
                        ),
                        coneDark,
                    ),
                )
            }
            add(CircleSpec(Vec2(0.445, 0.565), 0.078, scoopA))
            add(CircleSpec(Vec2(0.575, 0.550), 0.068, scoopB))
            add(CircleSpec(Vec2(0.505, 0.470), 0.082, scoopC))
            add(CircleSpec(Vec2(0.505, 0.375), 0.026, cherry))
            add(RoundRectSpec(0.502, 0.325, 0.008, 0.030, 0.003, stem))
            // A spoon, resting on the table.
            add(RoundRectSpec(0.66, 0.90, 0.20, 0.018, 0.009, dish))
            add(EllipseSpec(Vec2(0.65, 0.905), 0.038, 0.026, dish))
        }
        return SceneSpec("icecream", shapes)
    }
