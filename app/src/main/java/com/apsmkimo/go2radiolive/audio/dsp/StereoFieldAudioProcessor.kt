package com.apsmkimo.go2radiolive.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * Stereo image stage, then conversion back to 16-bit PCM.
 * Virtual surround is a mid/side width boost plus a short opposite-channel delay.
 * Balance uses constant-power (sine/cosine) gains normalized so center stays unity.
 * A soft knee near full scale keeps boosted samples from hard-clipping.
 * Mono input skips width, cross-feed, and balance.
 */
internal class StereoFieldAudioProcessor : BaseAudioProcessor() {
    private var delayL = FloatArray(0)
    private var delayR = FloatArray(0)
    private var delayLength = 0
    private var delayIndex = 0
    private var surroundArmed = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT,
        )
    }

    override fun onFlush() {
        super.onFlush()
        configureDelay(inputAudioFormat.sampleRate)
        surroundArmed = false
    }

    override fun onReset() {
        super.onReset()
        delayL = FloatArray(0)
        delayR = FloatArray(0)
        delayLength = 0
        delayIndex = 0
        surroundArmed = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        if (channels <= 0) {
            inputBuffer.position(inputBuffer.limit())
            replaceOutputBuffer(0).flip()
            return
        }
        val frames = inputBuffer.remaining() / (channels * 4)
        val output = replaceOutputBuffer(frames * channels * 2)
        val frame = DspRuntime.frame
        if (!frame.processingActive) {
            repeat(frames * channels) {
                output.putShort(floatToPcm16(inputBuffer.getFloat().toDouble()))
            }
        } else if (channels == 1) {
            repeat(frames) {
                val sample = softLimit(inputBuffer.getFloat().toDouble())
                output.putShort(floatToPcm16(sample))
            }
        } else {
            if (frame.surroundMix < 0.0008) {
                if (surroundArmed) {
                    clearDelay()
                    surroundArmed = false
                }
            } else {
                surroundArmed = true
                if (delayLength == 0) configureDelay(inputAudioFormat.sampleRate)
            }
            val applySurround = frame.surroundMix >= 0.0008 && delayLength > 0
            val applyBalance = abs(frame.balance) > 0.0008
            repeat(frames) {
                var left = inputBuffer.getFloat().toDouble()
                var right = inputBuffer.getFloat().toDouble()
                if (applySurround) {
                    val mid = 0.5 * (left + right)
                    val side = 0.5 * (left - right)
                    val wideLeft = mid + side * SURROUND_WIDTH
                    val wideRight = mid - side * SURROUND_WIDTH
                    val delayedLeft = delayL[delayIndex].toDouble()
                    val delayedRight = delayR[delayIndex].toDouble()
                    delayL[delayIndex] = wideLeft.toFloat()
                    delayR[delayIndex] = wideRight.toFloat()
                    delayIndex++
                    if (delayIndex >= delayLength) delayIndex = 0
                    val wetLeft = wideLeft * (1.0 - CROSSFEED) + delayedRight * CROSSFEED
                    val wetRight = wideRight * (1.0 - CROSSFEED) + delayedLeft * CROSSFEED
                    val mix = frame.surroundMix
                    left += (wetLeft - left) * mix
                    right += (wetRight - right) * mix
                }
                if (applyBalance) {
                    left *= frame.gainL
                    right *= frame.gainR
                }
                output.putShort(floatToPcm16(softLimit(left)))
                output.putShort(floatToPcm16(softLimit(right)))
                for (ch in 2 until channels) {
                    output.putShort(floatToPcm16(softLimit(inputBuffer.getFloat().toDouble())))
                }
            }
        }
        if (inputBuffer.hasRemaining()) {
            inputBuffer.position(inputBuffer.limit())
        }
        output.flip()
    }

    private fun configureDelay(sampleRate: Int) {
        val rate = sampleRate.coerceAtLeast(8_000)
        val needed = (rate * CROSSFEED_SECONDS).toInt().coerceIn(8, 96)
        if (needed != delayLength) {
            delayLength = needed
            delayL = FloatArray(needed)
            delayR = FloatArray(needed)
        } else if (delayLength > 0) {
            delayL.fill(0f)
            delayR.fill(0f)
        }
        delayIndex = 0
    }

    private fun clearDelay() {
        if (delayLength > 0) {
            delayL.fill(0f)
            delayR.fill(0f)
        }
        delayIndex = 0
    }

    private fun floatToPcm16(sample: Double): Short {
        val scaled = (sample * PCM16_SCALE).roundToInt()
        return scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    private fun softLimit(sample: Double): Double {
        val ax = abs(sample)
        if (ax <= LIMIT_KNEE) return sample
        val span = LIMIT_CEILING - LIMIT_KNEE
        val compressed = LIMIT_KNEE + span * tanh((ax - LIMIT_KNEE) / span)
        return if (sample < 0.0) -compressed else compressed
    }

    private companion object {
        const val PCM16_SCALE = 32768.0
        const val SURROUND_WIDTH = 1.55
        const val CROSSFEED = 0.18
        const val CROSSFEED_SECONDS = 0.0008
        const val LIMIT_KNEE = 0.97
        const val LIMIT_CEILING = 0.995
    }
}
