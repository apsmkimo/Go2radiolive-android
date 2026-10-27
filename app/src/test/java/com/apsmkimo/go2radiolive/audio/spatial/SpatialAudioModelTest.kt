package com.apsmkimo.go2radiolive.audio.spatial

import com.apsmkimo.go2radiolive.audio.dsp.designPeaking
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class SpatialAudioModelTest {

    @Test
    fun unknownModeFallsBackToOff() {
        assertEquals(SpatialAudioMode.OFF, SpatialAudioMode.fromStored(null))
        assertEquals(SpatialAudioMode.OFF, SpatialAudioMode.fromStored(""))
        assertEquals(SpatialAudioMode.OFF, SpatialAudioMode.fromStored("widen"))
        assertEquals(SpatialAudioMode.ORBIT, SpatialAudioMode.fromStored("orbit"))
    }

    @Test
    fun orbitControlsStayInsideSpec() {
        val settings = AudioSettings(
            orbitPeriodSec = 4f,
            orbitStrength = 3f,
        ).sanitized()
        assertEquals(AudioSettings.ORBIT_PERIOD_MIN, settings.orbitPeriodSec)
        assertEquals(1f, settings.orbitStrength)
        val slow = AudioSettings(orbitPeriodSec = 400f).sanitized()
        assertEquals(AudioSettings.ORBIT_PERIOD_MAX, slow.orbitPeriodSec)
    }

    @Test
    fun itdIsAntisymmetricAndBounded() {
        val right = BinauralModel.itdSeconds(PI / 2.0)
        val left = BinauralModel.itdSeconds(-PI / 2.0)
        assertTrue(right > 0.0)
        assertEquals(right, -left, 1e-12)
        assertEquals(0.0, BinauralModel.itdSeconds(0.0), 1e-12)
        assertEquals(0.0, BinauralModel.itdSeconds(PI), 1e-9)
        assertTrue(abs(right) < 0.001)
    }

    @Test
    fun azimuthWrapStaysOnTheCircle() {
        val wrapped = BinauralModel.wrapAzimuth(3.0 * PI)
        assertTrue(wrapped <= PI && wrapped >= -PI)
        assertEquals(PI, wrapped, 1e-9)
        assertEquals(-PI, BinauralModel.wrapAzimuth(-3.0 * PI), 1e-9)
    }

    @Test
    fun modeChangeFadesOutBeforeTheNextModeStarts() {
        var active = SpatialAudioMode.NATURAL
        var wet = wetTargetFor(active, 0.6)
        var target = wet
        val coeff = 0.08
        var sawSilence = false
        repeat(80) {
            val step = advanceSpatialMode(
                active = active,
                pending = SpatialAudioMode.ORBIT,
                wet = wet,
                targetWet = target,
                wetCoeff = coeff,
                orbitStrength = 0.6,
            )
            if (step.active != active) {
                assertTrue(wet <= 0.01)
                sawSilence = true
            }
            assertTrue(step.active == SpatialAudioMode.NATURAL || step.active == SpatialAudioMode.ORBIT)
            active = step.active
            wet = step.wet
            target = if (active != SpatialAudioMode.ORBIT && wet > 0.01) 0.0 else step.targetWet
        }
        assertTrue(sawSilence)
        assertEquals(SpatialAudioMode.ORBIT, active)
        assertTrue(wet > 0.2)
    }

    @Test
    fun delayAndLowpassStayFinite() {
        val delay = FractionalDelay(64)
        val lowpass = OnePoleLowpass()
        lowpass.setCutoff(2_000.0, 48_000.0)
        var last = 0f
        repeat(4_000) { index ->
            val sample = if (index % 2 == 0) 0.5f else -0.5f
            delay.push(sample)
            last = delay.read(8.5f)
            val filtered = lowpass.process(sample.toDouble())
            assertTrue(last.isFinite())
            assertTrue(filtered.isFinite())
        }
        assertTrue(abs(last) <= 0.5f)
    }

    @Test
    fun naturalMonoStaysCenteredAndStereoWidthDoesNotJump() {
        // SMCPKG_SUPPORT>>>Cursor116
        // Mono used to sit at azimuth 0, which made 3D inaudible on speech.
        // assertEquals(
        //     0.0,
        //     sourceAzimuth(SpatialAudioMode.NATURAL, 1, 0.0, naturalHalfRadians(0.5), right = false),
        //     1e-12,
        // )
        // assertEquals(
        //     0.0,
        //     sourceAzimuth(SpatialAudioMode.NATURAL, 1, 0.0, naturalHalfRadians(0.5), right = true),
        //     1e-12,
        // )
        val monoHalf = naturalHalfRadians(0.5)
        assertEquals(
            -monoHalf,
            sourceAzimuth(SpatialAudioMode.NATURAL, 1, 0.0, monoHalf, right = false),
            1e-12,
        )
        assertEquals(
            monoHalf,
            sourceAzimuth(SpatialAudioMode.NATURAL, 1, 0.0, monoHalf, right = true),
            1e-12,
        )
        assertTrue(Math.toDegrees(naturalHalfRadians(1.0)) >= 45.0)
        assertTrue(Math.toDegrees(naturalHalfRadians(0.0)) >= 70.0)
        // SMCPKG_SUPPORT<<<Cursor116
        val below = naturalHalfRadians(0.97)
        val above = naturalHalfRadians(0.99)
        assertTrue(abs(Math.toDegrees(above - below)) < 3.0)
        assertTrue(naturalHalfRadians(1.0) < naturalHalfRadians(0.80))
        assertEquals(0.98, holdCorrelation(0.98, 0.99), 1e-9)
        assertEquals(0.94, holdCorrelation(0.98, 0.94), 1e-9)
        val half = naturalHalfRadians(0.5)
        val left = sourceAzimuth(SpatialAudioMode.NATURAL, 2, 0.0, half, right = false)
        val right = sourceAzimuth(SpatialAudioMode.NATURAL, 2, 0.0, half, right = true)
        assertEquals(-half, left, 1e-12)
        assertEquals(half, right, 1e-12)
    }

    @Test
    fun offBypassKeepsTheImpulseOnTheDirectSample() {
        assertEquals(0.25, bypassMix(0.25, 0.9, 0.0), 1e-12)
        assertEquals(0.9, bypassMix(0.25, 0.9, 1.0), 1e-12)
        val blended = bypassMix(0.25, 0.9, 0.5)
        assertEquals(0.575, blended, 1e-12)
        assertTrue(blended > 0.25 && blended < 0.9)

        val delay = FractionalDelay(512)
        val frames = 128
        val direct = FloatArray(frames)
        val delayed = FloatArray(frames)
        var index = 0
        while (index < frames) {
            val sample = if (index == 0) 1f else 0f
            direct[index] = sample
            delay.push(sample)
            delayed[index] = delay.read(alignedBaseDelaySamples())
            val mixed = bypassMix(direct[index].toDouble(), delayed[index].toDouble(), align = 0.0)
            assertEquals(direct[index].toDouble(), mixed, 1e-6)
            index++
        }
        assertEquals(0, direct.indexOfFirst { it > 0.5f })
        assertEquals(BinauralModel.BASE_DELAY_SAMPLES, delayed.indexOfFirst { it > 0.5f })
    }

    @Test
    fun dryAndWetShareTheBaseDelaySoTheMixIsOneImpulse() {
        val delay = FractionalDelay(512)
        val base = alignedBaseDelaySamples()
        assertEquals(BinauralModel.BASE_DELAY_SAMPLES.toFloat(), base, 0f)
        val frames = 128
        val dry = FloatArray(frames)
        val wet = FloatArray(frames)
        var index = 0
        while (index < frames) {
            delay.push(if (index == 0) 1f else 0f)
            dry[index] = delay.read(base)
            wet[index] = delay.read(wetEarDelaySamples(0.0, 0.5))
            index++
        }
        val dryPeak = dry.indexOfFirst { it > 0.5f }
        val wetPeak = wet.indexOfFirst { it > 0.5f }
        assertTrue(dryPeak >= 0)
        assertEquals(dryPeak, wetPeak)
        var mixedPeaks = 0
        index = 0
        while (index < frames) {
            val mixed = 0.5f * dry[index] + 0.5f * wet[index]
            if (mixed > 0.5f) mixedPeaks++
            index++
        }
        assertEquals(1, mixedPeaks)
    }

    @Test
    fun rearReflectionFiltersDoNotShareEarOrTapState() {
        assertTrue(abs(BinauralModel.itdSeconds(Math.toRadians(-125.0))) > 1e-5)
        assertTrue(BinauralModel.rearAmount(Math.toRadians(-125.0)) > 0.75)
        assertTrue(BinauralModel.rearAmount(Math.toRadians(110.0)) < 0.75)
        val coeffs = designPeaking(48_000, 7_600.0, -8.0, q = 2.2)
        val side = EarSplitFilter()
        val rear = EarSplitFilter()
        side.set(coeffs)
        rear.set(coeffs)
        side.process(1.0, 0.0)
        assertTrue(abs(side.outL) > 0.1)
        assertEquals(0.0, side.outR, 1e-9)
        assertEquals(0.0, rear.outL, 0.0)
        rear.process(0.0, 0.0)
        assertEquals(0.0, rear.outL, 1e-9)
        assertEquals(0.0, rear.outR, 1e-9)
        side.process(0.0, 0.0)
        assertTrue(abs(side.outL) > 1e-4)
        assertEquals(0.0, side.outR, 1e-6)
    }
}
