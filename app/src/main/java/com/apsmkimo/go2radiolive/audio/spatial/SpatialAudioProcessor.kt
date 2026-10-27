package com.apsmkimo.go2radiolive.audio.spatial

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.audio.dsp.Biquad
import com.apsmkimo.go2radiolive.audio.dsp.BiquadCoeffs
import com.apsmkimo.go2radiolive.audio.dsp.designPeaking
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One spatial renderer for the whole stream. Modes are exclusive: the previous
 * mode is faded out before the next mode fades in, so two algorithms never
 * process the same samples.
 *
 * While a spatial mode is on, the dry signal uses the same
 * [BinauralModel.BASE_DELAY_SAMPLES] center delay as the binaural path so the
 * mix does not comb. Off does not use that delay. Mono and stereo both stay
 * active and always emit stereo, so turning a mode on does not wait for the
 * player to rebuild the sink. Stereo Off copies both channels with no delay.
 * Mono Off duplicates the sample to both channels with no delay.
 * Entering or leaving Off crossfades the direct and delayed paths (~30 ms)
 * after the delay line is primed, so the switch does not click.
 * Natural width follows correlation continuously; it is not a step between
 * two angles. Mono uses the same left/right pair as stereo, plus a common
 * room tail so a phone speaker (left + right) still hears the space.
 *
 * More than two channels stay inactive and are left untouched.
 */
internal class SpatialAudioProcessor : BaseAudioProcessor() {
    private val leftSource = SourcePath()
    private val rightSource = SourcePath()
    private val reflections = ReflectionBus()
    // SMCPKG_SUPPORT>>>Cursor116
    private val commonRoom = CommonRoom()
    private val haas = FractionalDelay(DELAY_CAPACITY)
    private var spaceArmed = false
    // SMCPKG_SUPPORT<<<Cursor116

