package com.apsmkimo.go2radiolive.presentation.player

// SMCPKG_SUPPORT>>>Cursor123
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
// SMCPKG_SUPPORT>>>Cursor124
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
// SMCPKG_SUPPORT<<<Cursor124
import androidx.media3.common.util.UnstableApi

/**
 * Session-facing player. While a podcast episode from a show list is playing, the
 * notification / lock screen previous and next buttons step to the older / newer
 * episode via [EpisodeQueue] instead of the single-item playlist. Everything else
 * (radio, alarms, episodes without a list) passes straight through.
 */
@OptIn(UnstableApi::class)
internal class EpisodeNavPlayer(player: Player) : ForwardingPlayer(player) {

    private val wrappers = HashMap<Player.Listener, Player.Listener>()

    private fun activeNav(): EpisodeNav? {
        val nav = EpisodeQueue.nav.value
        val id = nav.currentId ?: return null
        return if (id == currentMediaItem?.mediaId) nav else null
    }

    override fun addListener(listener: Player.Listener) {
        val wrapper = NavListener(listener)
        synchronized(wrappers) { wrappers[listener] = wrapper }
        super.addListener(wrapper)
    }

    override fun removeListener(listener: Player.Listener) {
        val wrapper = synchronized(wrappers) { wrappers.remove(listener) } ?: return
        super.removeListener(wrapper)
    }

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        val nav = activeNav() ?: return base
        val builder = base.buildUpon().removeAll(
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        )
        if (nav.hasOlder) {
            builder.addAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        }
        if (nav.hasNewer) {
            builder.addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        }
        return builder.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return availableCommands.contains(command)
    }

    override fun hasNextMediaItem(): Boolean {
        val nav = activeNav() ?: return super.hasNextMediaItem()
        return nav.hasNewer
    }

    override fun hasPreviousMediaItem(): Boolean {
        val nav = activeNav() ?: return super.hasPreviousMediaItem()
        return nav.hasOlder
    }

    override fun seekToNext() {
        val nav = activeNav() ?: return super.seekToNext()
        if (nav.hasNewer) EpisodeQueue.request(newer = true)
    }

    override fun seekToNextMediaItem() {
        val nav = activeNav() ?: return super.seekToNextMediaItem()
        if (nav.hasNewer) EpisodeQueue.request(newer = true)
    }

    override fun seekToPrevious() {
        val nav = activeNav() ?: return super.seekToPrevious()
        if (nav.hasOlder) EpisodeQueue.request(newer = false)
    }

    override fun seekToPreviousMediaItem() {
        val nav = activeNav() ?: return super.seekToPreviousMediaItem()
        if (nav.hasOlder) EpisodeQueue.request(newer = false)
    }

    /** Tells the session (notification, lock screen, controllers) to re-read the commands. */
    fun notifyNavChanged() {
        val commands = availableCommands
        val targets = synchronized(wrappers) { wrappers.keys.toList() }
        targets.forEach { it.onAvailableCommandsChanged(commands) }
    }

    // SMCPKG_SUPPORT>>>Cursor124
    // /** Replaces the wrapped player's command set with ours on every change it reports. */
    // private inner class NavListener(
    //     private val delegate: Player.Listener,
    // ) : Player.Listener by delegate {
    //     override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
    //         delegate.onAvailableCommandsChanged(this@EpisodeNavPlayer.availableCommands)
    //     }
    //
    //     override fun equals(other: Any?): Boolean {
    //         return other is NavListener && other.delegate == delegate
    //     }
    //
    //     override fun hashCode(): Int = delegate.hashCode()
    // }
    // Kotlin's `by delegate` does not forward Java default methods, and every
    // Player.Listener method is a Java default method. The old wrapper therefore
    // dropped every event except onAvailableCommandsChanged: the media session never
    // heard STATE_READY / isPlaying, the app timed out each attempt and stopped the
    // player, so neither radio nor podcasts could play. Forward every callback by hand.
    private inner class NavListener(
        delegate: Player.Listener,
    ) : ForwardingNavListener(delegate) {
        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            delegate.onAvailableCommandsChanged(this@EpisodeNavPlayer.availableCommands)
        }
    }
    // SMCPKG_SUPPORT<<<Cursor124
}
// SMCPKG_SUPPORT<<<Cursor123

