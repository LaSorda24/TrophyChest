package com.gonzalez.trophychest.data

import android.content.Context
import com.gonzalez.trophychest.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// APUNTE: IGDB DA CATALOGO DE JUEGOS; NO ES UNA CUENTA DE USUARIO NI GUARDA TROFEOS.
object IGDBRepository {
    private const val MAX_RECOMMENDATION_GENRES = 5
    private const val PREFS_NAME = "igdb_repository"
    private const val KEY_CACHED_EXPLORE = "cached_explore_v2_indies"
    private const val EXPLORE_CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    private val gson = Gson()

    val categories: List<IGDBCategoryDefinition> = listOf(
        IGDBCategoryDefinition(slug = "accion", displayName = "ACCION", igdbGenreName = "Action"),
        IGDBCategoryDefinition(slug = "aventura", displayName = "AVENTURA", igdbGenreName = "Adventure"),
        IGDBCategoryDefinition(slug = "rpg", displayName = "RPG", igdbGenreName = "Role-playing (RPG)"),
        IGDBCategoryDefinition(slug = "terror", displayName = "TERROR", igdbGenreName = "Horror"),
        IGDBCategoryDefinition(slug = "indie", displayName = "INDIE", igdbGenreName = "Indie"),
        IGDBCategoryDefinition(slug = "estrategia", displayName = "ESTRATEGIA", igdbGenreName = "Strategy"),
        IGDBCategoryDefinition(slug = "deportes", displayName = "DEPORTES", igdbGenreName = "Sport"),
        IGDBCategoryDefinition(slug = "simulacion", displayName = "SIMULACION", igdbGenreName = "Simulator"),
        IGDBCategoryDefinition(slug = "carreras", displayName = "CARRERAS", igdbGenreName = "Racing")
    )

    fun hasEndpoint(): Boolean = resolvedProxyBaseUrl().isNotBlank()

    fun categoryForSlug(slug: String): IGDBCategoryDefinition? {
        return categories.firstOrNull { it.slug == slug.trim().lowercase() }
    }

    suspend fun getExploreContent(
        context: Context,
        forceRefresh: Boolean = false
    ): Result<IGDBExploreContent> = withContext(Dispatchers.IO) {
        val recommendationGenres = buildExploreGenreFilters(context)
        val genreQuery = recommendationGenres.toGenreQueryParameter()
        val cached = loadExploreCache(context, genreQuery)
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && isCacheFresh(cached.fetchedAtMillis, now, EXPLORE_CACHE_TTL_MILLIS)) {
            return@withContext Result.success(cached.content)
        }

        val baseUrl = resolvedProxyBaseUrl()
        if (baseUrl.isBlank()) {
            if (cached != null) {
                return@withContext Result.success(cached.content)
            }
            return@withContext Result.failure(
                IllegalStateException(
                    "Configura IGDB_PROXY_BASE_URL o reutiliza RELEASE_CALENDAR_BASE_URL para activar Explorar con IGDB."
                )
            )
        }

        val result = runCatching {
            RetrofitInstance.igdbProxyApi(baseUrl)
                .getExploreGames(genreQuery, cacheBuster())
                .let(IGDBMapper::mapExploreContent)
        }

