package com.audioquran.data.repository

import com.audioquran.data.model.Surah

/**
 * Search utility using cosine similarity for fuzzy matching.
 * Handles typos by comparing n-gram vectors.
 * Uses cached n-grams for performance when searching repeatedly.
 */
object FuzzySearch {

    private const val NGRAM_SIZE = 2

    // Cache for precomputed n-grams per surah
    private var cachedSurahs: List<Surah>? = null
    private var cachedNgrams: List<Pair<Map<String, Int>, Map<String, Int>>>? = null

    /**
     * Precompute and cache n-grams for a list of surahs.
     * Call this once when the surah list is loaded.
     */
    fun cacheSurahNgrams(surahs: List<Surah>) {
        if (surahs == cachedSurahs) return // Already cached
        
        cachedSurahs = surahs
        cachedNgrams = surahs.map { surah ->
            val nameLower = surah.name.lowercase()
            val arabicLower = surah.arabicName.lowercase()
            getNGrams(nameLower) to getNGrams(arabicLower)
        }
    }

    /**
     * Clear the n-gram cache.
     */
    fun clearCache() {
        cachedSurahs = null
        cachedNgrams = null
    }

    /**
     * Search surahs by name using cosine similarity.
     * Returns top 5 surahs sorted by relevance score (descending).
     * Uses cached n-grams for better performance.
     */
    fun searchSurahs(surahs: List<Surah>, query: String): List<Surah> {
        if (query.isBlank()) return surahs

        // Rebuild cache if needed (e.g., if surah list changed)
        if (cachedSurahs != surahs) {
            cacheSurahNgrams(surahs)
        }

        val queryLower = query.lowercase().trim()
        val queryNgrams = getNGrams(queryLower)

        if (queryNgrams.isEmpty()) return surahs

        val ngrams = cachedNgrams ?: return surahs

        return surahs.zip(ngrams)
            .map { (surah, ngramPair) ->
                val (nameNgrams, arabicNgrams) = ngramPair
                // Search in both English and Arabic names using cached n-grams
                val nameScore = calculateSimilarity(queryNgrams, nameNgrams)
                val arabicScore = calculateSimilarity(queryNgrams, arabicNgrams)

                val maxScore = maxOf(nameScore, arabicScore)
                surah to maxScore
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    /**
     * Generate character n-grams from a string.
     */
    private fun getNGrams(text: String): Map<String, Int> {
        if (text.length < NGRAM_SIZE) {
            return if (text.isNotEmpty()) mapOf(text to 1) else emptyMap()
        }

        val ngrams = mutableMapOf<String, Int>()
        for (i in 0..text.length - NGRAM_SIZE) {
            val ngram = text.substring(i, i + NGRAM_SIZE)
            ngrams[ngram] = ngrams.getOrDefault(ngram, 0) + 1
        }
        return ngrams
    }

    /**
     * Calculate cosine similarity between two n-gram vectors.
     */
    private fun calculateSimilarity(queryNgrams: Map<String, Int>, textNgrams: Map<String, Int>): Double {
        if (queryNgrams.isEmpty() || textNgrams.isEmpty()) return 0.0

        // Get all unique n-grams
        val allKeys = (queryNgrams.keys + textNgrams.keys).toSet()

        // Calculate dot product
        var dotProduct = 0.0
        allKeys.forEach { key ->
            val queryFreq = queryNgrams[key] ?: 0
            val textFreq = textNgrams[key] ?: 0
            dotProduct += queryFreq * textFreq
        }

        // Calculate magnitudes
        val queryMagnitude = kotlin.math.sqrt(queryNgrams.values.sumOf { (it * it).toDouble() })
        val textMagnitude = kotlin.math.sqrt(textNgrams.values.sumOf { (it * it).toDouble() })

        if (queryMagnitude == 0.0 || textMagnitude == 0.0) return 0.0

        return dotProduct / (queryMagnitude * textMagnitude)
    }
}
