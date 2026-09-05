package io.github.muntasimulhaque.puzzlet.host

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.muntasimulhaque.puzzlet.core.Puzzle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "puzzlet")

/**
 * Everything the app keeps between runs, in one small preference file:
 * the sound switch and at most one unfinished picture. No accounts, no
 * analytics, nothing that leaves the device (AGENTS.md, hard constraints).
 */
class PuzzleStore(private val context: Context) {

    data class Snapshot(
        val sceneId: String,
        val rows: Int,
        val cols: Int,
        val placed: Set<Int>,
        val seatSeed: Long,
    )

    private object Keys {
        val SCENE = stringPreferencesKey("resume_scene")
        val ROWS = intPreferencesKey("resume_rows")
        val COLS = intPreferencesKey("resume_cols")
        val PLACED = stringPreferencesKey("resume_placed")
        val SEAT = longPreferencesKey("resume_seat")
        val MUTED = booleanPreferencesKey("muted")
    }

    /** Persist one unfinished picture; it replaces any previously saved one. */
    suspend fun saveResume(game: Puzzle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCENE] = game.sceneId
            prefs[Keys.ROWS] = game.rows
            prefs[Keys.COLS] = game.cols
            prefs[Keys.PLACED] = game.pieces.filter { it.placed }.joinToString(",") { it.id.toString() }
            prefs[Keys.SEAT] = game.seatSeed
        }
    }

    suspend fun clearResume() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SCENE)
            prefs.remove(Keys.ROWS)
            prefs.remove(Keys.COLS)
            prefs.remove(Keys.PLACED)
            prefs.remove(Keys.SEAT)
        }
    }

    suspend fun loadResume(): Snapshot? {
        val prefs = context.dataStore.data.first()
        val scene = prefs[Keys.SCENE] ?: return null
        val rows = prefs[Keys.ROWS] ?: return null
        val cols = prefs[Keys.COLS] ?: return null
        val placed = prefs[Keys.PLACED]
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()
        var seat = prefs[Keys.SEAT]
        if (seat == null) {
            var h = 1125899906842597L
            for (ch in scene) h = 31 * h + ch.code
            seat = h * 31 + rows * 1009L + cols
        }
        return Snapshot(scene, rows, cols, placed, seat)
    }

    /** Read the sound switch once at startup; the toggle writes through. */
    suspend fun loadMuted(): Boolean = context.dataStore.data.first()[Keys.MUTED] ?: false

    suspend fun saveMuted(muted: Boolean) {
        context.dataStore.edit { it[Keys.MUTED] = muted }
    }
}
