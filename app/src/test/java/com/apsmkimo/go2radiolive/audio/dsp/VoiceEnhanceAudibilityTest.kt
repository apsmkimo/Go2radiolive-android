package com.apsmkimo.go2radiolive.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// SMCPKG_SUPPORT>>>Cursor116
class VoiceEnhanceAudibilityTest {
    @After
    fun restore() {
        AudioSettingsRepository.setVoiceEnhancement(false)
        DspRuntime.resetForTests()
    }

    @Test
    fun voiceOnRaisesPresenceByAtLeastEightDb() {
        val ratio = levelRatio(enabled = true)
        assertTrue("presence ratio was $ratio", ratio >= 10.0.pow(8.0 / 20.0))
    }

    @Test
    fun voiceOffLeavesTheSineAlone() {
        val ratio = levelRatio(enabled = false)
        assertTrue("dry ratio was $ratio", ratio > 0.92 && ratio < 1.08)
    }

    private fun levelRatio(enabled: Boolean): Double {
        AudioSettingsRepository.setVoiceEnhancement(enabled)
        DspRuntime.resetForTests()
        DspRuntime.advance(SAMPLE_RATE)
        val processor = VoiceEnhanceAudioProcessor()
        processor.configure(
            AudioProcessor.AudioFormat(SAMPLE_RATE, 1, C.ENCODING_PCM_FLOAT),
        )
        processor.flush()
        val frames = SAMPLE_RATE / 2
        val input = FloatArray(frames)
        var index = 0
        while (index < frames) {
            input[index] = (0.2 * sin(2.0 * PI * 2_500.0 * index / SAMPLE_RATE)).toFloat()
            index++
        }
        val output = render(processor, input)
        val skip = SAMPLE_RATE / 20
        val inputRms = rms(input, skip)
        val outputRms = rms(output, skip)
        check(inputRms > 1e-4)
        return outputRms / inputRms
    }

    private fun render(processor: AudioProcessor, pcm: FloatArray): FloatArray {
        val input = ByteBuffer.allocateDirect(pcm.size * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(pcm)
        input.position(0)
        input.limit(pcm.size * 4)
        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.nativeOrder())
        val floats = FloatArray(output.remaining() / 4)
        output.asFloatBuffer().get(floats)
        return floats
    }

    private fun rms(samples: FloatArray, skip: Int): Double {
        var energy = 0.0
        var count = 0
        var index = skip
        while (index < samples.size) {
            val sample = samples[index].toDouble()
            energy += sample * sample
            count++
            index++
        }
        return sqrt(energy / count.coerceAtLeast(1))
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
    }
}
// SMCPKG_SUPPORT<<<Cursor116
