package io.github.muntasimulhaque.puzzlet.core

/**
 * Extra clues that keep every piece identifiable. Each picture's big flat
 * areas (sky, sea, wall, hill) get small calm inanimate texture, so no
 * quarter of the unit square is ever blank. A piece is then a thing to
 * recognise, not a mystery to guess: the brain works, nothing confuses.
 *
 * Applied once in [Scenes.all], so paintings stay small and every caller
 * (game, gallery, captures, store art) sees the same picture. All shapes
 * are inanimate, alpha floored at 25 percent like all scene art.
 */
internal fun SceneSpec.withClues(): SceneSpec =
    copy(shapes = shapes + extraFor(id))

private fun extraFor(id: String): List<SceneShape> = when (id) {
    "sail" -> sailExtra()
    "rocket" -> rocketExtra()
    "house" -> houseExtra()
    "lighthouse" -> lighthouseExtra()
    "balloon" -> balloonExtra()
    "train" -> trainExtra()
    "castle" -> castleExtra()
    "fruit" -> fruitExtra()
    else -> emptyList()
}

private fun sailExtra(): List<SceneShape> {
    val cloud = 0xFFFEFCF8L
    val hull = 0xFFE4572EL
    val halo = 0x40F0B429L
    return buildList {
        add(CircleSpec(Vec2(0.82, 0.17), 0.120, halo))
        addAll(cloud(Vec2(0.08, 0.38), 0.45, cloud))
        add(PolygonSpec(listOf(Vec2(0.12, 0.60), Vec2(0.18, 0.60), Vec2(0.165, 0.63), Vec2(0.135, 0.63)), hull))
        add(PolygonSpec(listOf(Vec2(0.15, 0.555), Vec2(0.15, 0.60), Vec2(0.175, 0.60)), cloud))
        add(PolygonSpec(listOf(Vec2(0.82, 0.585), Vec2(0.88, 0.585), Vec2(0.865, 0.615), Vec2(0.835, 0.615)), hull))
        add(PolygonSpec(listOf(Vec2(0.85, 0.54), Vec2(0.85, 0.585), Vec2(0.875, 0.585)), cloud))
        for (p in listOf(Vec2(0.25, 0.80), Vec2(0.45, 0.68), Vec2(0.65, 0.80), Vec2(0.85, 0.82), Vec2(0.35, 0.88), Vec2(0.55, 0.90))) {
            add(CircleSpec(p, 0.009, cloud))
        }
    }
}

private fun rocketExtra(): List<SceneShape> {
    val paper = 0xFFFEFCF8L
    val honey = 0xFFF0B429L
    val halo = 0x59FEFCF8L
    return buildList {
        add(CircleSpec(Vec2(0.20, 0.20), 0.135, halo))
        for (p in listOf(Vec2(0.08, 0.08), Vec2(0.35, 0.28), Vec2(0.55, 0.30), Vec2(0.70, 0.08), Vec2(0.92, 0.78), Vec2(0.42, 0.88), Vec2(0.12, 0.92), Vec2(0.62, 0.62), Vec2(0.30, 0.48), Vec2(0.85, 0.55))) {
            add(CircleSpec(p, 0.007, paper))
        }
        for ((p, r) in listOf(Vec2(0.50, 0.55) to 0.014, Vec2(0.25, 0.75) to 0.013)) {
            add(PolygonSpec(starPoints(p, r, r * 0.38, 4, 0.0), honey))
        }
    }
}

private fun houseExtra(): List<SceneShape> {
    val cloud = 0xFFFEFCF8L
    val honey = 0xFFF0B429L
    val halo = 0x40F0B429L
    val flowerA = 0xFFE4572EL
    val flowerB = 0xFFF0B429L
    val leaf = 0xFF5FA054L
    return buildList {
        add(CircleSpec(Vec2(0.14, 0.15), 0.110, halo))
        addAll(cloud(Vec2(0.45, 0.10), 0.5, cloud))
        add(CircleSpec(Vec2(0.627, 0.38), 0.025, cloud))
        add(CircleSpec(Vec2(0.635, 0.33), 0.030, cloud))
        add(CircleSpec(Vec2(0.515, 0.695), 0.008, honey))
        for ((p, c) in listOf(Vec2(0.25, 0.84) to flowerA, Vec2(0.55, 0.80) to flowerB, Vec2(0.75, 0.90) to flowerA, Vec2(0.35, 0.93) to flowerB)) {
            add(CircleSpec(p, 0.018, c))
        }
        for (p in listOf(Vec2(0.48, 0.85), Vec2(0.62, 0.87), Vec2(0.20, 0.90))) {
            add(CircleSpec(p, 0.015, leaf))
        }
    }
}

