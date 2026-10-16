package com.apsmkimo.go2radiolive.audio.dsp

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Installs the original PCM processor chain:
 * 16-bit EQ -> float voice enhancement -> stereo field / balance / soft limit -> 16-bit.
 * Float output is left off because Media3 only inserts app processors on the 16-bit path.
 */
class Go2RenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(
                arrayOf(
                    EqBiquadAudioProcessor(),
                    VoiceEnhanceAudioProcessor(),
                    StereoFieldAudioProcessor(),
                ),
            )
            .build()
    }
}
