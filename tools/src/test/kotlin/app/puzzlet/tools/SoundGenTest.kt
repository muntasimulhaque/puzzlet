package app.puzzlet.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.readBytes

class SoundGenTest {

    private val names = listOf("sfx_pick", "sfx_drop", "sfx_snap", "sfx_chime")
    private val expectedMs = mapOf("sfx_pick" to 55, "sfx_drop" to 130, "sfx_snap" to 110, "sfx_chime" to 480)
    private val expectedPeak = mapOf("sfx_pick" to 0.45, "sfx_drop" to 0.40, "sfx_snap" to 0.70, "sfx_chime" to 0.70)

    private fun parse(bytes: ByteArray): Raw {
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(bytes, 12, 4, Charsets.US_ASCII))
        assertEquals(1, bytes[20].toInt() and 0xFF)  // PCM
        assertEquals(1, bytes[22].toInt() and 0xFF)  // mono
        val rate = (bytes[24].toInt() and 0xFF) or ((bytes[25].toInt() and 0xFF) shl 8) or
            ((bytes[26].toInt() and 0xFF) shl 16) or ((bytes[27].toInt() and 0xFF) shl 24)
        assertEquals(44100, rate)
        assertEquals(16, bytes[34].toInt() and 0xFF) // 16-bit
        assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
        val dataBytes = (bytes[40].toInt() and 0xFF) or ((bytes[41].toInt() and 0xFF) shl 8) or
            ((bytes[42].toInt() and 0xFF) shl 16) or ((bytes[43].toInt() and 0xFF) shl 24)
        val samples = ShortArray(dataBytes / 2)
        for (i in samples.indices) {
            samples[i] = ((bytes[44 + 2 * i].toInt() and 0xFF) or ((bytes[45 + 2 * i].toInt() and 0xFF) shl 8)).toShort()
        }
        return Raw(rate, samples)
    }

    private class Raw(val rate: Int, val samples: ShortArray) {
        val ms get() = samples.size * 1000 / rate
        fun peak(): Double = samples.maxOf { kotlin.math.abs(it.toInt()) } / 32767.0
    }

    @Test
    fun `regeneration is byte-identical`() {
        val a = Files.createTempDirectory("sounds-a")
        val b = Files.createTempDirectory("sounds-b")
        SoundGen.generateAll(a)
        SoundGen.generateAll(b)
        for (name in names) {
            val fa = a.resolve("$name.wav")
            val fb = b.resolve("$name.wav")
            assertTrue(fa.readBytes().contentEquals(fb.readBytes()))
        }
    }

    @Test
    fun `every asset is canonical mono wav at the right length and peak`() {
        val dir = Files.createTempDirectory("sounds")
        SoundGen.generateAll(dir)
        for (name in names) {
            val raw = parse(dir.resolve("$name.wav").readBytes())
            val want = expectedMs.getValue(name)
            assertTrue("$name length ${raw.ms}ms should be near ${want}ms", raw.ms in (want * 0.9).toInt()..(want * 1.1).toInt())
            val peak = raw.peak()
            assertTrue("$name peak $peak over spec", peak <= expectedPeak.getValue(name) + 0.002)
            assertTrue("$name too quiet", peak >= expectedPeak.getValue(name) * 0.8)
        }
    }
}
