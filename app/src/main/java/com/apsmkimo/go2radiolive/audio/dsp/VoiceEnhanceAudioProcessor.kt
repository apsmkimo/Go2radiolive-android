package com.apsmkimo.go2radiolive.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Dialog clarity on the mid (L+R) signal: a low-shelf cut around 180 Hz and a
 * presence peak around 2.5 kHz. Mono is treated as the mid signal.
 * Extra channels beyond the stereo pair are copied unchanged.
 */
internal class VoiceEnhanceAudioProcessor : BaseAudioProcessor() {
    private val lowCut = Biquad()
    private val presence = Biquad()
    private var armed = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        super.onFlush()
        lowCut.reset()
        presence.reset()
        armed = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        if (channels <= 0) {
            inputBuffer.position(inputBuffer.limit())
            replaceOutputBuffer(0).flip()
            return
        }
        val frames = inputBuffer.remaining() / (channels * 4)
        val output = replaceOutputBuffer(frames * channels * 4)
        val frame = DspRuntime.frame
        if (frame.voiceMix < 0.0008) {
            if (armed) {
                lowCut.reset()
                presence.reset()
                armed = false
            }
            repeat(frames * channels) {
                output.putFloat(inputBuffer.getFloat())
            }
        } else {
            armed = true
            lowCut.set(frame.voiceLowCut)
            presence.set(frame.voicePresence)
            val mix = frame.voiceMix
            if (channels == 1) {
                repeat(frames) {
                    val dry = inputBuffer.getFloat().toDouble()
                    val wet = presence.process(lowCut.process(dry))
                    output.putFloat((dry + (wet - dry) * mix).toFloat())
                }
            } else {
                repeat(frames) {
                    val left = inputBuffer.getFloat().toDouble()
                    val right = inputBuffer.getFloat().toDouble()
                    val mid = 0.5 * (left + right)
                    val side = 0.5 * (left - right)
                    val wet = presence.process(lowCut.process(mid))
                    val mixed = mid + (wet - mid) * mix
                    output.putFloat((mixed + side).toFloat())
                    output.putFloat((mixed - side).toFloat())
                    for (ch in 2 until channels) {
                        output.putFloat(inputBuffer.getFloat())
                    }
                }
            }
        }
        if (inputBuffer.hasRemaining()) {
            inputBuffer.position(inputBuffer.limit())
        }
        output.flip()
    }
}
