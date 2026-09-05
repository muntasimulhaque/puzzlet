package io.github.muntasimulhaque.puzzlet.host

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.createPuzzle
import io.github.muntasimulhaque.puzzlet.core.pieceAt
import io.github.muntasimulhaque.puzzlet.core.restorePuzzle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.muntasimulhaque.puzzlet.core.drag as dragPiece
import io.github.muntasimulhaque.puzzlet.core.drop as dropPiece
import io.github.muntasimulhaque.puzzlet.core.grab as grabPiece
import io.github.muntasimulhaque.puzzlet.core.place as placePiece
import io.github.muntasimulhaque.puzzlet.core.relayout as relayoutPuzzle
import io.github.muntasimulhaque.puzzlet.core.restart as restartPuzzle

/** Where in the app we are. Home is the picture menu. */
sealed interface Screen {
    data object Home : Screen
    data class Choose(val sceneId: String) : Screen
    data class Playing(
        val game: Puzzle,
        /** The piece currently in hand, if any; the field draws it lifted. */
        val draggedId: Int? = null,
        /** The piece that last clicked home, with the time it happened. */
        val pulseId: Int = -1,
        val pulseAt: Long = 0L,
        /** Pieces gliding back to their tray seats after a missed drop. */
        val returning: Set<Int> = emptySet(),
    ) : Screen
}

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which piece is in hand, the sound switch, the one unfinished picture that
 * survives both backing out and a process death, and the play choreography:
 * the missed-drop glide home and the restart pour-back. Nothing teaches; the
 * tray-and-board layout is the whole lesson (AGENTS.md, D-037).
 */
class PuzzleHost(app: Application) : ViewModel() {

    private val store = PuzzleStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val resumes = HashMap<String, Puzzle>()

    // Pieces gliding back to their tray seats after a missed drop.
    private val returns = HashMap<Int, Job>()

    init {
        viewModelScope.launch {
            _muted.value = store.loadMuted()
            store.loadResume()?.let { snap ->
                resumes[resumeKey(snap.sceneId, snap.rows, snap.cols)] = restorePuzzle(
                    sceneId = snap.sceneId,
                    rows = snap.rows,
                    cols = snap.cols,
                    placedIds = snap.placed,
                    field = Area(0.0, 0.0, 1.0, 1.0),
                    capPx = 1e6,
                    seed = seedFor(snap.sceneId, snap.rows, snap.cols),
                )
            }
        }
    }

    fun choose(sceneId: String) {
        cancelReturns()
        draggedId = null
        _screen.value = Screen.Choose(sceneId)
    }

    fun home() {
        cancelReturns()
        draggedId = null
        _screen.value = Screen.Home
    }

    /**
     * Enter a picture. The real field size arrives with the first frame
     * (the play screen measures itself), so a placeholder game is created
     * here and immediately reshaped by layout(); progress survives that.
     */
    fun play(sceneId: String, rows: Int, cols: Int) {
        cancelReturns()
        draggedId = null
        val key = resumeKey(sceneId, rows, cols)
        val resumed = resumes.remove(key)
        val game = resumed ?: createPuzzle(
            sceneId = sceneId,
            rows = rows,
            cols = cols,
            field = Area(0.0, 0.0, 1.0, 1.0),
            capPx = 1e6,
            seed = seedFor(sceneId, rows, cols),
        )
        _screen.value = Screen.Playing(game)
    }

    fun layout(field: Area, capPx: Double) {
        if (_screen.value !is Screen.Playing) return
        cancelReturns()
        val s = _screen.value
        if (s !is Screen.Playing) return
        val changed = s.game.field.w != field.w || s.game.field.h != field.h
        if (changed) {
            _screen.value = s.copy(game = relayoutPuzzle(s.game, field, capPx))
        }
    }

    /** Lift a piece; returns its bbox corner so the drag keeps the grip point. */
    fun grabAt(pos: Vec2, hitRadius: Double): Vec2? {
        val s = _screen.value
        if (s !is Screen.Playing) return null
        val piece = pieceAt(s.game, pos, hitRadius, s.game.trayScale) ?: return null
        returns.remove(piece.id)?.cancel()
        draggedId = piece.id
        _screen.value = s.copy(
            game = grabPiece(s.game, piece.id),
            draggedId = piece.id,
            returning = s.returning - piece.id,
        )
        sfx(Sfx.PICK)
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
        _screen.value = s.copy(
            game = game,
            draggedId = null,
            pulseId = if (snapped) id else s.pulseId,
            pulseAt = if (snapped) System.nanoTime() else s.pulseAt,
            returning = if (snapped) s.returning else s.returning + id,
        )
        if (snapped) {
            sfx(Sfx.SNAP)
            if (game.completed) {
                sfx(Sfx.CHIME)
                viewModelScope.launch { store.clearResume() }
            }
        } else {
            sfx(Sfx.DROP)
            startReturn(id)
        }
        return snapped
    }

