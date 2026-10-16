package com.apsmkimo.go2radiolive.audio.spatial

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
}
