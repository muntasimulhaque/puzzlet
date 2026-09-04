package app.puzzlet.host

import androidx.lifecycle.ViewModel
import app.puzzlet.core.Area
import app.puzzlet.core.Piece
import app.puzzlet.core.Puzzle
import app.puzzlet.core.Vec2
import app.puzzlet.core.createPuzzle
import app.puzzlet.core.pieceAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.puzzlet.core.drag as dragPiece
import app.puzzlet.core.drop as dropPiece
import app.puzzlet.core.grab as grabPiece
import app.puzzlet.core.relayout as relayoutPuzzle
import app.puzzlet.core.restart as restartPuzzle

/** Where in the app we are. Home is the picture menu. */
sealed interface Screen {
    data object Home : Screen
    data class Choose(val sceneId: String) : Screen
    data class Playing(
        val game: Puzzle,
        /** The piece that last clicked home, with the time it happened. */
        val pulseId: Int = -1,
        val pulseAt: Long = 0L,
    ) : Screen
}

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which piece is in hand, and the session's unfinished games (so backing out
 * of a picture and returning resumes it, never restarting it).
 */
class PuzzleHost : ViewModel() {

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private var draggedId: Int? = null
    private val resumes = HashMap<String, Puzzle>()

    fun choose(sceneId: String) {
        draggedId = null
        _screen.value = Screen.Choose(sceneId)
    }

    fun home() {
        draggedId = null
        _screen.value = Screen.Home
    }

    /**
     * Enter a picture. The real field size arrives with the first frame
     * (the play screen measures itself), so a placeholder game is created
     * here and immediately reshaped by layout(); progress survives that.
     */
    fun play(sceneId: String, rows: Int, cols: Int) {
        draggedId = null
        val key = resumeKey(sceneId, rows, cols)
        val resumed = resumes.remove(key)
        val game = resumed ?: createPuzzle(
            sceneId = sceneId,
            rows = rows,
            cols = cols,
            field = Area(0.0, 0.0, 1.0, 1.0),
            boardSide = 1.0,
            seed = seedFor(sceneId, rows, cols),
        )
        _screen.value = Screen.Playing(game)
    }

    fun layout(field: Area, boardSide: Double) {
        val s = _screen.value
        if (s !is Screen.Playing) return
        val changed = s.game.field.w != field.w || s.game.field.h != field.h || s.game.board.w != boardSide
        if (changed) {
            _screen.value = s.copy(game = relayoutPuzzle(s.game, field, boardSide))
        }
    }

    /** Lift a piece; returns its bbox corner so the drag keeps the grip point. */
    fun grabAt(pos: Vec2, hitRadius: Double): Vec2? {
        val s = _screen.value
        if (s !is Screen.Playing) return null
        val piece = pieceAt(s.game, pos, hitRadius) ?: return null
        draggedId = piece.id
        _screen.value = s.copy(game = grabPiece(s.game, piece.id))
        return piece.current
    }

    fun dragTo(topLeft: Vec2) {
        val id = draggedId ?: return
        val s = _screen.value
        if (s is Screen.Playing) {
            _screen.value = s.copy(game = dragPiece(s.game, id, topLeft))
        }
    }

    /** Let go; true when the piece clicked home (the UI answers with a tick). */
    fun drop(): Boolean {
        val id = draggedId
        draggedId = null
        val s = _screen.value
        if (s !is Screen.Playing || id == null) return false
        val game = dropPiece(s.game, id)
        val snapped = game.placedCount > s.game.placedCount
        _screen.value = if (snapped) {
            s.copy(game = game, pulseId = id, pulseAt = System.nanoTime())
        } else {
            s.copy(game = game)
        }
        return snapped
    }

    fun restart() {
        val s = _screen.value
        if (s is Screen.Playing) {
            resumes.remove(resumeKey(s.game))
            _screen.value = s.copy(game = restartPuzzle(s.game), pulseId = -1, pulseAt = 0L)
        }
    }

    /** Leave the game; unfinished work stays on the shelf for this session. */
    fun backToChoose() {
        val s = _screen.value
        if (s is Screen.Playing) {
            if (!s.game.completed && s.game.placedCount > 0) {
                resumes[resumeKey(s.game)] = s.game
            }
            _screen.value = Screen.Choose(s.game.sceneId)
        } else {
            _screen.value = Screen.Home
        }
        draggedId = null
    }

    fun hasProgress(sceneId: String): Boolean = resumes.keys.any { it.startsWith("$sceneId:") }

    /** Snapshot for the draw pass: which piece is in hand right now. */
    fun draggedIdSnapshot(): Int? {
        val s = _screen.value
        if (s !is Screen.Playing) return null
        val id = draggedId ?: return null
        return if (s.game.piece(id)?.placed == false) id else null
    }

    fun currentPiece(id: Int): Piece? {
        val s = _screen.value
        return (s as? Screen.Playing)?.game?.piece(id)
    }
}

private fun resumeKey(sceneId: String, rows: Int, cols: Int) = "$sceneId:$rows:$cols"

private fun resumeKey(game: Puzzle) = resumeKey(game.sceneId, game.rows, game.cols)

private fun seedFor(sceneId: String, rows: Int, cols: Int): Long {
    var h = 1125899906842597L
    for (ch in sceneId) h = 31 * h + ch.code
    return h * 31 + rows * 1009L + cols
}
