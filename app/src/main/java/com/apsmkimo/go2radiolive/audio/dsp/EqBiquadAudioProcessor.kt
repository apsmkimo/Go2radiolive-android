package com.apsmkimo.go2radiolive.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Four-band EQ on 16-bit PCM, emitted as float so later processors can stay linear
 * until the final soft limiter. Bands: low shelf 70 Hz (Bass up to +15 dB),
 * peaking 250 Hz, peaking 1 kHz, high shelf 8 kHz. Low/Mid/High stay ±12 dB.
 * No global pre-gain; the last processor limits peaks.
 */
internal class EqBiquadAudioProcessor : BaseAudioProcessor() {
    private var filters: Array<Array<Biquad>> = emptyArray()
    private var eqWasActive = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_FLOAT,
        )
    }

    override fun onFlush() {
        super.onFlush()
        eqWasActive = false
        val count = inputAudioFormat.channelCount.coerceAtLeast(0)
        filters = Array(count) { Array(4) { Biquad() } }
    }

    override fun onReset() {
        super.onReset()
        filters = emptyArray()
        eqWasActive = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        if (channels <= 0 || sampleRate <= 0) {
            inputBuffer.position(inputBuffer.limit())
            replaceOutputBuffer(0).flip()
            return
        }
        DspRuntime.advance(sampleRate)
        val frame = DspRuntime.frame
        ensureChannels(channels)
        if (!frame.eqActive && eqWasActive) {
            resetFilters()
        }
        eqWasActive = frame.eqActive
        if (frame.eqActive) {
            for (ch in filters.indices) {
                filters[ch][0].set(frame.bass)
                filters[ch][1].set(frame.low)
                filters[ch][2].set(frame.mid)
                filters[ch][3].set(frame.high)
            }
        }

        val frames = inputBuffer.remaining() / (channels * 2)
        val output = replaceOutputBuffer(frames * channels * 4)
        if (!frame.processingActive) {
            repeat(frames * channels) {
                output.putFloat((inputBuffer.getShort().toDouble() / PCM16_SCALE).toFloat())
            }
        } else if (!frame.eqActive) {
            // SMCPKG_SUPPORT>>>Cursor032
            // val headroom = frame.headroom.toFloat()
            // repeat(frames * channels) {
            //     output.putFloat(inputBuffer.getShort().toFloat() / PCM16_SCALE_F * headroom)
            // }
            repeat(frames * channels) {
                output.putFloat(inputBuffer.getShort().toFloat() / PCM16_SCALE_F)
            }
            // SMCPKG_SUPPORT<<<Cursor032
        } else {
            repeat(frames) {
                for (ch in 0 until channels) {
                    var sample = inputBuffer.getShort().toDouble() / PCM16_SCALE
                    // SMCPKG_SUPPORT>>>Cursor033
                    // val headroom = frame.headroom
                    // sample *= headroom
                    // SMCPKG_SUPPORT<<<Cursor033
                    val band = filters[ch]
                    sample = band[0].process(sample)
                    sample = band[1].process(sample)
                    sample = band[2].process(sample)
                    sample = band[3].process(sample)
                    output.putFloat(sample.toFloat())
                }
            }
        }
        if (inputBuffer.hasRemaining()) {
            inputBuffer.position(inputBuffer.limit())
        }
        output.flip()
    }

    private fun ensureChannels(count: Int) {
        if (filters.size != count) {
            filters = Array(count) { Array(4) { Biquad() } }
        }
    }

    private fun resetFilters() {
        filters.forEach { band -> band.forEach { it.reset() } }
    }

    private companion object {
        const val PCM16_SCALE = 32768.0
        const val PCM16_SCALE_F = 32768f
    }
}