    private var sampleRate = 48_000
    private var wetCoeff = 0.01
    private var limitRelease = 0.01
    private var corrCoeff = 0.01
    private var alignCoeff = 0.01
    private var wet = 0.0
    private var align = 0.0
    private var alignTarget = 0.0
    private var priming = 0
    // SMCPKG_SUPPORT>>>Cursor116
    // Priming used to re-arm whenever align was still near 0, and align only
    // advanced after priming, so the spatial path never ran.
    private var primeDone = false
    // SMCPKG_SUPPORT<<<Cursor116
    private var outGain = 1.0
    private var targetWet = 0.0
    private var activeMode = SpatialAudioMode.OFF
    private var pendingMode = SpatialAudioMode.OFF
    private var orbitAzimuth = 0.0
    private var orbitSign = 1.0
    private var periodSec = 60.0
    private var strength = 0.6
    private var heldCorrelation = 0.0
    private var smoothedCorrelation = 0.0
    private var naturalHalf = naturalHalfRadians(0.0)
    private var azLeft = 0.0
    private var azRight = 0.0
    private var configuredChannels = 0
    private var bypassMono = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        configuredChannels = inputAudioFormat.channelCount
        SpatialRoute.inputChannels = inputAudioFormat.channelCount
        // SMCPKG_SUPPORT>>>Cursor116
        // val spatialOn = AudioSettingsRepository.settings.value.spatialMode != SpatialAudioMode.OFF
        // val outChannels = when (inputAudioFormat.channelCount) {
        //     1 -> if (spatialOn) 2 else 1
        //     2 -> 2
        //     else -> inputAudioFormat.channelCount
        // }
        // Mono Off used to drop this processor until the stream was reloaded.
        // Keep one stereo format for one or two input channels so toggles apply
        // on the next buffer.
        val outChannels = when (inputAudioFormat.channelCount) {
            1, 2 -> 2
            else -> inputAudioFormat.channelCount
        }
        // SMCPKG_SUPPORT>>>Cursor071
        // Mono Off is inactive so the pipeline never feeds this processor.
        // bypassMono = inputAudioFormat.channelCount == 1 && outChannels == 1
        bypassMono = false
        // SMCPKG_SUPPORT<<<Cursor071
        // SMCPKG_SUPPORT<<<Cursor116
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
        corrCoeff = 1.0 - exp(-1.0 / (sampleRate * CORR_SECONDS))
        alignCoeff = 1.0 - exp(-1.0 / (sampleRate * ALIGN_SECONDS))
        wet = 0.0
        align = 0.0
        alignTarget = 0.0
        priming = 0
        // SMCPKG_SUPPORT>>>Cursor116
        primeDone = false
        // SMCPKG_SUPPORT<<<Cursor116
        outGain = 1.0
        heldCorrelation = 0.0
        smoothedCorrelation = 0.0
        naturalHalf = naturalHalfRadians(0.0)
        leftSource.reset()
        rightSource.reset()
        reflections.reset(sampleRate)
        // SMCPKG_SUPPORT>>>Cursor116
        commonRoom.reset(sampleRate)
        haas.clear()
        spaceArmed = false
        // SMCPKG_SUPPORT<<<Cursor116
        orbitAzimuth = 0.0
        leftSource.prepare(0.0, sampleRate)
        rightSource.prepare(0.0, sampleRate)
    }

    override fun isActive(): Boolean {
        // onConfigure runs before this check. The flag stays fixed until the
        // sink configures again, because flush does not rebuild the format.
        if (configuredChannels <= 0 || configuredChannels > 2) return false
        return !bypassMono
    }

    override fun onReset() {
        super.onReset()
        onFlush()
        configuredChannels = 0
        bypassMono = false
        activeMode = SpatialAudioMode.OFF
        pendingMode = SpatialAudioMode.OFF
        targetWet = 0.0
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
        updateAlignTarget()
        val frames = inputBuffer.remaining() / (channels * 4)
        val outChannels = outputAudioFormat.channelCount.coerceAtLeast(1)
        val output = replaceOutputBuffer(frames * outChannels * 4)
        if (fullyDirect()) {
            outGain = 1.0
            priming = 0
            writeDirect(inputBuffer, output, channels, outChannels, frames)
            finish(inputBuffer, output)
            return
        }

        var sumL = 0.0
        var sumR = 0.0
        var sumLR = 0.0
        val step = PI * 2.0 / (sampleRate * periodSec)
        var index = 0
        while (index < frames) {
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
            updateAlignTarget()
            // SMCPKG_SUPPORT>>>Cursor116
            // if (alignTarget > 0.5 && align < 0.0008 && priming == 0) {
            //     priming = BinauralModel.BASE_DELAY_SAMPLES
            // }
            if (alignTarget < 0.0008 && align < 0.0008 && wet < 0.01) {
                primeDone = false
            }
            if (alignTarget > 0.5 && align < 0.0008 && priming == 0 && !primeDone) {
                priming = BinauralModel.BASE_DELAY_SAMPLES
            }
            // SMCPKG_SUPPORT<<<Cursor116
            if (priming > 0) {
                leftSource.pushOnly(leftIn)
                // SMCPKG_SUPPORT>>>Cursor116
                // if (channels == 2) rightSource.pushOnly(rightIn)
                rightSource.pushOnly(rightIn)
                // SMCPKG_SUPPORT<<<Cursor116
                writeDirectSample(output, leftIn, if (channels == 2) rightIn else leftIn, channels, outChannels)
                priming--
                // SMCPKG_SUPPORT>>>Cursor116
                if (priming == 0) primeDone = true
                // SMCPKG_SUPPORT<<<Cursor116
                index++
                continue
            }
            align += (alignTarget - align) * alignCoeff
            if (abs(align - alignTarget) < 0.0005) align = alignTarget
            if (activeMode == SpatialAudioMode.ORBIT) {
                orbitAzimuth = BinauralModel.wrapAzimuth(orbitAzimuth + orbitSign * step)
            }
            if (activeMode == SpatialAudioMode.NATURAL && channels == 2) {
                smoothedCorrelation += (heldCorrelation - smoothedCorrelation) * corrCoeff
                naturalHalf = naturalHalfRadians(smoothedCorrelation)
            }
            azLeft = sourceAzimuth(activeMode, channels, orbitAzimuth, naturalHalf, right = false)
            azRight = sourceAzimuth(activeMode, channels, orbitAzimuth, naturalHalf, right = true)
            // SMCPKG_SUPPORT>>>Cursor116
            // Mono Natural/Surround is a left/right pair. Mono Orbit stays one moving source.
            val pair = channels == 2 ||
                activeMode == SpatialAudioMode.NATURAL ||
                activeMode == SpatialAudioMode.SURROUND
            // SMCPKG_SUPPORT<<<Cursor116
            if (index == 0) {
                leftSource.prepare(azLeft, sampleRate)
                // SMCPKG_SUPPORT>>>Cursor116
                // if (channels == 2) rightSource.prepare(azRight, sampleRate)
                if (pair) rightSource.prepare(azRight, sampleRate)
                // SMCPKG_SUPPORT<<<Cursor116
            }
            leftSource.render(leftIn, azLeft, sampleRate)
            var spatL = leftSource.renderedL
            var spatR = leftSource.renderedR
            val delayedL = leftSource.readDry().toDouble()
            val delayedR: Double
            // SMCPKG_SUPPORT>>>Cursor116
            // if (channels == 2) {
            if (pair) {
            // SMCPKG_SUPPORT<<<Cursor116
                rightSource.render(rightIn, azRight, sampleRate)
                spatL = (spatL + rightSource.renderedL) * STEREO_SCALE
                spatR = (spatR + rightSource.renderedR) * STEREO_SCALE
                delayedR = rightSource.readDry().toDouble()
                if (channels == 2) {
                    sumL += leftIn * leftIn
                    sumR += rightIn * rightIn
                    sumLR += leftIn * rightIn
                }
            } else {
                delayedR = delayedL
            }
            val dryL = bypassMix(leftIn.toDouble(), delayedL, align)
            val dryR = bypassMix(rightIn.toDouble(), delayedR, align)
            // SMCPKG_SUPPORT>>>Cursor116
            // if (activeMode == SpatialAudioMode.SURROUND) {
            //     val mid = if (channels == 2) 0.5f * (leftIn + rightIn) else leftIn
            //     reflections.process(mid, sampleRate)
            //     spatL += reflections.outL
            //     spatR += reflections.outR
            // }
            if (activeMode == SpatialAudioMode.OFF && wet < 0.01) {
                if (spaceArmed) {
                    commonRoom.reset(sampleRate)
                    haas.clear()
                    spaceArmed = false
                }
            } else if (activeMode != SpatialAudioMode.OFF) {
                spaceArmed = true
                val mid = if (channels == 2) 0.5f * (leftIn + rightIn) else leftIn
                val wobble = if (activeMode == SpatialAudioMode.ORBIT) sin(orbitAzimuth) * 0.004 else 0.0
                commonRoom.process(mid, sampleRate, wobble)
                val room = commonRoom.out * roomScale(activeMode)
                spatL += room
                spatR += room
                haas.push(spatR.toFloat())
                val smeared = haas.read(haasDelaySamples(activeMode, sampleRate)).toDouble()
                val blend = haasBlend(activeMode)
                spatR = spatR * (1.0 - blend) + smeared * blend
                if (activeMode == SpatialAudioMode.SURROUND) {
                    reflections.process(mid, sampleRate)
                    spatL += reflections.outL
                    spatR += reflections.outR
                }
            }
            // SMCPKG_SUPPORT<<<Cursor116
            val keep = 1.0 - wet
            val mixedL = dryL * keep + spatL * wet
            val mixedR = dryR * keep + spatR * wet
            if (outChannels == 1) {
                writeLimitedMono(output, mixedL)
            } else {
                writeLimited(output, mixedL, mixedR)
            }
            index++
        }
        if (channels == 2) {
            val denom = sqrt(sumL * sumR)
            val raw = if (denom > 1e-8) (sumLR / denom).coerceIn(-1.0, 1.0) else 1.0
            heldCorrelation = holdCorrelation(heldCorrelation, raw)
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
                targetWet = wetTargetFor(activeMode, strength)
            } else {
                targetWet = 0.0
            }
        } else {
            targetWet = wetTargetFor(activeMode, strength)
        }
    }

    private fun advanceMode() {
        if (pendingMode != activeMode && wet <= 0.01) {
            activeMode = pendingMode
            targetWet = wetTargetFor(activeMode, strength)
        }
        wet += (targetWet - wet) * wetCoeff
        if (abs(wet - targetWet) < 0.0005) wet = targetWet
        if (wet < 0.0) wet = 0.0
        if (wet > 1.0) wet = 1.0
    }

    private fun updateAlignTarget() {
        alignTarget = if (activeMode == SpatialAudioMode.OFF && pendingMode == SpatialAudioMode.OFF && wet <= 0.01) {
            0.0
        } else {
            1.0
        }
    }

    private fun fullyDirect(): Boolean {
        return activeMode == SpatialAudioMode.OFF &&
            pendingMode == SpatialAudioMode.OFF &&
            wet < 0.0008 &&
            targetWet < 0.0008 &&
            align < 0.0008 &&
            alignTarget < 0.0008 &&
            priming == 0
    }

    private fun writeLimited(output: ByteBuffer, left: Double, right: Double) {
        val peak = max(abs(left), abs(right))
        if (peak > LIMIT_CEILING && peak.isFinite()) {
            val target = LIMIT_CEILING / peak
            if (target < outGain) outGain = target
        } else {
            outGain += (1.0 - outGain) * limitRelease
            if (outGain > 1.0) outGain = 1.0
        }
        val gain = if (outGain.isFinite()) outGain else 1.0
        output.putFloat(finite((left * gain).toFloat()))
        output.putFloat(finite((right * gain).toFloat()))
    }

    private fun writeDirect(
        input: ByteBuffer,
        output: ByteBuffer,
        channels: Int,
        outChannels: Int,
        frames: Int,
    ) {
        var index = 0
        while (index < frames) {
            if (channels == 1) {
                val sample = finite(input.getFloat())
                writeDirectSample(output, sample, sample, channels, outChannels)
            } else {
                val left = finite(input.getFloat())
                val right = finite(input.getFloat())
                writeDirectSample(output, left, right, channels, outChannels)
            }
            index++
        }
    }

    private fun writeDirectSample(
        output: ByteBuffer,
        left: Float,
        right: Float,
        channels: Int,
        outChannels: Int,
    ) {
        if (outChannels == 1) {
            output.putFloat(if (channels == 1) left else ((left + right) * 0.5f))
        } else if (channels == 1) {
            output.putFloat(left)
            output.putFloat(left)
        } else {
            output.putFloat(left)
            output.putFloat(right)
        }
    }

    private fun writeLimitedMono(output: ByteBuffer, sample: Double) {
        val peak = abs(sample)
        if (peak > LIMIT_CEILING && peak.isFinite()) {
            val target = LIMIT_CEILING / peak
            if (target < outGain) outGain = target
        } else {
            outGain += (1.0 - outGain) * limitRelease
            if (outGain > 1.0) outGain = 1.0
        }
        val gain = if (outGain.isFinite()) outGain else 1.0
        output.putFloat(finite((sample * gain).toFloat()))
    }

    private fun passthrough(inputBuffer: ByteBuffer, channels: Int) {
        val frames = inputBuffer.remaining() / (channels * 4)
        val output = replaceOutputBuffer(frames * channels * 4)
        var index = 0
        val count = frames * channels
        while (index < count) {
            output.putFloat(finite(inputBuffer.getFloat()))
            index++
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
        var renderedL = 0.0
        var renderedR = 0.0

        fun reset() {
            delay.clear()
            leftLowpass.reset()
            rightLowpass.reset()
            leftPinna.reset()
            rightPinna.reset()
            renderedL = 0.0
            renderedR = 0.0
        }

        fun pushOnly(sample: Float) {
            delay.push(sample)
        }

        fun readDry(): Float = delay.read(alignedBaseDelaySamples())

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

        fun render(sample: Float, azimuth: Double, sampleRate: Int) {
            delay.push(sample)
            val itdSamples = BinauralModel.itdSeconds(azimuth) * sampleRate
            val leftDelay = wetEarDelaySamples(itdSamples, 0.5)
            val rightDelay = wetEarDelaySamples(itdSamples, -0.5)
            val gainL = BinauralModel.earGain(azimuth, -PI / 2.0)
            val gainR = BinauralModel.earGain(azimuth, PI / 2.0)
            val left = colored(delay.read(leftDelay).toDouble(), leftLowpass, leftPinna, gainL)
            val right = colored(delay.read(rightDelay).toDouble(), rightLowpass, rightPinna, gainR)
            val norm = sqrt(gainL * gainL + gainR * gainR).coerceAtLeast(1e-6)
            val makeup = (1.0 / norm).coerceIn(0.5, 2.2)
            renderedL = left * makeup
            renderedR = right * makeup
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

    private class ReflectionBus {
        private val delay = FractionalDelay(DELAY_CAPACITY)
        private val lowpass = OnePoleLowpass()
        private val notches = Array(TAP_COUNT) { EarSplitFilter() }
        private val delaySeconds = floatArrayOf(0.009f, 0.016f, 0.028f)
        private val gains = doubleArrayOf(0.10, 0.07, 0.045)
        private val azimuths = doubleArrayOf(
            Math.toRadians(110.0),
            Math.toRadians(-125.0),
            Math.toRadians(180.0),
        )
        private val earGainL = DoubleArray(TAP_COUNT)
        private val earGainR = DoubleArray(TAP_COUNT)
        private val useNotch = BooleanArray(TAP_COUNT)
        private val itdSamples = FloatArray(TAP_COUNT)
        var outL = 0.0
        var outR = 0.0

        init {
            var index = 0
            while (index < TAP_COUNT) {
                earGainL[index] = BinauralModel.earGain(azimuths[index], -PI / 2.0)
                earGainR[index] = BinauralModel.earGain(azimuths[index], PI / 2.0)
                useNotch[index] = BinauralModel.rearAmount(azimuths[index]) > 0.75
                index++
            }
        }

        fun reset(sampleRate: Int) {
            delay.clear()
            lowpass.reset()
            lowpass.setCutoff(2_800.0, sampleRate.toDouble())
            val notch = designPeaking(sampleRate, 7_600.0, -8.0, q = 2.2)
            var index = 0
            while (index < TAP_COUNT) {
                itdSamples[index] = (BinauralModel.itdSeconds(azimuths[index]) * sampleRate).toFloat()
                notches[index].reset()
                if (useNotch[index]) notches[index].set(notch)
                index++
            }
            outL = 0.0
            outR = 0.0
        }

        fun process(mid: Float, sampleRate: Int) {
            val dull = lowpass.process(mid.toDouble()).toFloat()
            delay.push(dull)
            val rate = sampleRate.toFloat()
            var accL = 0.0
            var accR = 0.0
            var index = 0
            while (index < TAP_COUNT) {
                val center = delaySeconds[index] * rate
                val itd = itdSamples[index]
                var sampleL = delay.read((center + itd * 0.5f).coerceAtLeast(1f)).toDouble()
                var sampleR = delay.read((center - itd * 0.5f).coerceAtLeast(1f)).toDouble()
                if (useNotch[index]) {
                    val notch = notches[index]
                    notch.process(sampleL, sampleR)
                    sampleL = notch.outL
                    sampleR = notch.outR
                }
                val gain = gains[index]
                accL += sampleL * gain * earGainL[index]
                accR += sampleR * gain * earGainR[index]
                index++
            }
            outL = accL
            outR = accR
        }
    }

    // SMCPKG_SUPPORT>>>Cursor116
    /**
     * Early reflections that are identical on both channels. A phone speaker
     * sums left and right, which cancels a binaural image; this tail remains.
     */
    private class CommonRoom {
        private val delay = FractionalDelay(DELAY_CAPACITY)
        private val lowpass = OnePoleLowpass()
        private val tapsSec = floatArrayOf(0.017f, 0.031f, 0.047f)
        private val gains = doubleArrayOf(0.40, 0.26, 0.16)
        var out = 0.0

        fun reset(sampleRate: Int) {
            delay.clear()
            lowpass.reset()
            lowpass.setCutoff(3_400.0, sampleRate.toDouble())
            out = 0.0
        }

        fun process(mid: Float, sampleRate: Int, wobbleSec: Double) {
            val dull = lowpass.process(mid.toDouble()).toFloat()
            delay.push(dull)
            val rate = sampleRate.toFloat()
            val wobble = wobbleSec.toFloat()
            var acc = 0.0
            var index = 0
            while (index < tapsSec.size) {
                val center = (tapsSec[index] + wobble) * rate
                acc += delay.read(center.coerceAtLeast(1f)).toDouble() * gains[index]
                index++
            }
            out = acc
        }
    }
    // SMCPKG_SUPPORT<<<Cursor116

    private companion object {
        const val DELAY_CAPACITY = 8192
        const val WET_SECONDS = 0.045
        const val LIMIT_RELEASE_SECONDS = 0.05
        const val CORR_SECONDS = 0.15
        const val ALIGN_SECONDS = 0.03
        const val LIMIT_CEILING = 0.98
        const val STEREO_SCALE = 0.72
        const val TAP_COUNT = 3
    }
}

/** One biquad per ear so a rear reflection keeps its interaural delay. */
internal class EarSplitFilter {
    private val left = Biquad()
    private val right = Biquad()
    var outL = 0.0
    var outR = 0.0

    fun set(coeffs: BiquadCoeffs) {
        left.set(coeffs)
        right.set(coeffs)
    }

    fun reset() {
        left.reset()
        right.reset()
        outL = 0.0
        outR = 0.0
    }

    fun process(leftSample: Double, rightSample: Double) {
        outL = left.process(leftSample)
        outR = right.process(rightSample)
    }
}

internal object SpatialRoute {
    @Volatile
    var inputChannels: Int = 0
}

/** 0 keeps the direct sample. 1 uses the delayed sample that matches the binaural path. */
internal fun bypassMix(direct: Double, delayed: Double, align: Double): Double {
    val mix = align.coerceIn(0.0, 1.0)
    return direct * (1.0 - mix) + delayed * mix
}

internal fun alignedBaseDelaySamples(): Float = BinauralModel.BASE_DELAY_SAMPLES.toFloat()

/** Ear sign is +0.5 for the left ear and -0.5 for the right ear. */
internal fun wetEarDelaySamples(itdSamples: Double, earSign: Double): Float {
    val delay = BinauralModel.BASE_DELAY_SAMPLES + itdSamples * earSign
    return delay.toFloat().coerceAtLeast(1f)
}

internal fun naturalHalfRadians(correlation: Double): Double {
    val t = ((correlation - 0.80) / 0.20).coerceIn(0.0, 1.0)
    // SMCPKG_SUPPORT>>>Cursor116
    // ±10° on a correlated radio stream is inaudible, especially after the
    // speaker sums both channels. Keep the change continuous, but never narrower
    // than about ±48° and open out to about ±76° when the channels differ.
    // val wide = Math.toRadians(22.0)
    // val narrow = Math.toRadians(10.0)
    val wide = Math.toRadians(76.0)
    val narrow = Math.toRadians(48.0)
    // SMCPKG_SUPPORT<<<Cursor116
    return wide + (narrow - wide) * t
}

internal fun holdCorrelation(previous: Double, raw: Double, band: Double = 0.03): Double {
    return if (abs(raw - previous) >= band) raw else previous
}

internal fun sourceAzimuth(
    mode: SpatialAudioMode,
    channels: Int,
    orbitAzimuth: Double,
    naturalHalf: Double,
    right: Boolean,
): Double {
    val sign = if (right) 1.0 else -1.0
    // SMCPKG_SUPPORT>>>Cursor116
    // return when (mode) {
    //     SpatialAudioMode.NATURAL -> if (channels == 1) 0.0 else sign * naturalHalf
    //     SpatialAudioMode.SURROUND -> if (channels == 1) 0.0 else sign * Math.toRadians(50.0)
    //     SpatialAudioMode.ORBIT -> if (channels == 1) {
    //         orbitAzimuth
    //     } else {
    //         BinauralModel.wrapAzimuth(orbitAzimuth + sign * Math.toRadians(28.0))
    //     }
    //     SpatialAudioMode.OFF -> 0.0
    // }
    return when (mode) {
        SpatialAudioMode.NATURAL -> sign * naturalHalf
        SpatialAudioMode.SURROUND -> sign * Math.toRadians(62.0)
        SpatialAudioMode.ORBIT -> if (channels == 1) {
            orbitAzimuth
        } else {
            BinauralModel.wrapAzimuth(orbitAzimuth + sign * Math.toRadians(28.0))
        }
        SpatialAudioMode.OFF -> 0.0
    }
    // SMCPKG_SUPPORT<<<Cursor116
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
    // SMCPKG_SUPPORT>>>Cursor116
    // SpatialAudioMode.NATURAL -> 0.38
    // SpatialAudioMode.SURROUND -> 0.58
    // SpatialAudioMode.ORBIT -> 0.18 + 0.82 * strength.coerceIn(0.0, 1.0)
    SpatialAudioMode.NATURAL -> 0.62
    SpatialAudioMode.SURROUND -> 0.82
    SpatialAudioMode.ORBIT -> 0.30 + 0.60 * strength.coerceIn(0.0, 1.0)
    // SMCPKG_SUPPORT<<<Cursor116
}

/** How much of the common room is mixed in. The room itself is already modest. */
internal fun roomScale(mode: SpatialAudioMode): Double = when (mode) {
    SpatialAudioMode.NATURAL -> 1.0
    SpatialAudioMode.SURROUND -> 1.0
    SpatialAudioMode.ORBIT -> 0.90
    SpatialAudioMode.OFF -> 0.0
}

/**
 * Blend of the right channel with a short delayed copy of itself. Headphones
 * hear that as width. A speaker that adds both channels still keeps the
 * undelayed left channel plus the delayed copy.
 */
internal fun haasBlend(mode: SpatialAudioMode): Double = when (mode) {
    SpatialAudioMode.NATURAL -> 0.42
    SpatialAudioMode.SURROUND -> 0.55
    SpatialAudioMode.ORBIT -> 0.48
    SpatialAudioMode.OFF -> 0.0
}

internal fun haasDelaySamples(mode: SpatialAudioMode, sampleRate: Int): Float {
    val milliseconds = when (mode) {
        SpatialAudioMode.SURROUND -> 12.0
        SpatialAudioMode.ORBIT -> 10.0
        else -> 8.0
    }
    return (milliseconds * 0.001 * sampleRate).toFloat().coerceIn(1f, 4_000f)
}
