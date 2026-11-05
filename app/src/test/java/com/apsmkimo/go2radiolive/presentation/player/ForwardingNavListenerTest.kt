package com.apsmkimo.go2radiolive.presentation.player

// SMCPKG_SUPPORT>>>Cursor124
import androidx.media3.common.Player
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the bug where `Player.Listener by delegate` silently dropped every Java default
 * callback, so the media session never saw the player become ready and nothing played.
 */
class ForwardingNavListenerTest {

    private fun listenerMethods(): List<Method> =
        Player.Listener::class.java.methods.filter { !Modifier.isStatic(it.modifiers) }

    @Test
    fun declaresEveryPlayerListenerCallback() {
        val missing = listenerMethods().filter { method ->
            runCatching {
                ForwardingNavListener::class.java.getDeclaredMethod(method.name, *method.parameterTypes)
            }.isFailure
        }
        assertTrue("Not forwarded: ${missing.map { it.name }}", missing.isEmpty())
    }

    @Test
    fun forwardsPlaybackCallbacksToTheDelegate() {
        val calls = mutableListOf<String>()
        val delegate = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                calls += "isPlaying=$isPlaying"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                calls += "state=$playbackState"
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                calls += "playWhenReady=$playWhenReady"
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                calls += "loading=$isLoading"
            }

            override fun onVolumeChanged(volume: Float) {
                calls += "volume=$volume"
            }
        }
        val forwarding: Player.Listener = ForwardingNavListener(delegate)
        forwarding.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        forwarding.onPlaybackStateChanged(Player.STATE_BUFFERING)
        forwarding.onIsLoadingChanged(true)
        forwarding.onPlaybackStateChanged(Player.STATE_READY)
        forwarding.onIsPlayingChanged(true)
        forwarding.onVolumeChanged(0.5f)
        assertEquals(
            listOf(
                "playWhenReady=true",
                "state=${Player.STATE_BUFFERING}",
                "loading=true",
                "state=${Player.STATE_READY}",
                "isPlaying=true",
                "volume=0.5",
            ),
            calls,
        )
    }

    @Test
    fun equalityFollowsTheDelegate() {
        val delegate = object : Player.Listener {}
        assertEquals(ForwardingNavListener(delegate), ForwardingNavListener(delegate))
        assertEquals(ForwardingNavListener(delegate).hashCode(), ForwardingNavListener(delegate).hashCode())
    }
}
// SMCPKG_SUPPORT<<<Cursor124
