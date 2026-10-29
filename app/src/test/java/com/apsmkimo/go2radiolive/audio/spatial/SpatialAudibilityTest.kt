package com.apsmkimo.go2radiolive.audio.spatial

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

// SMCPKG_SUPPORT>>>Cursor116
class SpatialAudibilityTest {
    @After
    fun restore() {
        AudioSettingsRepository.setSpatialMode(SpatialAudioMode.OFF)
    }

    @Test
    fun monoSurroundIsAudibleWithoutReconfiguringTheSink() {
        AudioSettingsRepository.setSpatialMode(SpatialAudioMode.OFF)
        val processor = SpatialAudioProcessor()
        val format = processor.configure(
            AudioProcessor.AudioFormat(SAMPLE_RATE, 1, C.ENCODING_PCM_FLOAT),
        )
        assertEquals(2, format.channelCount)
        assertTrue(processor.isActive)
        processor.flush()
        AudioSettingsRepository.setSpatialMode(SpatialAudioMode.SURROUND)
        val samples = render(processor, channels = 1, pcm = impulse(channels = 1))
        assertSpatial(samples)
    }

    @Test
    fun correlatedStereoNaturalIsAudibleOnASpeakerSum() {
        AudioSettingsRepository.setSpatialMode(SpatialAudioMode.NATURAL)
        val processor = SpatialAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(SAMPLE_RATE, 2, C.ENCODING_PCM_FLOAT))
        processor.flush()
        val samples = render(processor, channels = 2, pcm = impulse(channels = 2))
        assertSpatial(samples)
    }

    @Test
    fun monoOffStaysADryStereoCopy() {
        AudioSettingsRepository.setSpatialMode(SpatialAudioMode.OFF)
        val processor = SpatialAudioProcessor()
        val format = processor.configure(
            AudioProcessor.AudioFormat(SAMPLE_RATE, 1, C.ENCODING_PCM_FLOAT),
        )
        assertEquals(2, format.channelCount)
        processor.flush()
        val samples = render(processor, channels = 1, pcm = impulse(channels = 1))
        val stats = stats(samples)
        assertTrue("off still moved left and right ${stats.diff}", stats.diff < 1e-3)
        assertTrue("off still had a tail ${stats.late} peak ${stats.peak}", stats.late < 0.02 * stats.peak)
    }

    private fun assertSpatial(samples: FloatArray) {
        val stats = stats(samples)
        assertTrue("no level, peak ${stats.peak}", stats.peak > 0.05)
        assertTrue(
            "left and right matched, diff ${stats.diff} peak ${stats.peak}",
            stats.diff > 0.12 * stats.peak,
        )
        assertTrue(
            "speaker sum had no tail, late ${stats.late} peak ${stats.peak}",
            stats.late > 0.15 * stats.peak,
        )
    }

    private fun stats(samples: FloatArray): LevelStats {
        val frames = samples.size / 2
        var peak = 0.0
        var peakIndex = 0
        var index = 0
        while (index < frames) {
            val left = samples[index * 2].toDouble()
            val right = samples[index * 2 + 1].toDouble()
            val mag = max(abs(left), abs(right))
            if (mag > peak) {
                peak = mag
                peakIndex = index
            }
            index++
        }
        var diff = 0.0
        var late = 0.0
        index = 0
        while (index < frames) {
            val left = samples[index * 2].toDouble()
            val right = samples[index * 2 + 1].toDouble()
            diff = max(diff, abs(left - right))
            if (index > peakIndex + SAMPLE_RATE / 125) {
                late = max(late, abs(left + right) * 0.5)
            }
            index++
        }
        return LevelStats(peak, diff, late)
    }

    private fun impulse(channels: Int): FloatArray {
        val pre = SAMPLE_RATE * 4 / 10
        val tail = SAMPLE_RATE * 12 / 100
        val frames = pre + 1 + tail
        val pcm = FloatArray(frames * channels)
        var channel = 0
        while (channel < channels) {
            pcm[pre * channels + channel] = 0.5f
            channel++
        }
        return pcm
    }

    private fun render(processor: AudioProcessor, channels: Int, pcm: FloatArray): FloatArray {
        val input = ByteBuffer.allocateDirect(pcm.size * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(pcm)
        input.position(0)
        input.limit(pcm.size * 4)
        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.nativeOrder())
        val floats = FloatArray(output.remaining() / 4)
        output.asFloatBuffer().get(floats)
        check(floats.size % 2 == 0)
        check(floats.size / 2 == pcm.size / channels)
        return floats
    }

    private data class LevelStats(val peak: Double, val diff: Double, val late: Double)

    private companion object {
        const val SAMPLE_RATE = 48_000
    }
}
// SMCPKG_SUPPORT<<<Cursor116
