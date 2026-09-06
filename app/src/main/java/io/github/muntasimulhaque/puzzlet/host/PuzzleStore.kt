package io.github.muntasimulhaque.puzzlet.host

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "puzzlet")

/**
 * Everything the app keeps between runs: wins per picture, which choose
 * the ladder step. No accounts, no analytics, nothing that leaves the
 * device. There is no saved picture and no sound switch: a small child
 * always starts fresh, and parents have volume buttons.
 */
class PuzzleStore(private val context: Context) {

    /** Wins so far for one picture; a picture never seen is zero. */
    suspend fun loadWins(sceneId: String): Int =
        context.dataStore.data.map { it[intPreferencesKey(keyFor(sceneId))] ?: 0 }.first()

    /** Records one finished picture; answers the new total. */
    suspend fun addWin(sceneId: String): Int {
        var total = 0
        context.dataStore.edit { prefs ->
            total = (prefs[intPreferencesKey(keyFor(sceneId))] ?: 0) + 1
            prefs[intPreferencesKey(keyFor(sceneId))] = total
        }
        return total
    }

    private fun keyFor(sceneId: String) = "wins_$sceneId"
}
