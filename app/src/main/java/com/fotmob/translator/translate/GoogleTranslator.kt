package com.fotmob.translator.translate

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class GoogleTranslator(
    private val translationCache: TranslationCache
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    companion object {
        private const val TAG = "GoogleTranslator"
        private const val BASE_URL = "https://translate.googleapis.com/translate_a/single"
        private const val MAX_RETRIES = 3
        private const val RATE_LIMIT_MS = 200L // 5 requests per second
        private const val BATCH_SEPARATOR = "\n"
        private const val MAX_BATCH_SIZE = 50
    }

    /**
     * Translate a single text from English to Chinese
     */
    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        // Check cache first
        translationCache.get(text)?.let { return@withContext it }

        // Check if text is already Chinese (simple heuristic)
        if (isLikelyChinese(text)) return@withContext text

        translateInternal(listOf(text)).firstOrNull() ?: text
    }

    /**
     * Translate multiple texts in batch
     */
    suspend fun translateBatch(texts: List<String>): List<String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<String>()
        val uncachedTexts = mutableListOf<String>()
        val uncachedIndices = mutableListOf<Int>()

        // Check cache for each text
        texts.forEachIndexed { index, text ->
            if (text.isBlank() || isLikelyChinese(text)) {
                results.add(text)
            } else {
                val cached = translationCache.get(text)
                if (cached != null) {
                    results.add(cached)
                } else {
                    results.add(text) // placeholder
                    uncachedTexts.add(text)
                    uncachedIndices.add(index)
                }
            }
        }

        if (uncachedTexts.isEmpty()) return@withContext results

        // Translate uncached texts in batches
        uncachedTexts.chunked(MAX_BATCH_SIZE).forEach { batch ->
            val translated = translateInternal(batch)
            translated.forEachIndexed { batchIndex, translatedText ->
                val originalText = batch[batchIndex]
                val resultIndex = uncachedIndices[uncachedTexts.indexOf(originalText)]
                results[resultIndex] = translatedText

                // Cache the result
                translationCache.put(originalText, translatedText)
            }
        }

        results
    }

    private suspend fun translateInternal(texts: List<String>): List<String> {
        var lastError: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                // Rate limiting
                rateLimitMutex.withLock {
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastRequestTime
                    if (elapsed < RATE_LIMIT_MS) {
                        delay(RATE_LIMIT_MS - elapsed)
                    }
                    lastRequestTime = System.currentTimeMillis()
                }

                val combinedText = texts.joinToString(BATCH_SEPARATOR)
                val response = executeTranslationRequest(combinedText)
                val translatedParts = parseResponse(response)

                // Split by the same separator and map back
                return translatedParts
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Translation attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }

        Log.e(TAG, "Translation failed after $MAX_RETRIES attempts", lastError)
        return texts // Return originals on failure
    }

    private fun executeTranslationRequest(text: String): String {
        val formBody = FormBody.Builder()
            .add("client", "gtx")
            .add("sl", "en")
            .add("tl", "zh-CN")
            .add("dt", "t")
            .add("q", text)
            .build()

        val request = Request.Builder()
            .url(BASE_URL)
            .post(formBody)
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }
            return response.body?.string()
                ?: throw RuntimeException("Empty response body")
        }
    }

    private fun parseResponse(responseBody: String): List<String> {
        val results = mutableListOf<String>()
        val translatedChunks = mutableListOf<String>()

        try {
            val jsonArray = JSONArray(responseBody)

            // The first element contains the translations
            if (jsonArray.length() > 0) {
                val translations = jsonArray.getJSONArray(0)

                for (i in 0 until translations.length()) {
                    val translationArray = translations.getJSONArray(i)
                    if (translationArray.length() > 0) {
                        val translatedText = translationArray.getString(0)
                        translatedChunks.add(translatedText)
                    }
                }
            }

            // Join chunks and split by our batch separator
            val fullTranslation = translatedChunks.joinToString("")
            val parts = fullTranslation.split(BATCH_SEPARATOR)

            return if (parts.size > 1) {
                parts
            } else {
                listOf(fullTranslation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse translation response", e)
            return listOf(responseBody)
        }
    }

    private fun isLikelyChinese(text: String): Boolean {
        val chineseCharCount = text.count { it.code in 0x4E00..0x9FFF }
        return chineseCharCount > text.length * 0.3
    }
}
