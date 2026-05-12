package com.gonzalez.trophychest.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

object RetrofitInstance {
    private const val STEAM_API_BASE_URL = "https://api.steampowered.com/"
    private const val STEAM_STORE_BASE_URL = "https://store.steampowered.com/"
    private val retrofitByBaseUrl = ConcurrentHashMap<String, Retrofit>()

    val steamApi: SteamApiService by lazy {
        Retrofit.Builder()
            .baseUrl(STEAM_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SteamApiService::class.java)
    }

    val steamStoreApi: SteamStoreApiService by lazy {
        Retrofit.Builder()
            .baseUrl(STEAM_STORE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SteamStoreApiService::class.java)
    }

    fun releaseCalendarApi(baseUrl: String): ReleaseCalendarApiService {
        return retrofitForBaseUrl(baseUrl)
            .create(ReleaseCalendarApiService::class.java)
    }

    fun igdbProxyApi(baseUrl: String): IGDBProxyApiService {
        return retrofitForBaseUrl(baseUrl)
            .create(IGDBProxyApiService::class.java)
    }

    fun playStationApi(baseUrl: String): PlayStationApiService {
        return retrofitForBaseUrl(baseUrl)
            .create(PlayStationApiService::class.java)
    }

    private fun retrofitForBaseUrl(baseUrl: String): Retrofit {
        val normalizedBaseUrl = baseUrl.ensureTrailingSlash()
        return retrofitByBaseUrl.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
