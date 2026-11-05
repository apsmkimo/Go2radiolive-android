package com.apsmkimo.go2radiolive.presentation.player

// SMCPKG_SUPPORT>>>Cursor123
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
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

    /** Replaces the wrapped player's command set with ours on every change it reports. */
    private inner class NavListener(
        private val delegate: Player.Listener,
    ) : Player.Listener by delegate {
        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            delegate.onAvailableCommandsChanged(this@EpisodeNavPlayer.availableCommands)
        }

        override fun equals(other: Any?): Boolean {
            return other is NavListener && other.delegate == delegate
        }

        override fun hashCode(): Int = delegate.hashCode()
    }
}
// SMCPKG_SUPPORT<<<Cursor123
