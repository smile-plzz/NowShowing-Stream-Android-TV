package com.nowshowing.tv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val DEFAULT_OMDB_KEY = "f5dad388"
    fun metadataKeys(context: android.content.Context): List<String> {
        // Reuse the legacy preference so single-key installations upgrade without re-entry.
        val stored = context.getSharedPreferences("metadata", android.content.Context.MODE_PRIVATE)
            .getString("key", null).orEmpty()
        return MetadataCredentials.parse(stored)?.takeIf { it.isNotEmpty() } ?: listOf(DEFAULT_OMDB_KEY)
    }

    private const val BASE_URL = "https://www.omdbapi.com/"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // URLs contain the metadata API credential.
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val service: OmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmdbApiService::class.java)
    }
}
