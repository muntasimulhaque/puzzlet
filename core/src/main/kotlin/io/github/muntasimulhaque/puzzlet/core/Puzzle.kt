package io.github.muntasimulhaque.puzzlet.core

import kotlin.math.hypot

/**
 * The board and its rules. All positions are in field coordinates: the field
 * is the whole play surface, the tray is the strip where unplaced pieces
 * wait, the board is the centred square where the picture assembles. Domain
 * types hold no Android and no Compose imports, so the whole game is
 * testable on the plain JVM (AGENTS.md, Architecture).
 */
data class Piece(
    val id: Int,
    val shape: PieceShape,
    /** Bbox top-left in field coordinates when the piece sits at its slot. */
    val home: Vec2,
    /**
     * Bbox top-left in field coordinates right now, at board scale. For a
     * waiting piece the centre (current + size/2) is its tray seat, and the
     * piece is drawn at tray scale around that centre, so the unscaled bbox
     * may legitimately leave the field; nothing may clamp a waiting piece.
     */
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
    val seatSeed: Long,
    val field: Area,
    /** The strip above the board where unplaced pieces wait. */
    val tray: Area,
    /** The centred square where the picture assembles. */
    val board: Area,
    /** The scale every waiting piece is drawn at in the tray. */
    val trayScale: Double,
    /** Tray seat centres (a waiting piece's draw centre), by piece id. */
    val seats: List<Vec2>,
    val pieces: List<Piece>,
    val placedCount: Int,
    val completed: Boolean,
    val snapTolerance: Double,
) {
    fun piece(id: Int): Piece? = pieces.firstOrNull { it.id == id }
}

/**
 * The stable cut seed: same picture and difficulty, same cut, every time.
 * The host deals fresh tray seats per game, but the cut never moves, like
 * a bought puzzle. The difficulty ladder previews the exact cut through
 * this, so what the child taps is what the child plays.
 */
fun cutSeedFor(sceneId: String, rows: Int, cols: Int): Long {
    var h = 1125899906842597L
    for (ch in sceneId) h = 31 * h + ch.code
    return h * 31 + rows * 1009L + cols
}

fun createPuzzle(
    sceneId: String,
    rows: Int,
    cols: Int,
    field: Area,
    capPx: Double,
    seed: Long,
    seatSeed: Long = seed,
): Puzzle {
    require(capPx > 0) { "Board cap must be positive" }
    require(field.w > 0 && field.h > 0) { "Field must have positive size" }
    // The ladder's share is a ceiling, not a promise: the first pack shows
    // how much shelf the pieces really need, and the second one is built
    // around that, which hands the spare height to the board.
    val share = trayHeightFor(field.h, rows * cols)
    val probe = buildField(sceneId, rows, cols, field, capPx, seed, seatSeed, share)
    val sizes = probe.pieces.map { it.size }
    val used = trayGridHeight(trayGridFor(probe.tray, sizes), sizes)
    val trayH = snugTrayHeight(field.h, share, used)
    if (trayH >= share - 0.5) return probe
    return buildField(sceneId, rows, cols, field, capPx, seed, seatSeed, trayH)
}

