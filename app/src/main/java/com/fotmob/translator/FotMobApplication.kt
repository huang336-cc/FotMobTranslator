package com.fotmob.translator

import android.app.Application
import com.fotmob.translator.data.FotMobApi
import com.fotmob.translator.data.repository.FotMobRepository
import com.fotmob.translator.translate.GoogleTranslator
import com.fotmob.translator.translate.TranslationCache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class FotMobApplication : Application() {

    lateinit var repository: FotMobRepository
        private set
    lateinit var translator: GoogleTranslator
        private set
    lateinit var translationCache: TranslationCache
        private set
    lateinit var api: FotMobApi
        private set

    override fun onCreate() {
        super.onCreate()

        translationCache = TranslationCache(this)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.fotmob.com/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(FotMobApi::class.java)
        repository = FotMobRepository(api, this)
        translator = GoogleTranslator(translationCache)
    }
}