// SMCPKG_SUPPORT>>>Cursor124
/**
 * Forwards every [Player.Listener] callback to [delegate]. Do not replace this with
 * Kotlin interface delegation (`by delegate`): Java default methods are not delegated.
 * ForwardingNavListenerTest fails if Media3 adds a callback that is not forwarded here.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
internal open class ForwardingNavListener(
    val delegate: Player.Listener,
) : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) = delegate.onEvents(player, events)
    override fun onTimelineChanged(timeline: Timeline, reason: Int) = delegate.onTimelineChanged(timeline, reason)
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
        delegate.onMediaItemTransition(mediaItem, reason)
    override fun onTracksChanged(tracks: Tracks) = delegate.onTracksChanged(tracks)
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = delegate.onMediaMetadataChanged(mediaMetadata)
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) =
        delegate.onPlaylistMetadataChanged(mediaMetadata)
    override fun onIsLoadingChanged(isLoading: Boolean) = delegate.onIsLoadingChanged(isLoading)
    override fun onLoadingChanged(isLoading: Boolean) = delegate.onLoadingChanged(isLoading)
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
        delegate.onAvailableCommandsChanged(availableCommands)
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
        delegate.onTrackSelectionParametersChanged(parameters)
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
        delegate.onPlayerStateChanged(playWhenReady, playbackState)
    override fun onPlaybackStateChanged(playbackState: Int) = delegate.onPlaybackStateChanged(playbackState)
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
        delegate.onPlayWhenReadyChanged(playWhenReady, reason)
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
        delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
    override fun onIsPlayingChanged(isPlaying: Boolean) = delegate.onIsPlayingChanged(isPlaying)
    override fun onRepeatModeChanged(repeatMode: Int) = delegate.onRepeatModeChanged(repeatMode)
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
        delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)
    override fun onPlayerError(error: PlaybackException) = delegate.onPlayerError(error)
    override fun onPlayerErrorChanged(error: PlaybackException?) = delegate.onPlayerErrorChanged(error)
    override fun onPositionDiscontinuity(reason: Int) = delegate.onPositionDiscontinuity(reason)
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) = delegate.onPositionDiscontinuity(oldPosition, newPosition, reason)
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
        delegate.onPlaybackParametersChanged(playbackParameters)
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) =
        delegate.onSeekBackIncrementChanged(seekBackIncrementMs)
    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
        delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)
    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
        delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
    override fun onAudioSessionIdChanged(audioSessionId: Int) = delegate.onAudioSessionIdChanged(audioSessionId)
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) =
        delegate.onAudioAttributesChanged(audioAttributes)
    override fun onVolumeChanged(volume: Float) = delegate.onVolumeChanged(volume)
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) =
        delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = delegate.onDeviceInfoChanged(deviceInfo)
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = delegate.onDeviceVolumeChanged(volume, muted)
    override fun onVideoSizeChanged(videoSize: VideoSize) = delegate.onVideoSizeChanged(videoSize)
    override fun onSurfaceSizeChanged(width: Int, height: Int) = delegate.onSurfaceSizeChanged(width, height)
    override fun onRenderedFirstFrame() = delegate.onRenderedFirstFrame()
    override fun onCues(cues: List<Cue>) = delegate.onCues(cues)
    override fun onCues(cueGroup: CueGroup) = delegate.onCues(cueGroup)
    override fun onMetadata(metadata: Metadata) = delegate.onMetadata(metadata)

    override fun equals(other: Any?): Boolean {
        return other is ForwardingNavListener && other.javaClass == javaClass && other.delegate == delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
}
// SMCPKG_SUPPORT<<<Cursor124