    fun restart() {
        cancelReturns()
        val s = _screen.value
        if (s is Screen.Playing) {
            resumes.remove(resumeKey(s.game))
            viewModelScope.launch { store.clearResume() }
            val fresh = restartPuzzle(s.game)
            // The pour-back: every piece glides from wherever it was to its
            // tray seat, a beat apart. The end state is exactly restart().
            val froms = s.game.pieces.associate { it.id to it.current }
            val staged = fresh.copy(
                pieces = fresh.pieces.map { p -> p.copy(current = froms[p.id] ?: p.current) },
            )
            _screen.value = s.copy(game = staged, draggedId = null, pulseId = -1, pulseAt = 0L)
            fresh.pieces.forEachIndexed { index, piece ->
                viewModelScope.launch {
                    delay(index * 18L)
                    startReturn(piece.id)
                }
            }
        }
    }

    /** Leave the game; unfinished work stays on the shelf for this session. */
    fun backToChoose() {
        val s = _screen.value
        if (s is Screen.Playing) {
            if (!s.game.completed && s.game.placedCount > 0) {
                val key = resumeKey(s.game)
                resumes[key] = s.game
                viewModelScope.launch { store.saveResume(s.game) }
            }
            _screen.value = Screen.Choose(s.game.sceneId)
        } else {
            _screen.value = Screen.Home
        }
        draggedId = null
    }

    /** Called from onStop: the shelf copy of an unfinished game goes to disk. */
    fun persistNow() {
        val s = _screen.value
        if (s is Screen.Playing && !s.game.completed && s.game.placedCount > 0) {
            val key = resumeKey(s.game)
            resumes[key] = s.game
            viewModelScope.launch { store.saveResume(s.game) }
        }
    }

    fun toggleMuted() {
        val next = !_muted.value
        _muted.value = next
        viewModelScope.launch { store.saveMuted(next) }
    }

    fun hasProgress(sceneId: String): Boolean = resumes.keys.any { it.startsWith("$sceneId:") }

    private fun sfx(sfx: Sfx) {
        if (!_muted.value) soundBoard.play(sfx)
    }

    private fun seatTopLeft(game: Puzzle, id: Int): Vec2 {
        val piece = game.piece(id) ?: return Vec2(0.0, 0.0)
        val seat = game.seats.getOrElse(id) { piece.currentCenter }
        return seat - piece.size * 0.5
    }

    /**
     * Glide a piece from where it is back to its tray seat, at full scale
     * (the screen draws returning pieces full-size); grabbable mid-glide.
     */
    private fun startReturn(id: Int) {
        if (draggedId == id) return
        returns.remove(id)?.cancel()
        val s = _screen.value
        if (s !is Screen.Playing) return
        val piece = s.game.piece(id) ?: return
        if (piece.placed) return
        val from = piece.current
        val to = seatTopLeft(s.game, id)
        val job = viewModelScope.launch {
            val steps = 14
            for (k in 1..steps) {
                delay(16)
                val cur = _screen.value
                if (cur !is Screen.Playing || draggedId == id) {
                    returns.remove(id)
                    return@launch
                }
                val t = k / steps.toDouble()
                val eased = 1.0 - (1.0 - t) * (1.0 - t)
                _screen.value = cur.copy(
                    game = placePiece(cur.game, id, from + (to - from) * eased),
                    returning = if (k == steps) cur.returning - id else cur.returning,
                )
            }
            returns.remove(id)
        }
        returns[id] = job
    }

    private fun cancelReturns() {
        returns.values.forEach { it.cancel() }
        returns.clear()
        val s = _screen.value
        if (s is Screen.Playing && s.returning.isNotEmpty()) {
            _screen.value = s.copy(returning = emptySet())
        }
    }

    private var draggedId: Int? = null

    override fun onCleared() {
        returns.values.forEach { it.cancel() }
        soundBoard.release()
    }
}

private fun resumeKey(sceneId: String, rows: Int, cols: Int) = "$sceneId:$rows:$cols"

private fun resumeKey(game: Puzzle) = resumeKey(game.sceneId, game.rows, game.cols)

private fun seedFor(sceneId: String, rows: Int, cols: Int): Long {
    var h = 1125899906842597L
    for (ch in sceneId) h = 31 * h + ch.code
    return h * 31 + rows * 1009L + cols
}
