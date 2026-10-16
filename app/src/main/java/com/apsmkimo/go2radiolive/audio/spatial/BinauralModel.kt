package com.apsmkimo.go2radiolive.audio.spatial

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Parametric binaural cues for headphone listening.
 *
 * Interaural time uses the spherical-head formula ITD = (r/c) (β + sin β),
 * with β the lateral angle folded into 0..π/2 (Woodworth; the same relation
 * used by structural pinna/head models). No measured HRTF dataset is shipped.
 * Level uses a cosine head-shadow, and a one-pole lowpass stands in for the
 * far-ear shadow. A peaking notch above 6 kHz deepens toward the rear as a
 * simple pinna cue. Speech energy around 1–4 kHz is left unfiltered.
 */
internal object BinauralModel {
    const val HEAD_RADIUS_M = 0.0875
    const val SPEED_OF_SOUND = 343.0
    const val BASE_DELAY_SAMPLES = 64

    fun wrapAzimuth(azimuth: Double): Double {
        var value = azimuth
        val turn = PI * 2.0
        while (value > PI) value -= turn
        while (value < -PI) value += turn
        return value
    }

    /** Positive means the right ear leads (source is to the right). */
    fun itdSeconds(azimuth: Double): Double {
        val wrapped = wrapAzimuth(azimuth)
        val absAz = abs(wrapped)
        val beta = if (absAz <= PI / 2.0) absAz else PI - absAz
        val magnitude = (HEAD_RADIUS_M / SPEED_OF_SOUND) * (beta + sin(beta))
        return if (wrapped >= 0.0) magnitude else -magnitude
    }

    fun earGain(azimuth: Double, earAzimuth: Double): Double {
        val closeness = (cos(angleDistance(azimuth, earAzimuth)) + 1.0) * 0.5
        return 0.22 + 0.78 * closeness
    }

    /** Lowpass cutoff for the head shadow. Near the ear this stays high. */
    fun shadowCutoffHz(azimuth: Double, earAzimuth: Double): Double {
        val far = angleDistance(azimuth, earAzimuth) / PI
        return 11000.0 - 9000.0 * far.coerceIn(0.0, 1.0)
    }

    /** 0 at the front, 1 directly behind. */
    fun rearAmount(azimuth: Double): Double {
        return ((-cos(wrapAzimuth(azimuth)) + 1.0) * 0.5).coerceIn(0.0, 1.0)
    }

    fun angleDistance(a: Double, b: Double): Double {
        val delta = abs(wrapAzimuth(a - b))
        return if (delta > PI) PI * 2.0 - delta else delta
    }
}

internal class OnePoleLowpass {
    private var y = 0.0
    private var coeff = 1.0

    fun setCutoff(frequencyHz: Double, sampleRate: Double) {
        val rate = sampleRate.coerceAtLeast(8_000.0)
        val fc = frequencyHz.coerceIn(80.0, rate * 0.45)
        coeff = 1.0 - exp(-2.0 * PI * fc / rate)
    }

    fun process(sample: Double): Double {
        y += coeff * (sample - y)
        if (y > -1e-20 && y < 1e-20) y = 0.0
        return if (y.isFinite()) y else 0.0
    }

    fun reset() {
        y = 0.0
    }
}

internal class FractionalDelay(private val capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var write = 0

    fun clear() {
        buffer.fill(0f)
        write = 0
    }

    fun push(sample: Float) {
        buffer[write] = if (sample.isFinite()) sample else 0f
        write++
        if (write >= capacity) write = 0
    }

    fun read(delaySamples: Float): Float {
        val maxDelay = (capacity - 2).toFloat()
        val delay = delaySamples.coerceIn(0f, maxDelay)
        val whole = delay.toInt()
        val fraction = delay - whole
        val newer = wrap(write - 1 - whole)
        val older = wrap(newer - 1)
        val a = buffer[newer]
        val b = buffer[older]
        return a + (b - a) * fraction
    }

    private fun wrap(index: Int): Int {
        var value = index
        while (value < 0) value += capacity
        while (value >= capacity) value -= capacity
        return value
    }
}
