package com.audioquran.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.audioquran.data.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File

/**
 * Repository for scanning and managing audio files from a user-selected folder.
 */
class AudioFileRepository(private val context: Context) {

    companion object {
        private const val TAG = "AudioFileRepository"
        private val SUPPORTED_EXTENSIONS = listOf("m4a", "mp3", "ogg", "wav", "aac", "flac", "webm")
    }

    /**
     * Scan a folder (represented by a tree URI) for audio files.
     */
    suspend fun scanFolder(treeUri: Uri): List<Surah> = withContext(Dispatchers.IO) {
        val surahList = mutableListOf<Surah>()

        try {
            // Take persistent permission
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (e: SecurityException) {
            // Permission might not be persistable - this is expected for some providers
            // We can still try to access the folder, but permission may need renewal
            Log.w(TAG, "Failed to take persistent URI permission: ${e.message}")
        }

        // Get the documents contract for the tree URI
        val documentContract = try {
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        // Get children documents
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        val documents = mutableListOf<Pair<Uri, String>>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeColumn)

                // Check if it's a file (not directory) and has audio extension
                if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension in SUPPORTED_EXTENSIONS) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        documents.add(documentUri to name)
                    }
                }
            }
        }

        // Sort lexicographically by filename
        // Lazy loading: only load name and basic info initially
        // Duration will be obtained from MediaController when playing
        documents.sortedBy { (_, name) -> name }.forEachIndexed { index, (uri, name) ->
            val surahNumber = Surah.parseSurahNumber(name) ?: (index + 1)
            val surahName = Surah.parseSurahName(name)
            val (arabicName) = Surah.SURAH_NAMES[surahNumber] ?: (surahName to "")

            surahList.add(
                Surah(
                    number = surahNumber,
                    name = surahName,
                    arabicName = arabicName,
                    filePath = uri.toString(),
                    fileUri = uri,
                    duration = 0L // Lazy loaded when playing via MediaController
                )
            )
        }

        surahList
    }

    /**
     * Scan the downloads folder as a fallback.
     * Note: This method only works on Android 9 (API 28) and below due to deprecated APIs.
     * For Android 10+, users should use the folder picker (SAF) instead.
     */
    @Suppress("DEPRECATION")
    suspend fun scanDefaultDownloadsFolder(): List<Surah> = withContext(Dispatchers.IO) {
        // Check API level - this deprecated API only works on API 28 and below
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // On Android 10+, scoped storage prevents access to Downloads without permission
            // Users should use the folder picker to grant access instead
            return@withContext emptyList()
        }

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS)

        if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
            return@withContext emptyList()
        }

        // Lazy loading: only load name and basic info initially
        val surahList = downloadsDir.listFiles { file ->
            val extension = file.extension.lowercase()
            extension in SUPPORTED_EXTENSIONS
        }?.sortedBy { it.name }?.map { file ->
            val name = file.name
            val surahNumber = Surah.parseSurahNumber(name) ?: 0
            val surahName = Surah.parseSurahName(name)
            val (arabicName) = Surah.SURAH_NAMES[surahNumber] ?: (surahName to "")

            Surah(
                number = surahNumber,
                name = surahName,
                arabicName = arabicName,
                filePath = file.absolutePath,
                fileUri = Uri.fromFile(file),
                duration = 0L // Lazy loaded when playing via MediaController
            )
        } ?: emptyList()

        surahList
    }

}
