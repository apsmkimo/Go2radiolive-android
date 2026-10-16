package com.apsmkimo.go2radiolive.audio.spatial

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.audio.dsp.Biquad
import com.apsmkimo.go2radiolive.audio.dsp.designPeaking
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * One spatial renderer for the whole stream. Modes are exclusive: the previous
 * mode is faded out before the next mode fades in, so two algorithms never
 * process the same samples.
 *
 * Channel strategy: the decoder channel count is the primary cue. Stereo is
 * kept as two sources. A running L/R correlation only tightens Natural 3D
 * placement when the pair is almost dual-mono; it is never used to downmix.
 * Mono and stereo both leave this processor as stereo float, because the
 * output channel count is fixed when the stream is configured. Off copies
 * stereo unchanged and writes the same mono sample to both ears.
 *
 * More than two channels are copied through untouched.
 */
internal class SpatialAudioProcessor : BaseAudioProcessor() {
    private val leftSource = SourcePath()
    private val rightSource = SourcePath()
    private val reflectionDelay = FractionalDelay(DELAY_CAPACITY)
    private val reflectionLowpass = OnePoleLowpass()
    private val rearReflectionNotch = Biquad()

    private var sampleRate = 48_000
    private var wetCoeff = 0.01
    private var limitRelease = 0.01
    private var wet = 0.0
    private var outGain = 1.0
    private var targetWet = 0.0
    private var activeMode = SpatialAudioMode.OFF
    private var pendingMode = SpatialAudioMode.OFF
    private var orbitAzimuth = 0.0
    private var orbitSign = 1.0
    private var periodSec = 60.0
    private var strength = 0.6
    private var stereoCorrelation = 0.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val outChannels = when (inputAudioFormat.channelCount) {
            1, 2 -> 2
            else -> inputAudioFormat.channelCount
        }
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            outChannels,
            C.ENCODING_PCM_FLOAT,
        )
    }

    override fun onFlush() {
        super.onFlush()
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8_000)
        wetCoeff = 1.0 - exp(-1.0 / (sampleRate * WET_SECONDS))
        limitRelease = 1.0 - exp(-1.0 / (sampleRate * LIMIT_RELEASE_SECONDS))
        wet = 0.0
        outGain = 1.0
        leftSource.reset()
        rightSource.reset()
        reflectionDelay.clear()
        reflectionLowpass.reset()
        reflectionLowpass.setCutoff(2_800.0, sampleRate.toDouble())
        rearReflectionNotch.reset()
        rearReflectionNotch.set(designPeaking(sampleRate, 7_600.0, -8.0, q = 2.2))
        orbitAzimuth = 0.0
        prepareCoeffs(0.0, leftSource)
        prepareCoeffs(0.0, rightSource)
    }

    override fun onReset() {
        super.onReset()
        onFlush()
        activeMode = SpatialAudioMode.OFF
        pendingMode = SpatialAudioMode.OFF
        targetWet = 0.0
        stereoCorrelation = 0.0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val rate = inputAudioFormat.sampleRate
        if (channels <= 0 || rate <= 0) {
            inputBuffer.position(inputBuffer.limit())
            replaceOutputBuffer(0).flip()
            return
        }
        if (channels > 2) {
            passthrough(inputBuffer, channels)
            return
        }
        syncControls()
        val frames = inputBuffer.remaining() / (channels * 4)
        val output = replaceOutputBuffer(frames * 2 * 4)
        if (shouldBypass()) {
            outGain = 1.0
            writeDry(inputBuffer, output, channels, frames)
            finish(inputBuffer, output)
            return
        }

        var sumL = 0.0
        var sumR = 0.0
        var sumLR = 0.0
        val step = PI * 2.0 / (sampleRate * periodSec)
        repeat(frames) {
            advanceMode()
            val leftIn: Float
            val rightIn: Float
            if (channels == 1) {
                val sample = finite(inputBuffer.getFloat())
                leftIn = sample
                rightIn = sample
            } else {
                leftIn = finite(inputBuffer.getFloat())
                rightIn = finite(inputBuffer.getFloat())
            }
            if (activeMode == SpatialAudioMode.ORBIT) {
                orbitAzimuth = BinauralModel.wrapAzimuth(orbitAzimuth + orbitSign * step)
            }
            val (azLeft, azRight) = azimuthsFor(channels)
            if (it == 0) {
                prepareCoeffs(azLeft, leftSource)
                if (channels == 2) prepareCoeffs(azRight, rightSource)
            }
            val spatial = render(channels, leftIn, rightIn, azLeft, azRight)
            val dryL = leftIn.toDouble()
            val dryR = rightIn.toDouble()
            val mixedL = dryL * (1.0 - wet) + spatial.first * wet
            val mixedR = dryR * (1.0 - wet) + spatial.second * wet
            val limited = limitPair(mixedL, mixedR)
            output.putFloat(limited.first)
            output.putFloat(limited.second)
            if (channels == 2) {
                sumL += dryL * dryL
                sumR += dryR * dryR
                sumLR += dryL * dryR
            }
        }
        if (channels == 2) {
            val denom = sqrt(sumL * sumR)
            stereoCorrelation = if (denom > 1e-8) (sumLR / denom).coerceIn(-1.0, 1.0) else 1.0
        }
        finish(inputBuffer, output)
    }

    private fun syncControls() {
        val settings = AudioSettingsRepository.settings.value.sanitized()
        pendingMode = settings.spatialMode
        periodSec = settings.orbitPeriodSec.toDouble().coerceIn(30.0, 120.0)
        orbitSign = if (settings.orbitClockwise) 1.0 else -1.0
        strength = settings.orbitStrength.toDouble().coerceIn(0.0, 1.0)
        if (pendingMode != activeMode) {
            if (wet <= 0.01) {
                activeMode = pendingMode
                targetWet = wetFor(activeMode)
            } else {
                targetWet = 0.0
            }
        } else {
            targetWet = wetFor(activeMode)
        }
    }

    private fun advanceMode() {
        val stepped = advanceSpatialMode(activeMode, pendingMode, wet, targetWet, wetCoeff, strength)
        activeMode = stepped.active
        wet = stepped.wet
        targetWet = stepped.targetWet
    }

    private fun wetFor(mode: SpatialAudioMode): Double = wetTargetFor(mode, strength)

    private fun shouldBypass(): Boolean {
        return activeMode == SpatialAudioMode.OFF &&
            pendingMode == SpatialAudioMode.OFF &&
            wet < 0.0008 &&
            targetWet < 0.0008
    }

    private fun azimuthsFor(channels: Int): Pair<Double, Double> {
        val naturalHalf = if (stereoCorrelation > 0.98) RAD_10 else RAD_22
        return when (activeMode) {
            SpatialAudioMode.NATURAL -> if (channels == 1) {
                RAD_16 to RAD_16
            } else {
                -naturalHalf to naturalHalf
            }
            SpatialAudioMode.SURROUND -> if (channels == 1) {
                0.0 to 0.0
            } else {
                -RAD_50 to RAD_50
            }
            SpatialAudioMode.ORBIT -> if (channels == 1) {
                orbitAzimuth to orbitAzimuth
            } else {
                BinauralModel.wrapAzimuth(orbitAzimuth - RAD_28) to
                    BinauralModel.wrapAzimuth(orbitAzimuth + RAD_28)
            }
            SpatialAudioMode.OFF -> 0.0 to 0.0
        }
    }

    private fun render(
        channels: Int,
        left: Float,
        right: Float,
        azLeft: Double,
        azRight: Double,
    ): Pair<Double, Double> {
        if (activeMode == SpatialAudioMode.OFF) {
            return left.toDouble() to right.toDouble()
        }
        val fromLeft = leftSource.render(left, azLeft, sampleRate)
        val fromRight = if (channels == 2) {
            rightSource.render(right, azRight, sampleRate)
        } else {
            0.0 to 0.0
        }
        val scale = if (channels == 2) 0.72 else 1.0
        var outL = (fromLeft.first + fromRight.first) * scale
        var outR = (fromLeft.second + fromRight.second) * scale
        if (activeMode == SpatialAudioMode.SURROUND) {
            val mid = if (channels == 2) 0.5f * (left + right) else left
            val reflected = reflections(mid)
            outL += reflected.first
            outR += reflected.second
        }
        return outL to outR
    }

    private fun reflections(mid: Float): Pair<Double, Double> {
        val dull = reflectionLowpass.process(mid.toDouble()).toFloat()
        reflectionDelay.push(dull)
        val rate = sampleRate.toFloat()
        var left = 0.0
        var right = 0.0
        for (tap in REFLECTION_TAPS) {
            val itd = (BinauralModel.itdSeconds(tap.azimuth) * sampleRate).toFloat()
            val center = tap.delaySeconds * rate
            var sampleL = reflectionDelay.read((center + itd * 0.5f).coerceAtLeast(1f)).toDouble()
            var sampleR = reflectionDelay.read((center - itd * 0.5f).coerceAtLeast(1f)).toDouble()
            if (BinauralModel.rearAmount(tap.azimuth) > 0.75) {
                val notched = rearReflectionNotch.process(0.5 * (sampleL + sampleR))
                sampleL = notched
                sampleR = notched
            }
            left += sampleL * tap.gain * BinauralModel.earGain(tap.azimuth, -PI / 2.0)
            right += sampleR * tap.gain * BinauralModel.earGain(tap.azimuth, PI / 2.0)
        }
        return left to right
    }

    private fun limitPair(left: Double, right: Double): Pair<Float, Float> {
        val peak = max(abs(left), abs(right))
        if (peak > LIMIT_CEILING && peak.isFinite()) {
            val target = LIMIT_CEILING / peak
            if (target < outGain) outGain = target
        } else {
            outGain += (1.0 - outGain) * limitRelease
            if (outGain > 1.0) outGain = 1.0
        }
        val gain = if (outGain.isFinite()) outGain else 1.0
        return finite((left * gain).toFloat()) to finite((right * gain).toFloat())
    }

    private fun prepareCoeffs(azimuth: Double, source: SourcePath) {
        source.prepare(azimuth, sampleRate)
    }

    private fun writeDry(input: ByteBuffer, output: ByteBuffer, channels: Int, frames: Int) {
        if (channels == 1) {
            repeat(frames) {
                val sample = finite(input.getFloat())
                output.putFloat(sample)
                output.putFloat(sample)
            }
        } else {
            repeat(frames * 2) {
                output.putFloat(finite(input.getFloat()))
            }
        }
    }

    private fun passthrough(inputBuffer: ByteBuffer, channels: Int) {
        val frames = inputBuffer.remaining() / (channels * 4)
        val output = replaceOutputBuffer(frames * channels * 4)
        repeat(frames * channels) {
            output.putFloat(finite(inputBuffer.getFloat()))
        }
        finish(inputBuffer, output)
    }

    private fun finish(inputBuffer: ByteBuffer, output: ByteBuffer) {
        if (inputBuffer.hasRemaining()) {
            inputBuffer.position(inputBuffer.limit())
        }
        output.flip()
    }

    private fun finite(sample: Float): Float = if (sample.isFinite()) sample else 0f

    private class SourcePath {
        val delay = FractionalDelay(DELAY_CAPACITY)
        private val leftLowpass = OnePoleLowpass()
        private val rightLowpass = OnePoleLowpass()
        private val leftPinna = Biquad()
        private val rightPinna = Biquad()

        fun reset() {
            delay.clear()
            leftLowpass.reset()
            rightLowpass.reset()
            leftPinna.reset()
            rightPinna.reset()
        }

        fun prepare(azimuth: Double, sampleRate: Int) {
            val rate = sampleRate.toDouble()
            leftLowpass.setCutoff(BinauralModel.shadowCutoffHz(azimuth, -PI / 2.0), rate)
            rightLowpass.setCutoff(BinauralModel.shadowCutoffHz(azimuth, PI / 2.0), rate)
            val rear = BinauralModel.rearAmount(azimuth)
            val notchHz = 10_200.0 - 2_600.0 * rear
            val notchDb = -11.0 * rear
            val notch = designPeaking(sampleRate, notchHz, notchDb, q = 2.4)
            leftPinna.set(notch)
            rightPinna.set(notch)
        }

        fun render(sample: Float, azimuth: Double, sampleRate: Int): Pair<Double, Double> {
            delay.push(sample)
            val itdSamples = BinauralModel.itdSeconds(azimuth) * sampleRate
            val base = BinauralModel.BASE_DELAY_SAMPLES.toDouble()
            val leftDelay = (base + itdSamples * 0.5).toFloat().coerceAtLeast(1f)
            val rightDelay = (base - itdSamples * 0.5).toFloat().coerceAtLeast(1f)
            val gainL = BinauralModel.earGain(azimuth, -PI / 2.0)
            val gainR = BinauralModel.earGain(azimuth, PI / 2.0)
            val left = colored(delay.read(leftDelay).toDouble(), leftLowpass, leftPinna, gainL)
            val right = colored(delay.read(rightDelay).toDouble(), rightLowpass, rightPinna, gainR)
            val norm = sqrt(gainL * gainL + gainR * gainR).coerceAtLeast(1e-6)
            val makeup = (1.0 / norm).coerceIn(0.5, 2.2)
            return left * makeup to right * makeup
        }

        private fun colored(
            sample: Double,
            lowpass: OnePoleLowpass,
            pinna: Biquad,
            gain: Double,
        ): Double {
            val shadowed = lowpass.process(sample)
            val notched = pinna.process(shadowed)
            return notched * gain
        }
    }

    private companion object {
        const val DELAY_CAPACITY = 8192
        const val WET_SECONDS = 0.045
        const val LIMIT_RELEASE_SECONDS = 0.05
        const val LIMIT_CEILING = 0.98
        val RAD_10 = Math.toRadians(10.0)
        val RAD_16 = Math.toRadians(16.0)
        val RAD_22 = Math.toRadians(22.0)
        val RAD_28 = Math.toRadians(28.0)
        val RAD_50 = Math.toRadians(50.0)
        val REFLECTION_TAPS = arrayOf(
            ReflectionTap(0.009f, 0.10, Math.toRadians(110.0)),
            ReflectionTap(0.016f, 0.07, Math.toRadians(-125.0)),
            ReflectionTap(0.028f, 0.045, Math.toRadians(180.0)),
        )
    }

    private data class ReflectionTap(
        val delaySeconds: Float,
        val gain: Double,
        val azimuth: Double,
    )
}

