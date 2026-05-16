package com.gonzalez.trophychest.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// RETROFITINSTANCE ES LA FABRICA CENTRAL DE CLIENTES HTTP DE LA APP.
object RetrofitInstance {
    private const val STEAM_API_BASE_URL = "https://api.steampowered.com/"
    private const val STEAM_STORE_BASE_URL = "https://store.steampowered.com/"
    private val retrofitByBaseUrl = ConcurrentHashMap<String, Retrofit>()
    private val httpClient: OkHttpClient by lazy {
        // OKHTTP CONFIGURA TIMEOUTS Y DNS PARA QUE LAS LLAMADAS AL WORKER SEAN MAS ESTABLES.
        OkHttpClient.Builder()
            .dns(WorkersDevFallbackDns())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val steamApi: SteamApiService by lazy {
        // LAZY SIGNIFICA QUE EL SERVICIO SE CREA SOLO CUANDO SE USA POR PRIMERA VEZ.
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
        // USO CACHE POR BASEURL PARA NO CREAR RETROFIT NUEVO EN CADA LLAMADA.
        val normalizedBaseUrl = baseUrl.ensureTrailingSlash()
        return retrofitByBaseUrl.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}

internal class WorkersDevFallbackDns(
    private val systemDns: Dns = Dns.SYSTEM,
    private val fallbackHost: String = "workers.dev",
    private val workerHosts: Set<String> = setOf("trophychest-igdb-proxy.trophychest.workers.dev")
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        // SI EL DOMINIO DEL WORKER FALLA, INTENTO RESOLVER WORKERS.DEV COMO FALLBACK.
        val systemResult = runCatching { systemDns.lookup(hostname) }
        if (hostname.lowercase() !in workerHosts) {
            return systemResult.getOrThrow()
        }

        val fallbackAddresses = runCatching { systemDns.lookup(fallbackHost) }.getOrDefault(emptyList())
        val systemAddresses = systemResult.getOrDefault(emptyList())
        val orderedAddresses = orderedWorkerAddresses(
            fallbackAddresses = fallbackAddresses,
            systemAddresses = systemAddresses
        )

        if (orderedAddresses.isNotEmpty()) return orderedAddresses
        return systemResult.getOrThrow()
    }

    internal fun orderedWorkerAddresses(
        fallbackAddresses: List<InetAddress>,
        systemAddresses: List<InetAddress>
    ): List<InetAddress> {
        return (fallbackAddresses + systemAddresses)
            .distinctBy { it.hostAddress }
    }
}
