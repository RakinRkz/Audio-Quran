package com.audioquran.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audioquran.data.model.PlaybackState
import com.audioquran.data.model.Surah
import com.audioquran.data.repository.AudioFileRepository
import com.audioquran.data.repository.FuzzySearch
import com.audioquran.data.repository.PreferencesRepository
import com.audioquran.player.AudioPlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the Audio Quran app.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioFileRepository = AudioFileRepository(application)
    private val preferencesRepository = PreferencesRepository(application)
    val audioPlayerController = AudioPlayerController(application)

    private val _surahList = MutableStateFlow<List<Surah>>(emptyList())
    val surahList: StateFlow<List<Surah>> = _surahList.asStateFlow()

    // Starts true so the UI shows the loading screen (not the folder picker)
    // until the saved folder preference has been read on startup.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Folder currently loaded or being loaded, and the job doing it.
    private var loadedFolderUri: Uri? = null
    private var loadJob: Job? = null

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _hasFolderSelected = MutableStateFlow(false)
    val hasFolderSelected: StateFlow<Boolean> = _hasFolderSelected.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayerController.playbackState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredSurahList = MutableStateFlow<List<Surah>>(emptyList())
    val filteredSurahList: StateFlow<List<Surah>> = _filteredSurahList.asStateFlow()

    init {
        audioPlayerController.initialize()
        observeSavedFolder()
    }

    private fun observeSavedFolder() {
        viewModelScope.launch {
            preferencesRepository.audioFolderUri.collect { uri ->
                if (uri == null) {
                    // Nothing saved yet: stop the startup loading state so the
                    // folder selection screen can show.
                    if (loadJob?.isActive != true) _isLoading.value = false
                } else if (uri != loadedFolderUri) {
                    // Skip if onFolderSelected already started loading this folder.
                    _hasFolderSelected.value = true
                    loadAudioFiles(uri)
                }
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun onFolderSelected(uri: Uri) {
        // Switch to the loading screen right away, before the preference write,
        // so the user gets immediate feedback after picking a folder.
        _hasFolderSelected.value = true
        loadAudioFiles(uri)
        viewModelScope.launch {
            preferencesRepository.saveAudioFolderUri(uri)
        }
    }

    fun changeFolder() {
        loadJob?.cancel()
        loadJob = null
        loadedFolderUri = null
        _isLoading.value = false
        viewModelScope.launch {
            preferencesRepository.clearAudioFolderUri()
            _hasFolderSelected.value = false
            _surahList.value = emptyList()
            _filteredSurahList.value = emptyList()
            _searchQuery.value = ""
            FuzzySearch.clearCache()
        }
    }

    private fun loadAudioFiles(folderUri: Uri) {
        loadJob?.cancel()
        loadedFolderUri = folderUri
        _isLoading.value = true
        _errorMessage.value = null
        loadJob = viewModelScope.launch {
            try {
                val files = audioFileRepository.scanFolder(folderUri)
                if (files.isEmpty()) {
                    _errorMessage.value = "No audio files found in selected folder"
                    FuzzySearch.clearCache()
                } else {
                    _surahList.value = files
                    // Cache n-grams for faster search
                    FuzzySearch.cacheSurahNgrams(files)
                    // Sync filtered list with source list
                    _filteredSurahList.value = if (_searchQuery.value.isBlank()) {
                        files
                    } else {
                        FuzzySearch.searchSurahs(files, _searchQuery.value)
                    }
                }
            } catch (e: CancellationException) {
                // A newer load replaced this one; it owns the loading flag now.
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Error loading audio files: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun playSurah(surah: Surah) {
        audioPlayerController.playSurah(surah, _surahList.value)
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            audioPlayerController.pause()
        } else {
            audioPlayerController.play()
        }
    }

    fun next() {
        audioPlayerController.next()
    }

    fun previous() {
        audioPlayerController.previous()
    }

    fun seekTo(position: Long) {
        audioPlayerController.seekTo(position)
    }

    fun toggleRepeatMode() {
        audioPlayerController.toggleRepeatMode()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // Debounce is handled in the UI, this just updates state
        viewModelScope.launch {
            delay(150) // Debounce to reduce computation during typing
            if (!isActive) return@launch
            
            _filteredSurahList.value = if (query.isBlank()) {
                _surahList.value
            } else {
                FuzzySearch.searchSurahs(_surahList.value, query)
            }
        }
    }

    private fun startPositionUpdates() {
        // Position updates are handled by AudioPlayerController's Player.Listener
        // which updates the PlaybackState flow automatically
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerController.release()
    }
}
