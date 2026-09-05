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
        /** Last restart moment; the field staggers the pour-back from it. */
        val restartAt: Long = 0L,
    ) : Screen
}

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which piece is in hand, the sound switch, the one unfinished picture that
 * survives both backing out and a process death, and the play choreography:
 * the missed-drop glide home and the restart pour-back. Nothing teaches; the
 * tray-and-board layout is the whole lesson (AGENTS.md, D-037). A miss sets
 * the piece home at once in logic while the field springs its tile there,
 * so no cancellation can strand a piece; restart sets all home at once and
 * the field staggers the visible pour from restartAt.
 */
class PuzzleHost(app: Application) : ViewModel() {

    private val store = PuzzleStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val resumes = HashMap<String, Puzzle>()

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
            capPx = 1e6,
            seed = seedFor(sceneId, rows, cols),
        )
        _screen.value = Screen.Playing(game)
    }

    fun layout(field: Area, capPx: Double) {
        if (_screen.value !is Screen.Playing) return
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
        draggedId = piece.id
        _screen.value = s.copy(
            game = grabPiece(s.game, piece.id),
            draggedId = piece.id,
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
        // A miss sets the piece home at once in logic. The field animates
        // its tile from the drop point to the seat, so the truth never sits
        // mid flight where a cancel could strand it.
        val settled = if (snapped) game else placePiece(game, id, seatTopLeft(game, id))
        _screen.value = s.copy(
            game = settled,
            draggedId = null,
            pulseId = if (snapped) id else s.pulseId,
            pulseAt = if (snapped) System.nanoTime() else s.pulseAt,
        )
        if (snapped) {
            sfx(Sfx.SNAP)
            if (settled.completed) {
                sfx(Sfx.CHIME)
                viewModelScope.launch { store.clearResume() }
            }
        } else {
            sfx(Sfx.DROP)
        }
        return snapped
    }

    fun restart() {
        val s = _screen.value
        if (s is Screen.Playing) {
            resumes.remove(resumeKey(s.game))
            viewModelScope.launch { store.clearResume() }
            // All home at once in logic; the field staggers the visible
            // pour piece by piece from restartAt.
            _screen.value = s.copy(
                game = restartPuzzle(s.game),
                draggedId = null,
                pulseId = -1,
                pulseAt = 0L,
                restartAt = System.nanoTime(),
            )
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

    private var draggedId: Int? = null

    override fun onCleared() {
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
