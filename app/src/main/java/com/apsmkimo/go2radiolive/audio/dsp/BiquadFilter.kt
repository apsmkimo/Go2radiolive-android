package com.apsmkimo.go2radiolive.audio.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct-form-I biquad. Coefficients follow the public-domain RBJ Audio EQ Cookbook
 * (Robert Bristow-Johnson): lowshelf, highshelf, and peaking equalizer.
 */
internal data class BiquadCoeffs(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    companion object {
        val UNITY = BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0)
    }
}

internal class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun set(coeffs: BiquadCoeffs) {
        b0 = coeffs.b0
        b1 = coeffs.b1
        b2 = coeffs.b2
        a1 = coeffs.a1
        a2 = coeffs.a2
    }

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = if (y.isFinite()) y else 0.0
        if (y1 > -1e-20 && y1 < 1e-20) y1 = 0.0
        return y1
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }
}

internal fun designLowShelf(sampleRate: Int, frequencyHz: Double, gainDb: Double): BiquadCoeffs {
    if (abs(gainDb) < 0.05 || sampleRate <= 0) return BiquadCoeffs.UNITY
    val a = 10.0.pow(gainDb / 40.0)
    val (sinW, cosW) = angle(sampleRate, frequencyHz)
    val alpha = shelfAlpha(sinW, a)
    val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
    val b0 = a * ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)
    val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)
    val b2 = a * ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha)
    val a0 = (a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha
    val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW)
    val a2 = (a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha
    return normalize(b0, b1, b2, a0, a1, a2)
}

internal fun designHighShelf(sampleRate: Int, frequencyHz: Double, gainDb: Double): BiquadCoeffs {
    if (abs(gainDb) < 0.05 || sampleRate <= 0) return BiquadCoeffs.UNITY
    val a = 10.0.pow(gainDb / 40.0)
    val (sinW, cosW) = angle(sampleRate, frequencyHz)
    val alpha = shelfAlpha(sinW, a)
    val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
    val b0 = a * ((a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha)
    val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW)
    val b2 = a * ((a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha)
    val a0 = (a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha
    val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW)
    val a2 = (a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha
    return normalize(b0, b1, b2, a0, a1, a2)
}

internal fun designPeaking(
    sampleRate: Int,
    frequencyHz: Double,
    gainDb: Double,
    q: Double,
): BiquadCoeffs {
    if (abs(gainDb) < 0.05 || sampleRate <= 0) return BiquadCoeffs.UNITY
    val a = 10.0.pow(gainDb / 40.0)
    val (sinW, cosW) = angle(sampleRate, frequencyHz)
    val alpha = sinW / (2.0 * q.coerceIn(0.05, 20.0))
    val b0 = 1.0 + alpha * a
    val b1 = -2.0 * cosW
    val b2 = 1.0 - alpha * a
    val a0 = 1.0 + alpha / a
    val a1 = -2.0 * cosW
    val a2 = 1.0 - alpha / a
    return normalize(b0, b1, b2, a0, a1, a2)
}

/** Shelf slope S = 1, the cookbook default. */
private fun shelfAlpha(sinW: Double, a: Double): Double {
    return sinW / 2.0 * sqrt((a + 1.0 / a) * (1.0 / 1.0 - 1.0) + 2.0)
}

private fun angle(sampleRate: Int, frequencyHz: Double): Pair<Double, Double> {
    val freq = frequencyHz.coerceIn(20.0, sampleRate * 0.45)
    val w0 = 2.0 * PI * freq / sampleRate.toDouble()
    return sin(w0) to cos(w0)
}

private fun normalize(
    b0: Double,
    b1: Double,
    b2: Double,
    a0: Double,
    a1: Double,
    a2: Double,
): BiquadCoeffs {
    if (!a0.isFinite() || abs(a0) < 1e-12) return BiquadCoeffs.UNITY
    val coeffs = BiquadCoeffs(
        b0 = b0 / a0,
        b1 = b1 / a0,
        b2 = b2 / a0,
        a1 = a1 / a0,
        a2 = a2 / a0,
    )
    if (!coeffs.b0.isFinite() || !coeffs.b1.isFinite() || !coeffs.b2.isFinite() ||
        !coeffs.a1.isFinite() || !coeffs.a2.isFinite()
    ) {
        return BiquadCoeffs.UNITY
    }
    return coeffs
}