internal data class SpatialModeStep(
    val active: SpatialAudioMode,
    val wet: Double,
    val targetWet: Double,
)

/** Fade the current mode out before the pending mode fades in. */
internal fun advanceSpatialMode(
    active: SpatialAudioMode,
    pending: SpatialAudioMode,
    wet: Double,
    targetWet: Double,
    wetCoeff: Double,
    orbitStrength: Double,
): SpatialModeStep {
    var nextActive = active
    var nextTarget = targetWet
    if (pending != nextActive && wet <= 0.01) {
        nextActive = pending
        nextTarget = wetTargetFor(pending, orbitStrength)
    }
    var nextWet = wet + (nextTarget - wet) * wetCoeff
    if (abs(nextWet - nextTarget) < 0.0005) nextWet = nextTarget
    nextWet = nextWet.coerceIn(0.0, 1.0)
    return SpatialModeStep(nextActive, nextWet, nextTarget)
}

internal fun wetTargetFor(mode: SpatialAudioMode, strength: Double): Double = when (mode) {
    SpatialAudioMode.OFF -> 0.0
    SpatialAudioMode.NATURAL -> 0.38
    SpatialAudioMode.SURROUND -> 0.58
    SpatialAudioMode.ORBIT -> 0.18 + 0.82 * strength.coerceIn(0.0, 1.0)
}
