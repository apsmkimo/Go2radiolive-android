package com.apsmkimo.go2radiolive.audio.dsp

import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Smoothed DSP parameters published once per audio buffer from the EQ processor
 * (the first processor in the chain). Later processors only read [frame].
 */
internal data class DspFrame(
    val headroom: Double,
    val bass: BiquadCoeffs,
    val low: BiquadCoeffs,
    val mid: BiquadCoeffs,
    val high: BiquadCoeffs,
    val voiceLowCut: BiquadCoeffs,
    val voicePresence: BiquadCoeffs,
    val voiceMix: Double,
    val surroundMix: Double,
    val balance: Double,
    val gainL: Double,
    val gainR: Double,
    val eqActive: Boolean,
    val processingActive: Boolean,
) {
    companion object {
        val NEUTRAL = DspFrame(
            headroom = 1.0,
            bass = BiquadCoeffs.UNITY,
            low = BiquadCoeffs.UNITY,
            mid = BiquadCoeffs.UNITY,
            high = BiquadCoeffs.UNITY,
            voiceLowCut = BiquadCoeffs.UNITY,
            voicePresence = BiquadCoeffs.UNITY,
            voiceMix = 0.0,
            surroundMix = 0.0,
            balance = 0.0,
            gainL = 1.0,
            gainR = 1.0,
            eqActive = false,
            processingActive = false,
        )
    }
}

internal object DspRuntime {
    @Volatile
    var frame: DspFrame = DspFrame.NEUTRAL
        private set

    private var lastAdvanceNs = 0L
    private var smoothedBass = 0.0
    private var smoothedLow = 0.0
    private var smoothedMid = 0.0
    private var smoothedHigh = 0.0
    private var smoothedBalance = 0.0
    private var smoothedVoice = 0.0
    private var smoothedSurround = 0.0

    private var designedRate = 0
    private var designedBass = Double.NaN
    private var designedLow = Double.NaN
    private var designedMid = Double.NaN
    private var designedHigh = Double.NaN
    private var bassCoeff = BiquadCoeffs.UNITY
    private var lowCoeff = BiquadCoeffs.UNITY
    private var midCoeff = BiquadCoeffs.UNITY
    private var highCoeff = BiquadCoeffs.UNITY

    private var voiceRate = 0
    private var voiceLowCut = BiquadCoeffs.UNITY
    private var voicePresence = BiquadCoeffs.UNITY

    fun advance(sampleRate: Int) {
        val target = AudioSettingsRepository.settings.value.sanitized()
        val now = System.nanoTime()
        if (lastAdvanceNs == 0L) {
            snap(target)
        } else {
            val dt = ((now - lastAdvanceNs) / 1_000_000_000.0).coerceIn(0.0, 0.1)
            val k = 1.0 - exp(-dt / 0.05)
            approach(target, k)
        }
        lastAdvanceNs = now
        val rate = sampleRate.coerceAtLeast(8_000)
        ensureEq(rate)
        ensureVoice(rate)
        frame = publish()
    }

    private fun snap(target: com.apsmkimo.go2radiolive.domain.model.AudioSettings) {
        smoothedBass = target.bassDb.toDouble()
        smoothedLow = target.lowDb.toDouble()
        smoothedMid = target.midDb.toDouble()
        smoothedHigh = target.highDb.toDouble()
        smoothedBalance = target.balance.toDouble()
        smoothedVoice = if (target.voiceEnhancement) 1.0 else 0.0
        smoothedSurround = if (target.virtualSurround) 1.0 else 0.0
    }

    private fun approach(target: com.apsmkimo.go2radiolive.domain.model.AudioSettings, k: Double) {
        smoothedBass = move(smoothedBass, target.bassDb.toDouble(), k, 0.01)
        smoothedLow = move(smoothedLow, target.lowDb.toDouble(), k, 0.01)
        smoothedMid = move(smoothedMid, target.midDb.toDouble(), k, 0.01)
        smoothedHigh = move(smoothedHigh, target.highDb.toDouble(), k, 0.01)
        smoothedBalance = move(smoothedBalance, target.balance.toDouble(), k, 0.002)
        val voiceTarget = if (target.voiceEnhancement) 1.0 else 0.0
        val surroundTarget = if (target.virtualSurround) 1.0 else 0.0
        smoothedVoice = move(smoothedVoice, voiceTarget, k, 0.002)
        smoothedSurround = move(smoothedSurround, surroundTarget, k, 0.002)
    }

