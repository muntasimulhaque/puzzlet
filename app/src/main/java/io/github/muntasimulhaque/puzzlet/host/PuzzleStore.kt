package io.github.muntasimulhaque.puzzlet.host

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "puzzlet")

/** Everything the shelf needs to draw itself, read in one pass. */
data class ShelfProgress(
    /** Wins per picture, which walk the ladder up. */
    val wins: Map<String, Int>,
    /** The count a parent picked per picture; zero means follow the ladder. */
    val chosen: Map<String, Int>,
    /** Whether the two effects play at all. */
    val soundOn: Boolean,
)

/**
 * Everything the app keeps between runs: wins per picture, the count a
 * parent picked per picture, and the sound switch. No accounts, no
 * analytics, nothing that leaves the device. There is no saved picture: a
 * small child always starts a fresh one.
 */
class PuzzleStore(private val context: Context) {

    suspend fun loadProgress(): ShelfProgress = context.dataStore.data.map { prefs ->
        val wins = HashMap<String, Int>()
        val chosen = HashMap<String, Int>()
        for ((key, value) in prefs.asMap()) {
            if (value !is Int) continue
            val name = key.name
            if (name.startsWith(WINS)) wins[name.removePrefix(WINS)] = value
            if (name.startsWith(CHOSEN)) chosen[name.removePrefix(CHOSEN)] = value
        }
        ShelfProgress(wins, chosen, prefs[SOUND_ON] ?: true)
    }.first()

    /** Records one finished picture; answers the new total. */
    suspend fun addWin(sceneId: String): Int {
        var total = 0
        context.dataStore.edit { prefs ->
            val key = intPreferencesKey(WINS + sceneId)
            total = (prefs[key] ?: 0) + 1
            prefs[key] = total
        }
        return total
    }

    /** Remembers the count a parent picked for a picture. */
    suspend fun setChosen(sceneId: String, pieces: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey(CHOSEN + sceneId)] = pieces
        }
    }

    suspend fun setSound(on: Boolean) {
        context.dataStore.edit { prefs -> prefs[SOUND_ON] = on }
    }

    private companion object {
        const val WINS = "wins_"
        const val CHOSEN = "chosen_"
        val SOUND_ON = booleanPreferencesKey("sound_on")
    }
}