private fun lighthouseExtra(): List<SceneShape> {
    val cloud = 0xFFFEFCF8L
    val halo = 0x40F0B429L
    val speck = 0xFF8FA39BL
    return buildList {
        add(CircleSpec(Vec2(0.14, 0.15), 0.105, halo))
        addAll(cloud(Vec2(0.55, 0.08), 0.5, cloud))
        for (p in listOf(Vec2(0.30, 0.78), Vec2(0.55, 0.82), Vec2(0.80, 0.78), Vec2(0.15, 0.90), Vec2(0.45, 0.92))) {
            add(CircleSpec(p, 0.010, cloud))
        }
        for (p in listOf(Vec2(0.72, 0.85), Vec2(0.85, 0.88), Vec2(0.92, 0.84))) {
            add(CircleSpec(p, 0.012, speck))
        }
    }
}

private fun balloonExtra(): List<SceneShape> {
    val cloud = 0xFFFEFCF8L
    val coral = 0xFFE4572EL
    val honey = 0xFFF0B429L
    val teal = 0xFF0C7A64L
    val basket = 0xFFA67B5BL
    return buildList {
        add(CircleSpec(Vec2(0.15, 0.45), 0.045, coral))
        add(RoundRectSpec(0.142, 0.50, 0.016, 0.014, 0.004, basket))
        add(CircleSpec(Vec2(0.65, 0.55), 0.035, teal))
        add(RoundRectSpec(0.643, 0.59, 0.014, 0.012, 0.004, basket))
        addAll(cloud(Vec2(0.38, 0.12), 0.5, cloud))
        addAll(cloud(Vec2(0.95, 0.30), 0.4, cloud))
        for ((p, c) in listOf(Vec2(0.35, 0.92) to coral, Vec2(0.45, 0.88) to honey, Vec2(0.75, 0.90) to coral, Vec2(0.85, 0.95) to honey)) {
            add(CircleSpec(p, 0.015, c))
        }
    }
}

private fun trainExtra(): List<SceneShape> {
    val cloud = 0xFFFEFCF8L
    val halo = 0x40F0B429L
    val leaf = 0xFF5FA054L
    val coral = 0xFFE4572EL
    val honey = 0xFFF0B429L
    return buildList {
        add(CircleSpec(Vec2(0.85, 0.13), 0.095, halo))
        addAll(cloud(Vec2(0.75, 0.30), 0.45, cloud))
        add(CircleSpec(Vec2(0.46, 0.33), 0.042, cloud))
        for (p in listOf(Vec2(0.20, 0.75), Vec2(0.65, 0.78), Vec2(0.85, 0.72), Vec2(0.10, 0.85), Vec2(0.50, 0.82))) {
            add(CircleSpec(p, 0.018, leaf))
        }
        for ((p, c) in listOf(Vec2(0.30, 0.80) to coral, Vec2(0.58, 0.84) to honey, Vec2(0.90, 0.80) to coral)) {
            add(CircleSpec(p, 0.012, c))
        }
    }
}

private fun castleExtra(): List<SceneShape> {
    val halo = 0x59FEFCF8L
    val star = 0xFFF6D06BL
    val window = 0xFFF0B429L
    val stone = 0xFFD8D2C4L
    return buildList {
        add(CircleSpec(Vec2(0.20, 0.16), 0.100, halo))
        for ((p, r) in listOf(Vec2(0.70, 0.10) to 0.012, Vec2(0.85, 0.22) to 0.011, Vec2(0.45, 0.22) to 0.010, Vec2(0.12, 0.42) to 0.011, Vec2(0.88, 0.35) to 0.010, Vec2(0.60, 0.30) to 0.009, Vec2(0.78, 0.08) to 0.010, Vec2(0.28, 0.28) to 0.009)) {
            add(PolygonSpec(starPoints(p, r, r * 0.38, 4, 0.0), star))
        }
        for (p in listOf(Vec2(0.20, 0.82), Vec2(0.80, 0.78), Vec2(0.35, 0.90), Vec2(0.62, 0.90))) {
            add(CircleSpec(p, 0.011, window))
        }
        add(CircleSpec(Vec2(0.485, 0.82), 0.010, stone))
        add(CircleSpec(Vec2(0.515, 0.88), 0.010, stone))
    }
}

private fun fruitExtra(): List<SceneShape> {
    val rim = 0xFFE3DCC9L
    val grain = 0xFFD4B98FL
    val seed = 0xFF2E3A36L
    return buildList {
        for (x in listOf(0.15, 0.35, 0.65, 0.85)) {
            add(CircleSpec(Vec2(x, 0.12), 0.014, rim))
            add(CircleSpec(Vec2(x, 0.22), 0.014, rim))
        }
        add(RoundRectSpec(0.0, 0.42, 1.0, 0.018, 0.0, grain))
        add(RoundRectSpec(0.0, 0.86, 1.0, 0.018, 0.0, grain))
        add(RoundRectSpec(0.0, 0.95, 1.0, 0.020, 0.0, grain))
        for (p in listOf(Vec2(0.38, 0.70), Vec2(0.60, 0.70), Vec2(0.48, 0.78))) {
            add(CircleSpec(p, 0.006, seed))
        }
    }
}