    private fun move(current: Double, target: Double, k: Double, snapDistance: Double): Double {
        val next = current + (target - current) * k
        return if (abs(next - target) < snapDistance) target else next
    }

    private fun ensureEq(rate: Int) {
        if (designedRate == rate &&
            abs(designedBass - smoothedBass) < 0.02 &&
            abs(designedLow - smoothedLow) < 0.02 &&
            abs(designedMid - smoothedMid) < 0.02 &&
            abs(designedHigh - smoothedHigh) < 0.02
        ) {
            return
        }
        designedRate = rate
        designedBass = smoothedBass
        designedLow = smoothedLow
        designedMid = smoothedMid
        designedHigh = smoothedHigh
        // SMCPKG_SUPPORT>>>Cursor030
        // bassCoeff = designLowShelf(rate, 60.0, smoothedBass)
        // Low shelf (RBJ cookbook, shelf slope S = 1). Corner was 60 Hz; headphones lose
        // energy that low, so the corner is 70 Hz. Peaking Q is not used for this shelf.
        bassCoeff = designLowShelf(rate, 70.0, smoothedBass)
        // SMCPKG_SUPPORT<<<Cursor030
        lowCoeff = designPeaking(rate, 250.0, smoothedLow, q = 1.0)
        midCoeff = designPeaking(rate, 1_000.0, smoothedMid, q = 1.0)
        highCoeff = designHighShelf(rate, 8_000.0, smoothedHigh)
    }

    private fun ensureVoice(rate: Int) {
        if (voiceRate == rate) return
        voiceRate = rate
        // Mild cut under 150 Hz plus a wide presence peak centered at 2 kHz (about 1–4 kHz).
        voiceLowCut = designLowShelf(rate, 150.0, -3.5)
        voicePresence = designPeaking(rate, 2_000.0, 4.5, q = 0.65)
    }

    private fun publish(): DspFrame {
        val eqActive = abs(smoothedBass) > 0.04 || abs(smoothedLow) > 0.04 ||
            abs(smoothedMid) > 0.04 || abs(smoothedHigh) > 0.04
        val processingActive = eqActive || smoothedVoice > 0.004 ||
            smoothedSurround > 0.004 || abs(smoothedBalance) > 0.004
        val theta = (smoothedBalance.coerceIn(-1.0, 1.0) + 1.0) * (PI / 4.0)
        val gainL = cos(theta) * SQRT_2
        val gainR = sin(theta) * SQRT_2
        // SMCPKG_SUPPORT>>>Cursor031
        // Global pre-gain tracked the positive boost and made Bass feel quiet.
        // val positives = doubleArrayOf(smoothedBass, smoothedLow, smoothedMid, smoothedHigh)
        //     .filter { it > 0.0 }
        // val eqBoost = if (positives.isEmpty()) {
        //     0.0
        // } else {
        //     val max = positives.max()
        //     val rest = positives.sum() - max
        //     max + 0.45 * rest
        // }
        // val boost = (eqBoost + 4.5 * smoothedVoice + 4.0 * smoothedSurround + 3.0 * abs(smoothedBalance))
        //     .coerceIn(0.0, 24.0)
        // val headroom = 10.0.pow(-boost / 20.0)
        val headroom = 1.0
        // SMCPKG_SUPPORT<<<Cursor031
        return DspFrame(
            headroom = headroom,
            bass = bassCoeff,
            low = lowCoeff,
            mid = midCoeff,
            high = highCoeff,
            voiceLowCut = voiceLowCut,
            voicePresence = voicePresence,
            voiceMix = smoothedVoice.coerceIn(0.0, 1.0),
            surroundMix = smoothedSurround.coerceIn(0.0, 1.0),
            balance = smoothedBalance.coerceIn(-1.0, 1.0),
            gainL = gainL,
            gainR = gainR,
            eqActive = eqActive,
            processingActive = processingActive,
        )
    }
}

private const val SQRT_2 = 1.4142135623730951
