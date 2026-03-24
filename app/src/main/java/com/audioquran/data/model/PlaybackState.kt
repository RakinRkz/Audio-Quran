package com.audioquran.data.model

/**
 * Represents the current playback state of the audio player.
 */
data class PlaybackState(
    val currentSurah: Surah? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playlist: List<Surah> = emptyList(),
    val currentIndex: Int = -1
) {
    val hasNext: Boolean
        get() = currentIndex < playlist.lastIndex || repeatMode == RepeatMode.ALL

    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
}

/**
 * Repeat mode for audio playback.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
