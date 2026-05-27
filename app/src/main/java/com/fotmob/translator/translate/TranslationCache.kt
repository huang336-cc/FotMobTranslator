package com.fotmob.translator.translate

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class TranslationCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("translation_cache", Context.MODE_PRIVATE)

    private val memoryCache = LruCache<String, String>(5000)
    private val mutex = Mutex()

    companion object {
        private const val MAX_CACHE_SIZE = 5000
        private const val KEY_CACHE_SIZE = "cache_size"
        private const val KEY_PREFIX = "trans_"
    }

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val size = prefs.getInt(KEY_CACHE_SIZE, 0)
        for (i in 0 until size.coerceAtMost(MAX_CACHE_SIZE)) {
            val key = prefs.getString("${KEY_PREFIX}key_$i", null) ?: continue
            val value = prefs.getString("${KEY_PREFIX}val_$i", null) ?: continue
            memoryCache.put(key, value)
        }
    }

    suspend fun get(key: String): String? = mutex.withLock {
        memoryCache.get(key)
    }

    suspend fun put(key: String, value: String) = mutex.withLock {
        if (memoryCache.get(key) == null) {
            memoryCache.put(key, value)
            saveToDisk(key, value)
        }
    }

    private fun saveToDisk(key: String, value: String) {
        val size = prefs.getInt(KEY_CACHE_SIZE, 0)
        if (size >= MAX_CACHE_SIZE) {
            return
        }

        prefs.edit()
            .putString("${KEY_PREFIX}key_$size", key)
            .putString("${KEY_PREFIX}val_$size", value)
            .putInt(KEY_CACHE_SIZE, size + 1)
            .apply()
    }

    fun contains(key: String): Boolean {
        return memoryCache.get(key) != null
    }

    fun size(): Int {
        return prefs.getInt(KEY_CACHE_SIZE, 0)
    }

    fun clear() {
        memoryCache.evictAll()
        prefs.edit().clear().apply()
    }
}
