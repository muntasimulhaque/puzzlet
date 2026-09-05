package app.puzzlet.core

/**
Four more paintings, pure data in a unit square, same content rules as
their siblings: inanimate subjects only, no faces, no eyes on objects.
*/


    // ---------------------------------------------------------------------
    // Train: a bright morning, two wagons behind a little red engine, smoke
    // puffing, rails and sleepers for texture. No driver, no animals.
    // ---------------------------------------------------------------------
    internal fun train(): SceneSpec {
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
    internal fun castle(): SceneSpec {
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
    internal fun fruit(): SceneSpec {
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
    internal fun house(): SceneSpec {
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