        result.onSuccess { content -> saveExploreCache(context, genreQuery, content) }
        result.getOrNull()?.let { content -> return@withContext Result.success(content) }
        cached?.let { return@withContext Result.success(it.content) }
        result
    }

    suspend fun getCategoryGames(context: Context, categorySlug: String): Result<IGDBCategoryContent> = withContext(Dispatchers.IO) {
        val category = categoryForSlug(categorySlug)
            ?: return@withContext Result.failure(IllegalArgumentException("Categoria de IGDB no reconocida."))
        val baseUrl = resolvedProxyBaseUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Configura IGDB_PROXY_BASE_URL o reutiliza RELEASE_CALENDAR_BASE_URL para activar categorias con IGDB."
                )
            )
        }

        runCatching {
            RetrofitInstance.igdbProxyApi(baseUrl)
                .getCategoryGames(category.igdbGenreName)
                .let { response -> IGDBMapper.mapCategoryContent(response, category) }
        }
    }

    suspend fun searchGames(query: String): Result<List<IGDBGame>> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        val baseUrl = resolvedProxyBaseUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Configura IGDB_PROXY_BASE_URL o reutiliza RELEASE_CALENDAR_BASE_URL para activar IGDB."
                )
            )
        }

        runCatching {
            RetrofitInstance.igdbProxyApi(baseUrl)
                .searchGames(normalizedQuery, cacheBuster())
                .games
                .mapNotNull(IGDBMapper::mapSearchGame)
        }
    }

    suspend fun getGameDetails(gameId: String): Result<Juego> = withContext(Dispatchers.IO) {
        val numericId = gameId.toLongOrNull()
            ?: return@withContext Result.failure(
                IllegalArgumentException("Identificador de IGDB no valido.")
            )

        val baseUrl = resolvedProxyBaseUrl()
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Configura IGDB_PROXY_BASE_URL o reutiliza RELEASE_CALENDAR_BASE_URL para cargar detalles desde IGDB."
                )
            )
        }

        runCatching {
            val dto = RetrofitInstance.igdbProxyApi(baseUrl).getGameDetails(numericId, cacheBuster())
            val game = IGDBMapper.mapDetailToJuego(dto)
                ?: throw IllegalStateException("IGDB no ha devuelto un detalle valido para este juego.")
            game.withSteamStoreFallbacks()
        }
    }

    internal fun proxyBaseUrl(igdbProxyBaseUrl: String, releaseCalendarBaseUrl: String): String {
        val directValue = igdbProxyBaseUrl.trim()
        if (directValue.isNotBlank()) return directValue
        return releaseCalendarBaseUrl.trim()
    }

    internal fun recommendationGenresFromCachedGames(
        steamGames: List<Juego>,
        playStationGames: List<Juego>
    ): List<String> {
        return ExploreGenreProfileBuilder.buildFromCachedGames(
            steamGames = steamGames,
            playStationGames = playStationGames,
            maxGenres = MAX_RECOMMENDATION_GENRES
        )
    }

    internal fun List<String>.toGenreQueryParameter(): String? {
        return ExploreGenreProfileBuilder
            .mergeGenreLists(this, emptyList(), MAX_RECOMMENDATION_GENRES)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
    }

    internal fun isCacheFresh(fetchedAtMillis: Long, nowMillis: Long, ttlMillis: Long): Boolean {
        return fetchedAtMillis > 0L && nowMillis - fetchedAtMillis in 0 until ttlMillis
    }

    private fun buildExploreGenreFilters(context: Context): List<String> {
        val steamGames = SteamRepository.getCachedGames(context)
        val playStationGames = PlayStationRepository.getCachedGames(context) +
            PlayStationRepository.getCachedTrophyGames(context)
        return recommendationGenresFromCachedGames(steamGames, playStationGames)
    }

    private fun resolvedProxyBaseUrl(): String {
        return proxyBaseUrl(
            igdbProxyBaseUrl = BuildConfig.IGDB_PROXY_BASE_URL,
            releaseCalendarBaseUrl = BuildConfig.RELEASE_CALENDAR_BASE_URL
        )
    }

    private fun cacheBuster(): String {
        return "${BuildConfig.VERSION_CODE}-${System.currentTimeMillis() / 60_000L}"
    }

    private fun saveExploreCache(context: Context, genreQuery: String?, content: IGDBExploreContent) {
        val entry = IGDBExploreCacheEntry(
            fetchedAtMillis = System.currentTimeMillis(),
            genreQuery = genreQuery,
            content = content
        )
        prefs(context).edit()
            .putString(exploreCacheKey(genreQuery), gson.toJson(entry))
            .apply()
    }

    private fun loadExploreCache(context: Context, genreQuery: String?): IGDBExploreCacheEntry? {
        val json = prefs(context).getString(exploreCacheKey(genreQuery), null) ?: return null
        return runCatching { gson.fromJson(json, IGDBExploreCacheEntry::class.java) }.getOrNull()
    }

    private fun exploreCacheKey(genreQuery: String?): String {
        return "$KEY_CACHED_EXPLORE::${genreQuery?.takeIf { it.isNotBlank() } ?: "default"}"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private suspend fun Juego.withSteamStoreFallbacks(): Juego {
        val appId = steamAppId?.takeIf { it.isNotBlank() } ?: return this
        val details = SteamRepository.getSpanishStoreDetails(appId).getOrNull()
        val spanishDescription = details
            ?.shortDescription
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val steamAgeRating = SteamRepository.pegiAgeRatingFromStoreDetails(details)

        return copy(
            shortDescription = spanishDescription ?: shortDescription,
            ageRating = ageRating ?: steamAgeRating
        )
    }
}
