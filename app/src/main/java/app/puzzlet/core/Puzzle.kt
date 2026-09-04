package app.puzzlet.core

import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/**
 * The board and its rules. All positions are in field coordinates: the field
 * is the whole play surface, the board is the centred square where the
 * picture assembles. Domain types hold no Android and no Compose imports, so
 * the whole game is testable on the plain JVM (AGENTS.md, Architecture).
 */
data class Piece(
    val id: Int,
    val shape: PieceShape,
    /** Bbox top-left in field coordinates when the piece sits at its slot. */
    val home: Vec2,
    /** Bbox top-left in field coordinates right now. */
    val current: Vec2,
    val placed: Boolean,
) {
    val size get() = shape.size
    fun centerAt(pos: Vec2) = pos + size * 0.5
    val homeCenter get() = centerAt(home)
    val currentCenter get() = centerAt(current)
    val halfDiagonal get() = hypot(size.x, size.y) / 2.0
}

data class Puzzle(
    val sceneId: String,
    val rows: Int,
    val cols: Int,
    val seed: Long,
    val field: Area,
    val board: Area,
    val pieces: List<Piece>,
    val placedCount: Int,
    val completed: Boolean,
    val snapTolerance: Double,
) {
    fun piece(id: Int): Piece? = pieces.firstOrNull { it.id == id }
}

fun createPuzzle(
    sceneId: String,
    rows: Int,
    cols: Int,
    field: Area,
    boardSide: Double,
    seed: Long,
): Puzzle {
    require(boardSide > 0) { "Board side must be positive" }
    val board = Area(
        (field.w - boardSide) / 2.0,
        (field.h - boardSide) / 2.0,
        boardSide,
        boardSide,
    )
    val cut = PieceCut.generate(rows, cols, boardSide, boardSide, seed)
    val shapes = cut.shapes
    val centers = scatterCenters(
        field = field,
        board = board,
        sizes = shapes.map { it.size },
        seed = seed,
    )
    val pieces = shapes.mapIndexed { i, shape ->
        val cellX = board.x + (i % cols) * cut.cellW
        val cellY = board.y + (i / cols) * cut.cellH
        val home = Vec2(cellX, cellY) + shape.offsetInCell
        val topLeft = centers[i] - shape.size * 0.5
        Piece(
            id = i,
            shape = shape,
            home = home,
            current = clampBoxTopLeft(topLeft, shape.size.x, shape.size.y, field),
            placed = false,
        )
    }
    return Puzzle(
        sceneId = sceneId,
        rows = rows,
        cols = cols,
        seed = seed,
        field = field,
        board = board,
        pieces = pieces,
        placedCount = 0,
        completed = false,
        snapTolerance = 0.38 * minOf(cut.cellW, cut.cellH),
    )
}

/** Lift a piece: it becomes the topmost unplaced piece while held. */
fun grab(p: Puzzle, id: Int): Puzzle {
    val piece = p.piece(id) ?: return p
    if (piece.placed) return p
    val rest = p.pieces.filterNot { it.id == id }
    return p.copy(pieces = rest + piece)
}

/** Move a held piece; its bbox top-left is clamped so it can never leave the field. */
fun drag(p: Puzzle, id: Int, topLeft: Vec2): Puzzle {
    val piece = p.piece(id) ?: return p
    if (piece.placed) return p
    val clamped = clampBoxTopLeft(topLeft, piece.size.x, piece.size.y, p.field)
    if (clamped == piece.current) return p
    return p.copy(pieces = p.pieces.replace(piece.id) { it.copy(current = clamped) })
}

/**
 * Let go. Within tolerance of the slot the piece clicks home; anywhere else
 * it simply rests where it was dropped. No penalties exist (AGENTS.md).
 */
fun drop(p: Puzzle, id: Int): Puzzle {
    val piece = p.piece(id) ?: return p
    if (piece.placed) return p
    val near = dist(piece.currentCenter, piece.homeCenter) <= p.snapTolerance
    if (!near) return p
    val placedPiece = piece.copy(placed = true, current = piece.home)
    val placedCount = p.placedCount + 1
    return p.copy(
        pieces = p.pieces.replace(piece.id) { placedPiece },
        placedCount = placedCount,
        completed = placedCount == p.pieces.size,
    )
}

/**
 * The window changed size or shape: rebuild the cut at the new board size and
 * carry the placed pieces to their new slots. Unplaced pieces scatter again;
 * losing their hand-placed piles on rotation is kinder than letting them be
 * half off-screen.
 */
fun relayout(p: Puzzle, field: Area, boardSide: Double): Puzzle {
    val fresh = createPuzzle(p.sceneId, p.rows, p.cols, field, boardSide, p.seed)
    val pieces = fresh.pieces.map { newPiece ->
        val old = p.piece(newPiece.id) ?: return@map newPiece
        if (old.placed) newPiece.copy(placed = true, current = newPiece.home) else newPiece
    }
    return fresh.copy(
        pieces = pieces,
        placedCount = p.placedCount,
        completed = p.completed,
    )
}

