package io.github.muntasimulhaque.puzzlet.core

/**
Four more paintings, pure data in a unit square, same content rules as
their siblings: inanimate subjects only, no faces, no eyes on objects.
Every one stands on a graded ground, so no piece comes out blank.
*/


    // ---------------------------------------------------------------------
    // Train: a bright morning, two wagons behind a little red engine, smoke
    // puffing, rails and sleepers for texture. No driver, no animals.
    // ---------------------------------------------------------------------
    internal fun train(): SceneSpec {
        val skyTop = 0xFF9CCFE0L
        val skyLow = 0xFFDFF0F2L
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillFar = 0xFF77BE63L
        val hillMid = 0xFF8CCB78L
        val hillNear = 0xFFA2D98CL
        val engine = 0xFFE4572EL
        val cab = 0xFFC4502CL
        val ink = 0xFF2E3A36L
        val teal = 0xFF0C7A64L
        val honey = 0xFFF0B429L
        val ballast = 0xFFB9A58CL
        val ballastDark = 0xFFA89378L
        val rail = 0xFF4A524EL
        val sleeper = 0xFF8F6A4BL

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.72, skyTop, skyLow))
            add(CircleSpec(Vec2(0.85, 0.13), 0.065, sun))
            addAll(cloud(Vec2(0.22, 0.15), 0.7, cloud))
            addAll(cloud(Vec2(0.55, 0.25), 0.5, cloud))
            // Rolling hills behind the track.
            addAll(rollingHills(1.02, 0.38, listOf(hillFar, hillMid, hillNear), 6, seed = 31))
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
            addAll(ground(0.0, 0.80, 1.0, 0.09, ballast, ballastDark))
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
    internal fun castle(): SceneSpec {
        val skyTop = 0xFF5E6CA0L
        val skyLow = 0xFFA3AED0L
        val moon = 0xFFFEFCF8L
        val star = 0xFFF6D06BL
        val hillFar = 0xFF3F6B45L
        val hillMid = 0xFF4E7A50L
        val hillNear = 0xFF5E8F5CL
        val stone = 0xFFD8D2C4L
        val stoneShade = 0xFFC6BFAEL
        val roof = 0xFFE4572EL
        val window = 0xFFF0B429L
        val gate = 0xFF0C7A64L
        val path = 0xFFC9C3B4L
        val ink = 0xFF2E3A36L

        val shapes = buildList<SceneShape> {
            addAll(ground(0.0, 0.0, 1.0, 0.66, skyTop, skyLow))
            // Crescent moon: paper disc, then the sky over its shoulder.
            add(CircleSpec(Vec2(0.20, 0.16), 0.070, moon))
            add(CircleSpec(Vec2(0.232, 0.145), 0.058, mixArgb(skyTop, skyLow, 0.14)))
            for ((p, r) in listOf(
                Vec2(0.35, 0.10) to 0.012, Vec2(0.55, 0.16) to 0.010, Vec2(0.10, 0.30) to 0.011,
                Vec2(0.70, 0.34) to 0.011, Vec2(0.88, 0.20) to 0.010, Vec2(0.44, 0.40) to 0.009,
                Vec2(0.16, 0.50) to 0.010, Vec2(0.92, 0.50) to 0.009, Vec2(0.62, 0.54) to 0.008,
            )) {
                add(PolygonSpec(starPoints(p, r, r * 0.38, 4, 0.0), star))
            }
            addAll(rollingHills(1.02, 0.40, listOf(hillFar, hillMid, hillNear), 6, seed = 41))
            addAll(texture(0.05, 0.72, 0.90, 0.24, 10, 4, 0.010, hillFar, seed = 43))
            // Towers with crenellations and coral cones, shaded on one side.
            add(RoundRectSpec(0.335, 0.36, 0.095, 0.40, 0.0, stone))
            add(RoundRectSpec(0.57, 0.36, 0.095, 0.40, 0.0, stone))
            add(RoundRectSpec(0.408, 0.36, 0.022, 0.40, 0.0, stoneShade))
            add(RoundRectSpec(0.643, 0.36, 0.022, 0.40, 0.0, stoneShade))
            for (x in listOf(0.335, 0.3715, 0.408)) add(RoundRectSpec(x, 0.334, 0.022, 0.026, 0.0, stone))
            for (x in listOf(0.57, 0.6065, 0.643)) add(RoundRectSpec(x, 0.334, 0.022, 0.026, 0.0, stone))
            add(PolygonSpec(listOf(Vec2(0.325, 0.36), Vec2(0.44, 0.36), Vec2(0.3825, 0.26)), roof))
            add(PolygonSpec(listOf(Vec2(0.56, 0.36), Vec2(0.675, 0.36), Vec2(0.6175, 0.26)), roof))
            // The keep, its flag, the lit windows, the gate, the path.
            add(RoundRectSpec(0.445, 0.46, 0.11, 0.30, 0.0, stone))
            add(RoundRectSpec(0.534, 0.46, 0.021, 0.30, 0.0, stoneShade))
            for (x in listOf(0.445, 0.478, 0.511)) add(RoundRectSpec(x, 0.434, 0.020, 0.026, 0.0, stone))
            add(RoundRectSpec(0.497, 0.395, 0.008, 0.045, 0.002, ink))
            add(PolygonSpec(listOf(Vec2(0.505, 0.40), Vec2(0.505, 0.435), Vec2(0.545, 0.4175)), roof))
            for (p in listOf(Vec2(0.3825, 0.46), Vec2(0.3825, 0.54), Vec2(0.6175, 0.46), Vec2(0.6175, 0.54))) {
                add(CircleSpec(p, 0.020, window))
            }
            for (p in listOf(Vec2(0.475, 0.52), Vec2(0.525, 0.52))) add(CircleSpec(p, 0.018, window))
            add(RoundRectSpec(0.46, 0.62, 0.08, 0.13, 0.04, gate))
            add(PolygonSpec(listOf(Vec2(0.462, 0.75), Vec2(0.538, 0.75), Vec2(0.58, 1.0), Vec2(0.42, 1.0)), path))
            // Two firs on the near hill, and a lantern by the path.
            add(RoundRectSpec(0.795, 0.935, 0.016, 0.055, 0.004, ink))
            add(PolygonSpec(listOf(Vec2(0.803, 0.71), Vec2(0.735, 0.96), Vec2(0.871, 0.96)), hillFar))
            add(PolygonSpec(listOf(Vec2(0.803, 0.79), Vec2(0.752, 0.99), Vec2(0.854, 0.99)), hillMid))
            add(RoundRectSpec(0.915, 0.945, 0.013, 0.045, 0.004, ink))
            add(PolygonSpec(listOf(Vec2(0.921, 0.80), Vec2(0.868, 0.98), Vec2(0.974, 0.98)), hillFar))
            add(CircleSpec(Vec2(0.635, 0.795), 0.022, window))
            add(RoundRectSpec(0.628, 0.812, 0.014, 0.030, 0.004, ink))
            for ((p, c) in listOf(Vec2(0.30, 0.80) to window, Vec2(0.24, 0.88) to roof, Vec2(0.12, 0.94) to roof)) {
                add(CircleSpec(p, 0.012, c))
            }
        }
        return SceneSpec("castle", shapes)
    }


    // ---------------------------------------------------------------------
    // Fruit: a wooden table, a white plate, an apple, an orange, a pear,
    // green grapes, a watermelon wedge, a small cup. Warm and quiet.
    // ---------------------------------------------------------------------
    internal fun fruit(): SceneSpec {
        val wallTop = 0xFFE2C99FL
        val wallLow = 0xFFF7ECD9L
        val woodTop = 0xFFEBD2A4L
        val woodLow = 0xFFCEA96BL
        val grain = 0xFFC39C63L
        val plate = 0xFFFEFCF8L
        val plateWell = 0xFFF3EDE1L
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
            addAll(ground(0.0, 0.0, 1.0, 0.30, wallTop, wallLow))
            addAll(texture(0.03, 0.03, 0.94, 0.24, 8, 3, 0.009, wallLow, seed = 51))
            addAll(ground(0.0, 0.30, 1.0, 0.70, woodTop, woodLow))
            // Grain: long quiet lines, drawn under the plate.
            for (y in listOf(0.36, 0.44, 0.52, 0.68, 0.78, 0.90)) {
                add(RoundRectSpec(0.0, y, 1.0, 0.010, 0.0, grain))
            }
            add(CircleSpec(Vec2(0.50, 0.60), 0.335, plate))
            add(CircleSpec(Vec2(0.50, 0.60), 0.250, plateWell))
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
    internal fun house(): SceneSpec {
        val skyTop = 0xFF87C9DCL
        val skyLow = 0xFFE1F1EDL
        val sun = 0xFFF0B429L
        val cloud = 0xFFFEFCF8L
        val hillFar = 0xFF5CA24DL
        val hillMid = 0xFF6FB863L
        val hillNear = 0xFF86CC72L
        val wallTop = 0xFFFEFCF8L
        val wallLow = 0xFFF0E4D2L
        val siding = 0xFFE6D8C4L
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
            addAll(ground(0.0, 0.0, 1.0, 0.58, skyTop, skyLow))
            add(CircleSpec(Vec2(0.14, 0.15), 0.080, sun))
            addAll(cloud(Vec2(0.72, 0.13), 0.7, cloud))
            addAll(cloud(Vec2(0.40, 0.34), 0.5, cloud))
            // Rolling hills, far to near, with grass on them.
            addAll(rollingHills(1.00, 0.42, listOf(hillFar, hillMid, hillNear), 6, seed = 61))
            addAll(texture(0.04, 0.80, 0.92, 0.18, 12, 4, 0.012, hillFar, seed = 67))
            // Tree behind the hill line.
            add(RoundRectSpec(0.185, 0.60, 0.035, 0.15, 0.01, trunk))
            add(CircleSpec(Vec2(0.202, 0.545), 0.085, leafA))
            add(CircleSpec(Vec2(0.145, 0.588), 0.060, leafB))
            add(CircleSpec(Vec2(0.258, 0.588), 0.058, leafB))
            // Chimney first so the roof covers its foot.
            add(RoundRectSpec(0.605, 0.435, 0.045, 0.09, 0.008, chimney))
            // Weatherboard walls: a graded wall with four quiet courses.
            addAll(ground(0.40, 0.55, 0.24, 0.21, wallTop, wallLow, steps = 5))
            for (i in 0 until 4) add(RoundRectSpec(0.40, 0.596 + i * 0.048, 0.24, 0.006, 0.002, siding))
            add(PolygonSpec(listOf(Vec2(0.355, 0.555), Vec2(0.685, 0.555), Vec2(0.52, 0.40)), roof))
            add(RoundRectSpec(0.475, 0.63, 0.065, 0.13, 0.02, door))
            add(CircleSpec(Vec2(0.565, 0.625), 0.042, wallTop))
            add(CircleSpec(Vec2(0.565, 0.625), 0.030, window))
            // A bush in the near corner, with a flower beside it.
            add(CircleSpec(Vec2(0.865, 0.885), 0.072, leafA))
            add(CircleSpec(Vec2(0.945, 0.905), 0.052, leafB))
            add(CircleSpec(Vec2(0.80, 0.80), 0.018, flowerA))
            add(CircleSpec(Vec2(0.80, 0.80), 0.007, wallTop))
            // Flowers: a petal dot with a paper centre.
            val flowers = listOf(Vec2(0.32, 0.79), Vec2(0.41, 0.87), Vec2(0.70, 0.82), Vec2(0.80, 0.72), Vec2(0.60, 0.92))
            for ((i, p) in flowers.withIndex()) {
                add(CircleSpec(p, 0.020, if (i % 2 == 0) flowerA else flowerB))
                add(CircleSpec(p, 0.008, wallTop))
            }
        }
        return SceneSpec("house", shapes)
    }
