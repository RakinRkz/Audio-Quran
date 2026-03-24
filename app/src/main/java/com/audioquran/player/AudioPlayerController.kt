package com.audioquran.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.audioquran.data.model.PlaybackState
import com.audioquran.data.model.RepeatMode
import com.audioquran.data.model.Surah
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.AsyncFunction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller for managing audio playback using Media3.
 */
class AudioPlayerController(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerController"
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private var playlist: List<Surah> = emptyList()
    private var currentIndex: Int = -1

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState()
            handlePlaybackCompletion(playbackState)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updateState()
        }
    }

    fun initialize() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlaybackService::class.java)
        )

        try {
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(playerListener)
                    _playerError.value = null
                    updateState()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get MediaController: ${e.message}")
                    _playerError.value = "Failed to initialize audio player: ${e.message}"
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build MediaController: ${e.message}")
            _playerError.value = "Failed to initialize audio player: ${e.message}"
        }
    }

    fun release() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }

    fun playSurah(surah: Surah, playlist: List<Surah>) {
        this.playlist = playlist
        this.currentIndex = playlist.indexOfFirst { it.number == surah.number }

        // If the clicked surah is already the current track and playing, pause it
        if (currentIndex >= 0 && mediaController?.currentMediaItemIndex == currentIndex && mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            val mediaItems = playlist.map { it.toMediaItem() }
            mediaController?.apply {
                setMediaItems(mediaItems, currentIndex, 0)
                prepare()
                play()
            }
        }
        updateState()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun stop() {
        mediaController?.stop()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun next() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            controller.seekTo(0, 0)
            controller.play()
        }
        currentIndex = controller.currentMediaItemIndex
        updateState()
    }

    fun previous() {
        val controller = mediaController ?: return
        
        // If more than 3 seconds played, restart current track
        if (controller.currentPosition > 3000) {
            controller.seekTo(0)
            return
        }

        if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            controller.seekTo(controller.mediaItemCount - 1, 0)
        }
        currentIndex = controller.currentMediaItemIndex
        updateState()
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val currentMode = _playbackState.value.repeatMode
        val newMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        controller.repeatMode = when (newMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        updateState()
    }

    fun setRepeatMode(mode: RepeatMode) {
        val controller = mediaController ?: return
        controller.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        updateState()
    }

    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return mediaController?.duration ?: 0L
    }

    private fun updateState() {
        val controller = mediaController ?: return

        val currentMediaItem = controller.currentMediaItem
        val currentSurah = currentMediaItem?.let { _ ->
            playlist.getOrNull(controller.currentMediaItemIndex)
        }

        val repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> RepeatMode.OFF
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        _playbackState.value = PlaybackState(
            currentSurah = currentSurah,
            isPlaying = controller.isPlaying,
            currentPosition = controller.currentPosition,
            duration = controller.duration.takeIf { it > 0 } ?: 0L,
            repeatMode = repeatMode,
            playlist = playlist,
            currentIndex = controller.currentMediaItemIndex
        )
    }

    private fun handlePlaybackCompletion(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            val mode = _playbackState.value.repeatMode
            when (mode) {
                RepeatMode.ONE -> {
                    mediaController?.seekTo(0)
                    mediaController?.play()
                }
                RepeatMode.ALL -> {
                    // Media3 handles this automatically with REPEAT_MODE_ALL
                }
                RepeatMode.OFF -> {
                    // Check if there are more items
                    if (mediaController?.hasNextMediaItem() != true) {
                        // Playback completed
                    }
                }
            }
        }
    }

    private fun Surah.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(fileUri)
            .setMediaId(number.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setAlbumTitle("Quran with Bangla Translation")
                    .setArtworkUri(Uri.parse("android.resource://com.audioquran/drawable/ic_quran"))
                    .build()
            )
            .build()
    }
}
