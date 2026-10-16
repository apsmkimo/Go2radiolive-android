package com.apsmkimo.go2radiolive.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Stereo image stage, then conversion back to 16-bit PCM.
 * Virtual surround is a mid/side width boost plus a short opposite-channel delay.
 * Balance uses constant-power (sine/cosine) gains normalized so center stays unity.
 * Peaks are caught by a lookahead-free gain-smoothing limiter before 16-bit conversion.
 * Mono input skips width, cross-feed, and balance.
 */
internal class StereoFieldAudioProcessor : BaseAudioProcessor() {
    private var delayL = FloatArray(0)
    private var delayR = FloatArray(0)
    private var delayLength = 0
    private var delayIndex = 0
    private var surroundArmed = false
    private var limiterGain = 1.0

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
        limiterGain = 1.0
    }

    override fun onReset() {
        super.onReset()
        delayL = FloatArray(0)
        delayR = FloatArray(0)
        delayLength = 0
        delayIndex = 0
        surroundArmed = false
        limiterGain = 1.0
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
            limiterGain = 1.0
            repeat(frames * channels) {
                output.putShort(floatToPcm16(inputBuffer.getFloat().toDouble()))
            }
        } else if (channels == 1) {
            repeat(frames) {
                val sample = inputBuffer.getFloat().toDouble()
                advanceLimiter(abs(sample))
                output.putShort(floatToPcm16(applyLimiter(sample)))
            }
        } else {
            // SMCPKG_SUPPORT>>>Cursor051
            // The old mid/side widener is not part of the live chain anymore.
            // Spatial modes run in SpatialAudioProcessor, one at a time.
            // if (frame.surroundMix < 0.0008) {
            //     if (surroundArmed) {
            //         clearDelay()
            //         surroundArmed = false
            //     }
            // } else {
            //     surroundArmed = true
            //     if (delayLength == 0) configureDelay(inputAudioFormat.sampleRate)
            // }
            // val applySurround = frame.surroundMix >= 0.0008 && delayLength > 0
            val applySurround = false
            // SMCPKG_SUPPORT<<<Cursor051
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
                var peak = max(abs(left), abs(right))
                val extraCount = channels - 2
                val extras = if (extraCount > 0) DoubleArray(extraCount) else EMPTY_DOUBLES
                for (index in 0 until extraCount) {
                    val sample = inputBuffer.getFloat().toDouble()
                    extras[index] = sample
                    peak = max(peak, abs(sample))
                }
                advanceLimiter(peak)
                output.putShort(floatToPcm16(applyLimiter(left)))
                output.putShort(floatToPcm16(applyLimiter(right)))
                for (sample in extras) {
                    output.putShort(floatToPcm16(applyLimiter(sample)))
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

    // SMCPKG_SUPPORT>>>Cursor040
    // private fun softLimit(sample: Double): Double {
    //     val ax = abs(sample)
    //     if (ax <= LIMIT_KNEE) return sample
    //     val span = LIMIT_CEILING - LIMIT_KNEE
    //     val compressed = LIMIT_KNEE + span * tanh((ax - LIMIT_KNEE) / span)
    //     return if (sample < 0.0) -compressed else compressed
    // }
    //
    // Lookahead-free limiter: attack is immediate so a Bass boost cannot hard-clip,
    // release eases the gain back up. Signals under the ceiling keep full level.
    private fun advanceLimiter(peak: Double) {
        val target = if (peak > LIMIT_CEILING && peak > 0.0) LIMIT_CEILING / peak else 1.0
        if (target < limiterGain) {
            limiterGain = target
        } else {
            val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8_000).toDouble()
            val release = 1.0 - exp(-1.0 / (sampleRate * RELEASE_SECONDS))
            limiterGain += (target - limiterGain) * release
            if (limiterGain > 1.0) limiterGain = 1.0
        }
    }

    private fun applyLimiter(sample: Double): Double {
        return (sample * limiterGain).coerceIn(-LIMIT_CEILING, LIMIT_CEILING)
    }
    // SMCPKG_SUPPORT<<<Cursor040

    private companion object {
        val EMPTY_DOUBLES = DoubleArray(0)
        const val PCM16_SCALE = 32768.0
        const val SURROUND_WIDTH = 1.55
        const val CROSSFEED = 0.18
        const val CROSSFEED_SECONDS = 0.0008
        // const val LIMIT_KNEE = 0.97
        const val LIMIT_CEILING = 0.98
        const val RELEASE_SECONDS = 0.05
    }
}
