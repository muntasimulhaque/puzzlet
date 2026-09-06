package io.github.muntasimulhaque.puzzlet.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import io.github.muntasimulhaque.puzzlet.R
import java.util.Collections

/**
 * SoundPool rather than MediaPlayer, deliberately (the house lesson):
 * MediaPlayer's start latency runs past 100 ms, and past about 100 ms a
 * 3-year-old no longer perceives the sound as caused by their own finger.
 * The effects only work as immediate physical consequences of the child's
 * own actions.
 *
 * Two sounds, nothing more: the click home and the single bell for a
 * finished picture. A lift needs no sound (the piece growing under the
 * finger is the answer) and a miss needs none (gliding home is the
 * answer). Only [Sfx.CHIME] has a pitch. The board refuses to play it
 * twice inside 1200 ms: two pitched notes in quick succession make an
 * interval, and intervals are where melody starts.
 */
enum class Sfx { SNAP, CHIME }

class SoundBoard(context: Context) {

    private val app = context.applicationContext

    private val loaded: Map<Sfx, Int>
    private val readySamples = Collections.synchronizedSet(HashSet<Int>())

    /** Requests that arrived before their sample decoded, replayed on load. */
    private val pending = Collections.synchronizedSet(HashSet<Sfx>())

    @Volatile private var released = false

    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        loaded = mapOf(
            Sfx.SNAP to loadOrZero(R.raw.sfx_snap),
            Sfx.CHIME to loadOrZero(R.raw.sfx_chime),
        )
        // Registered after the load calls, so the listener only ever sees
        // sample ids this map already knows about.
        runCatching {
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                runCatching {
                    if (status != 0) return@setOnLoadCompleteListener
                    readySamples += sampleId
                    loaded.entries.firstOrNull { it.value == sampleId }?.key?.let { wanted ->
                        if (pending.remove(wanted)) playNow(wanted)
                    }
                }
            }
        }
    }

    private fun loadOrZero(resId: Int): Int =
        runCatching { pool.load(app, resId, 1) }.getOrDefault(0)

    @Volatile private var lastChimeAt = 0L

    fun play(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (sfx == Sfx.CHIME) {
            val now = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
            if (now - lastChimeAt < CHIME_GAP_MS) return
            lastChimeAt = now
        }
        if (id == 0 || id !in readySamples) {
            pending.add(sfx)
            return
        }
        playNow(sfx)
    }

    private fun playNow(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (id == 0) return
        val volume = volumeOf(sfx)
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.SNAP -> 0.90f
        Sfx.CHIME -> 0.80f
    }

    fun release() {
        if (released) return
        released = true
        runCatching { pending.clear() }
        runCatching { pool.release() }
    }

    private companion object {
        const val CHIME_GAP_MS = 1200L
    }
}