/** One field: a tray of that height, the board under it, the cut and seats. */
private fun buildField(
    sceneId: String,
    rows: Int,
    cols: Int,
    field: Area,
    capPx: Double,
    seed: Long,
    seatSeed: Long,
    trayH: Double,
): Puzzle {
    val tray = Area(field.x, field.y, field.w, trayH)
    val side = boardSideFor(field.w, field.h, trayH, capPx)
    val stage = Area(field.x, field.y + trayH, field.w, maxOf(field.h - trayH, 1.0))
    val board = Area(stage.centerX - side / 2.0, stage.centerY - side / 2.0, side, side)
    val cut = PieceCut.generate(rows, cols, side, side, seed)
    val shapes = cut.shapes
    val snapTolerance = 0.38 * minOf(cut.cellW, cut.cellH)
    // Homes first: the cut and the slots never depend on where pieces wait.
    val homes = shapes.mapIndexed { i, shape ->
        Vec2(board.x + (i % cols) * cut.cellW, board.y + (i / cols) * cut.cellH) + shape.offsetInCell
    }
    // The tray seats every piece above the board, so no piece starts within
    // reach of its own slot: the thinking guarantee is structural now
    // (AGENTS.md, D-037). The seating jumbles from seatSeed (D-041), never
    // serial, while the cut stays stable from seed like a bought puzzle.
    val pack = trayPack(tray, shapes.map { it.size }, seatSeed)
    val pieces = shapes.mapIndexed { i, shape ->
        Piece(
            id = i,
            shape = shape,
            home = homes[i],
            // No clamp here: the seat is the scaled draw centre, and the
            // unscaled bbox of a big piece may leave the field. Clamping a
            // waiting piece would drag it off its seat and into its
            // neighbours, which the tray tests pin as a bug.
            current = pack.seats[i] - shape.size * 0.5,
            placed = false,
        )
    }
    return Puzzle(
        sceneId = sceneId,
        rows = rows,
        cols = cols,
        seed = seed,
        seatSeed = seatSeed,
        field = field,
        tray = tray,
        board = board,
        trayScale = pack.scale,
        seats = pack.seats,
        pieces = pieces,
        placedCount = 0,
        completed = false,
        snapTolerance = snapTolerance,
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
 * the piece glides back to its tray seat (the host animates the glide; the
 * rule here is only that a miss never parks on the board). No penalties
 * exist (AGENTS.md).
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
 * Let go at a finger position, as one rule: the position is clamped like any
 * drag, then the drop rule runs on where the piece really sits. The play
 * field calls this once, at release; while a piece is in hand the finger
 * owns it and nothing writes through the game state (AGENTS.md, D-055).
 */
fun dropAt(p: Puzzle, id: Int, topLeft: Vec2): Puzzle = drop(drag(p, id, topLeft), id)

/**
 * The window changed size or shape: rebuild the cut at the new board size
 * and carry the placed pieces to their new slots. Unplaced pieces re-seat in
 * the tray, which is where they live anyway.
 */
fun relayout(p: Puzzle, field: Area, capPx: Double): Puzzle {
    val fresh = createPuzzle(p.sceneId, p.rows, p.cols, field, capPx, p.seed, p.seatSeed)
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

/** Back to the tray; same cut, same seating, everything waiting again. */
fun restart(p: Puzzle): Puzzle {
    val pieces = p.pieces.map { piece ->
        piece.copy(placed = false, current = p.seats[piece.id] - piece.size * 0.5)
    }
    return p.copy(pieces = pieces, placedCount = 0, completed = false)
}

/** A fresh jumble after a finish: same cut, new seating, nothing placed. */
fun redeal(p: Puzzle, seatSeed: Long): Puzzle {
    val pack = trayPack(p.tray, p.pieces.map { it.size }, seatSeed)
    val pieces = p.pieces.map { piece ->
        piece.copy(placed = false, current = pack.seats[piece.id] - piece.size * 0.5)
    }
    return p.copy(
        seatSeed = seatSeed,
        seats = pack.seats,
        trayScale = pack.scale,
        pieces = pieces,
        placedCount = 0,
        completed = false,
    )
}

/**
 * Rebuild a puzzle from saved progress: same seed, so unplaced pieces land
 * back on their tray seats; placed pieces return to their slots.
 */
fun restorePuzzle(
    sceneId: String,
    rows: Int,
    cols: Int,
    placedIds: Set<Int>,
    field: Area,
    capPx: Double,
    seed: Long,
    seatSeed: Long = seed,
): Puzzle {
    val fresh = createPuzzle(sceneId, rows, cols, field, capPx, seed, seatSeed)
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

/**
 * Set a piece's position exactly, without the field clamp. For host-driven
 * glides whose endpoints are trusted seats; a child's drag keeps using
 * [drag], which clamps.
 */
fun place(p: Puzzle, id: Int, topLeft: Vec2): Puzzle {
    val piece = p.piece(id) ?: return p
    if (piece.placed) return p
    if (topLeft == piece.current) return p
    return p.copy(pieces = p.pieces.replace(piece.id) { it.copy(current = topLeft) })
}

/**
 * The nearest unplaced piece within reach of [pos]. [scaleHint] shrinks the
 * reach for tray-scale pieces, so a fingertip cannot grab a neighbour by
 * accident; placed pieces are never hit, however large the radius.
 */
fun pieceAt(p: Puzzle, pos: Vec2, hitRadius: Double, scaleHint: Double = 1.0): Piece? =
    p.pieces
        .filter { !it.placed && dist(pos, it.currentCenter) <= maxOf(hitRadius, it.halfDiagonal * scaleHint) }
        .minByOrNull { dist(pos, it.currentCenter) }

private inline fun List<Piece>.replace(id: Int, transform: (Piece) -> Piece): List<Piece> =
    map { if (it.id == id) transform(it) else it }
