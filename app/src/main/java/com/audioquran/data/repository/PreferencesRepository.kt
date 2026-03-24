package com.audioquran.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_quran_prefs")

/**
 * Repository for storing and retrieving user preferences.
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        private val AUDIO_FOLDER_URI_KEY = stringPreferencesKey("audio_folder_uri")
    }

    /**
     * Get the saved audio folder URI as a Flow.
     */
    val audioFolderUri: Flow<Uri?> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_FOLDER_URI_KEY]?.let { Uri.parse(it) }
    }

    /**
     * Save the audio folder URI.
     */
    suspend fun saveAudioFolderUri(uri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_FOLDER_URI_KEY] = uri.toString()
        }
    }

    /**
     * Clear the saved audio folder URI.
     */
    suspend fun clearAudioFolderUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUDIO_FOLDER_URI_KEY)
        }
    }
}
