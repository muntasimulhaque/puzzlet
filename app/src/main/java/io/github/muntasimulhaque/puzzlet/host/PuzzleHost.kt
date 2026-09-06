package io.github.muntasimulhaque.puzzlet.host

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.puzzlet.core.Area
import io.github.muntasimulhaque.puzzlet.core.LadderStep
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import io.github.muntasimulhaque.puzzlet.core.Vec2
import io.github.muntasimulhaque.puzzlet.core.createPuzzle
import io.github.muntasimulhaque.puzzlet.core.cutSeedFor
import io.github.muntasimulhaque.puzzlet.core.pieceAt
import io.github.muntasimulhaque.puzzlet.core.stepFor
import io.github.muntasimulhaque.puzzlet.core.stepForPieces
import io.github.muntasimulhaque.puzzlet.core.redeal as redealPuzzle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import io.github.muntasimulhaque.puzzlet.core.drag as dragPiece
import io.github.muntasimulhaque.puzzlet.core.drop as dropPiece
import io.github.muntasimulhaque.puzzlet.core.grab as grabPiece
import io.github.muntasimulhaque.puzzlet.core.place as placePiece
import io.github.muntasimulhaque.puzzlet.core.relayout as relayoutPuzzle

/** Where in the app we are. Home is the picture shelf; tap one and play. */
sealed interface Screen {
    data object Home : Screen
    data class Playing(
        val game: Puzzle,
        /** The piece currently in hand, if any; the field draws it lifted. */
        val draggedId: Int? = null,
        /** The piece that last clicked home, with the time it happened. */
        val pulseId: Int = -1,
        val pulseAt: Long = 0L,
        /** Last restart moment; the field staggers the pour-back from it. */
        val restartAt: Long = 0L,
        /** The finished picture held up over the board, so the child can look. */
        val peeking: Boolean = false,
    ) : Screen
}

/** What the shelf needs: the sound switch and the count each picture opens at. */
data class ShelfState(
    val soundOn: Boolean = true,
    /** Pieces per picture. Missing means follow the ladder; see [stepFor]. */
    val pieces: Map<String, Int> = emptyMap(),
)

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which piece is in hand, and whether the picture is being looked at.
 * Tapping a picture plays it at its count: the ladder's, or the one a
 * parent picked on the shelf (D-047). Finishing records one win. A miss
 * sets the piece home at once in logic while the field springs its tile
 * there, so no cancellation can strand a piece. Nothing teaches; the
 * tray-and-board layout is the whole lesson. There is no saved picture:
 * every launch starts fresh on the shelf.
 */
class PuzzleHost(app: Application) : ViewModel() {

    private val store = PuzzleStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _shelf = MutableStateFlow(ShelfState())
    val shelf: StateFlow<ShelfState> = _shelf.asStateFlow()

    private val wins = HashMap<String, Int>()

    init {
        viewModelScope.launch {
            val saved = runCatching { store.loadProgress() }.getOrNull() ?: return@launch
            wins.putAll(saved.wins)
            _shelf.value = ShelfState(soundOn = saved.soundOn, pieces = saved.chosen)
        }
    }

    fun home() {
        draggedId = null
        _screen.value = Screen.Home
    }

    /** Play a picture at the count its shelf row is showing. */
    fun play(sceneId: String) {
        start(sceneId, stepForScene(sceneId))
    }

    /** Play a picture at a count the parent picked; the choice sticks. */
    fun playAt(sceneId: String, pieces: Int) {
        _shelf.value = _shelf.value.copy(pieces = _shelf.value.pieces + (sceneId to pieces))
        viewModelScope.launch { runCatching { store.setChosen(sceneId, pieces) } }
        start(sceneId, stepForPieces(pieces))
    }

    fun setSound(on: Boolean) {
        _shelf.value = _shelf.value.copy(soundOn = on)
        viewModelScope.launch { runCatching { store.setSound(on) } }
    }

    /** Look at the finished picture, or put it away again. */
    fun setPeek(on: Boolean) {
        val s = _screen.value
        if (s is Screen.Playing) _screen.value = s.copy(peeking = on)
    }

    /**
     * Start a picture. The real field size arrives with the first frame
     * (the play screen measures itself), so a placeholder game is created
     * here and immediately reshaped by layout().
     */
    private fun start(sceneId: String, step: LadderStep) {
        draggedId = null
        viewModelScope.launch {
            val known = wins[sceneId] ?: 0
            _screen.value = Screen.Playing(
                createPuzzle(
                    sceneId = sceneId,
                    rows = step.rows,
                    cols = step.cols,
                    field = Area(0.0, 0.0, 1.0, 1.0),
                    capPx = 1e6,
                    seed = cutSeedFor(sceneId, step.rows, step.cols),
                    seatSeed = Random.Default.nextLong(),
                ),
            )
        }
    }

    /** The count a picture opens at: a parent's pick if there is one, else the ladder's. */
    private fun stepForScene(sceneId: String): LadderStep {
        val chosen = _shelf.value.pieces[sceneId] ?: 0
        if (chosen > 0) return stepForPieces(chosen)
        return stepFor(wins[sceneId] ?: 0)
    }

    fun layout(field: Area, capPx: Double) {
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
            peeking = false,
        )
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
            chime(Sfx.SNAP)
            if (settled.completed) {
                chime(Sfx.CHIME)
                viewModelScope.launch { recordWin(settled.sceneId) }
            }
        }
        return snapped
    }

    /** A fresh jumble after a finish: same cut, new seating, nothing placed. */
    fun restart() {
        val s = _screen.value
        if (s is Screen.Playing) {
            _screen.value = s.copy(
                game = redealPuzzle(s.game, Random.Default.nextLong()),
                draggedId = null,
                pulseId = -1,
                pulseAt = 0L,
                restartAt = System.nanoTime(),
                peeking = false,
            )
        }
    }

    /**
     * One finished picture. Where nobody has chosen a count, the win walks
     * the ladder up so the next game is a little harder; a parent's pick
     * outranks the ladder and stays put.
     */
    private suspend fun recordWin(sceneId: String) {
        val total = runCatching { store.addWin(sceneId) }.getOrNull() ?: return
        wins[sceneId] = total
        val chosen = _shelf.value.pieces[sceneId] ?: 0
        if (chosen <= 0) {
            _shelf.value = _shelf.value.copy(pieces = _shelf.value.pieces + (sceneId to stepFor(total).pieces))
        }
    }

    /** The two effects, unless the shelf switch is off. Haptics never stop. */
    private fun chime(sfx: Sfx) {
        if (_shelf.value.soundOn) soundBoard.play(sfx)
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