/** Back to a full scatter; same cut, same seed family, fresh piles. */
fun restart(p: Puzzle): Puzzle {
    val centers = scatterCenters(
        field = p.field,
        board = p.board,
        sizes = p.pieces.map { it.size },
        seed = p.seed + 7919L,
    )
    val pieces = p.pieces.mapIndexed { i, piece ->
        val topLeft = centers[i] - piece.size * 0.5
        piece.copy(
            placed = false,
            current = clampBoxTopLeft(topLeft, piece.size.x, piece.size.y, p.field),
        )
    }
    return p.copy(pieces = pieces, placedCount = 0, completed = false)
}

/**
 * Scatter piece centres. Candidates sit on a jittered grid over the whole
 * field; pieces prefer the margin around the board (farthest from the board
 * centre first) and fall back onto the board's own ghost only when a small
 * screen leaves no room. Three passes with a relaxing minimum distance keep
 * this bounded and deterministic: a scatter can never loop forever.
 */
fun scatterCenters(field: Area, board: Area, sizes: List<Vec2>, seed: Long): List<Vec2> {
    if (sizes.isEmpty()) return emptyList()
    val rnd = Random(seed)
    val maxHalf = sizes.maxOf { hypot(it.x, it.y) / 2.0 }
    val step = 2.0 * maxHalf * 1.04
    val cols = max(1, floor((field.w - 2 * maxHalf) / step).toInt() + 1)
    val rows = max(1, floor((field.h - 2 * maxHalf) / step).toInt() + 1)
    val x0 = field.x + maxHalf
    val y0 = field.y + maxHalf

    data class Candidate(val pos: Vec2, val outsideBoard: Boolean, val score: Double)

    val candidates = ArrayList<Candidate>(cols * rows)
    for (r in 0 until rows) for (c in 0 until cols) {
        val pos = Vec2(
            x0 + c * step + (rnd.nextDouble() - 0.5) * 0.44 * step,
            y0 + r * step + (rnd.nextDouble() - 0.5) * 0.44 * step,
        )
        val outside = !board.inflated(maxHalf * 0.5).contains(pos)
        candidates.add(Candidate(pos, outside, dist(pos, board.centerVec()) + rnd.nextDouble() * step * 0.5))
    }
    val ordered = candidates.sortedWith(
        compareBy({ !it.outsideBoard }, { -it.score }),
    )

    val assigned = ArrayList<Vec2>(sizes.size)
    val strictMin = step * 0.98
    val relaxedMin = maxHalf * 1.1
    for (minDist in listOf(strictMin, relaxedMin, 0.0)) {
        for (i in assigned.size until sizes.size) {
            val half = hypot(sizes[i].x, sizes[i].y) / 2.0
            val candidate = ordered.firstOrNull { c ->
                assigned.all { a -> dist(a, c.pos) >= max(minDist, half) } && !assigned.contains(c.pos)
            } ?: continue
            assigned.add(candidate.pos)
        }
        if (assigned.size == sizes.size) break
    }
    // Last resort for absurdly small fields: cycle the candidates with fresh
    // jitter. Bounded, deterministic, and better than an empty seat.
    while (assigned.size < sizes.size) {
        val base = ordered[assigned.size % ordered.size].pos
        assigned.add(base + Vec2(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5) * step * 0.3)
    }
    return assigned
}

/**
 * Rebuild a puzzle from saved progress: same seed, so unplaced pieces land
 * back on their original scatter seats; placed pieces return to their slots.
 */
fun restorePuzzle(
    sceneId: String,
    rows: Int,
    cols: Int,
    placedIds: Set<Int>,
    field: Area,
    boardSide: Double,
    seed: Long,
): Puzzle {
    val fresh = createPuzzle(sceneId, rows, cols, field, boardSide, seed)
    val pieces = fresh.pieces.map { piece ->
        if (piece.id in placedIds) piece.copy(placed = true, current = piece.home) else piece
    }
    val placedCount = pieces.count { it.placed }
    return fresh.copy(
        pieces = pieces,
        placedCount = placedCount,
        completed = pieces.isNotEmpty() && placedCount == pieces.size,
    )
}

/** The topmost unplaced piece whose centre is within reach of [pos]. */
fun pieceAt(p: Puzzle, pos: Vec2, hitRadius: Double): Piece? =
    p.pieces.lastOrNull { piece ->
        !piece.placed && dist(pos, piece.currentCenter) <= maxOf(hitRadius, piece.halfDiagonal)
    }

private fun Area.centerVec() = Vec2(centerX, centerY)

private inline fun List<Piece>.replace(id: Int, transform: (Piece) -> Piece): List<Piece> =
    map { if (it.id == id) transform(it) else it }
