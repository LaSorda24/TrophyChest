package com.gonzalez.trophychest.data

import android.content.Context
import android.content.SharedPreferences
import com.gonzalez.trophychest.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException

// ESTE REPOSITORIO CENTRALIZA STEAM: VINCULAR CUENTA, CACHEAR JUEGOS Y CARGAR LOGROS.
object SteamRepository {
    private const val PREFS_NAME = "steam_repository_prefs"
    private const val KEY_LINKED_ACCOUNT = "linked_account"
    private const val KEY_CACHED_GAMES = "cached_games"
    private const val KEY_CACHED_DETAILS = "cached_details_v3"
    private const val KEY_CACHED_ACHIEVEMENTS_LEGACY = "cached_achievements"
    private const val KEY_CACHED_ACHIEVEMENTS = "cached_achievements_v2"
    private const val KEY_LEGACY_STORAGE_CLEARED = "legacy_storage_cleared_v2"

    private val gson = Gson()
    private val legacyStorageKeys = listOf(
        KEY_LINKED_ACCOUNT,
        KEY_CACHED_GAMES,
        KEY_CACHED_DETAILS,
        KEY_CACHED_ACHIEVEMENTS_LEGACY,
        KEY_CACHED_ACHIEVEMENTS
    )

    fun hasApiKey(): Boolean = BuildConfig.STEAM_API_KEY.isNotBlank()

    fun getLinkedAccount(context: Context): SteamLinkedAccount? {
        // LA CUENTA VINCULADA SE GUARDA LOCALMENTE Y SEPARADA POR UID DE FIREBASE.
        val json = readScopedJson(context, KEY_LINKED_ACCOUNT) ?: return null
        return runCatching { gson.fromJson(json, SteamLinkedAccount::class.java) }.getOrNull()
    }

    fun getCachedGames(context: Context): List<Juego> {
        val json = readScopedJson(context, KEY_CACHED_GAMES) ?: return emptyList()
        val type = object : TypeToken<List<Juego>>() {}.type
        return runCatching { gson.fromJson<List<Juego>>(json, type) }.getOrDefault(emptyList())
    }

    fun getCachedGame(context: Context, gameId: String): Juego? {
        return getCachedGames(context).firstOrNull { it.platformGameId == gameId }
    }



    //VINCULACION DE STEAM
    suspend fun importLibrary(context: Context, profileInput: String): SteamSyncResult = withContext(Dispatchers.IO) {
        // WITHCONTEXT(IO) MUEVE RED Y DISCO FUERA DEL HILO PRINCIPAL.
        if (!hasApiKey()) return@withContext SteamSyncResult.MissingApiKey
        val currentUid = currentUserUid()
            ?: return@withContext SteamSyncResult.Failure("Inicia sesion para vincular una cuenta de Steam.")

        val resolvedAccount = resolveProfileInput(profileInput)
        resolvedAccount.fold(
            onSuccess = { linkedAccount ->
                val ownedGamesResponse = runCatching {
                    // AQUI SE HACE LA LLAMADA REAL A LA API OFICIAL DE STEAM PARA JUEGOS DEL USUARIO.
                    RetrofitInstance.steamApi.getOwnedGames(BuildConfig.STEAM_API_KEY, linkedAccount.steamId)
                }.getOrElse { throwable ->
                    return@withContext SteamSyncResult.Failure(errorMessageFor(throwable))
                }

                val games = ownedGamesResponse.response.games.map { it.toJuego() }.sortedForHome()
                val warningMessage = if (games.isEmpty()) {
                    "No se ha podido leer la biblioteca. El perfil puede ser privado o no tener juegos visibles."
                } else {
                    null
                }

                saveLinkedAccount(
                    context,
                    currentUid,
                    linkedAccount.copy(isPublicProfile = games.isNotEmpty())
                )
                saveGames(context, currentUid, games)
                SteamSyncResult.Success(
                    linkedAccount = linkedAccount.copy(isPublicProfile = games.isNotEmpty()),
                    games = games,
                    warningMessage = warningMessage
                )
            },
            onFailure = { throwable ->
                SteamSyncResult.Failure(throwable.message ?: "No se pudo vincular la cuenta de Steam.")
            }
        )
    }


    //CACHE STEAM
    suspend fun getGameDetails(context: Context, gameId: String): Result<Juego> = withContext(Dispatchers.IO) {
        // PRIMERO BUSCO EN CACHE; SI NO HAY DATOS SUFICIENTES, PIDO DETALLES A STEAM STORE.
        val linkedGame = getCachedGame(context, gameId)
        val baseGame = linkedGame ?: gameId.toIntOrNull()?.let { appId ->
            Juego(
                platform = PlataformaJuego.STEAM,
                platformGameId = appId.toString(),
                title = "Steam App $appId",
                headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg",
                capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg",
                heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$appId/capsule_616x353.jpg",
                iconImageUrl = null,
                steamAppId = appId.toString()
            )
        } ?: return@withContext Result.failure(IllegalStateException("Identificador de Steam no valido."))

        val cachedDetails = loadDetailsCache(context)[gameId]
        if (cachedDetails != null) {
            return@withContext Result.success(baseGame.mergeWithDetails(cachedDetails))
        }

        runCatching {
            val response = RetrofitInstance.steamStoreApi.getAppDetails(gameId)
            val details = response[gameId]?.takeIf { it.success }?.data
            if (details == null && linkedGame == null) {
                throw IllegalStateException("Steam no ha devuelto detalles para este juego.")
            }
            val merged = baseGame.mergeWithDetails(details)
            saveDetailsCache(context, gameId, details)
            if (linkedGame != null) {
                replaceCachedGame(context, merged)
            }
            merged
        }
    }

    suspend fun getSpanishStoreDetails(steamAppId: String): Result<SteamStoreAppDetails?> = withContext(Dispatchers.IO) {
        val appId = steamAppId.toIntOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("AppID de Steam no valido."))

        runCatching {
            RetrofitInstance.steamStoreApi
                .getAppDetails(appId.toString())
                .get(appId.toString())
                ?.takeIf { it.success }
                ?.data
        }
    }

    suspend fun getGamesForHome(context: Context): Result<List<Juego>> = withContext(Dispatchers.IO) {
        val cached = getCachedGames(context).sortedForHome()
        Result.success(cached)
    }

    //JUEGOS PRINCIPAL
    suspend fun getGamesWithAchievementSummaries(context: Context): Result<List<Juego>> = withContext(Dispatchers.IO) {
        // ESTA FUNCION PREPARA LA LISTA DE JUEGOS QUE APARECE EN LA PANTALLA DE TROFEOS.
        val linkedAccount = getLinkedAccount(context)
            ?: return@withContext Result.failure(IllegalStateException("Vincula una cuenta de Steam para ver tus trofeos."))
        val currentUid = currentUserUid()
            ?: return@withContext Result.failure(IllegalStateException("Inicia sesion para ver tus trofeos."))

        val games = getCachedGames(context)
        if (games.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val achievementCache = loadAchievementCache(context).toMutableMap()
        val semaphore = Semaphore(6)

        val updatedGames = coroutineScope {
            games.map { game ->
                async {
                    semaphore.withPermit {
                        val currentBundle = achievementCache[game.platformGameId]
                        if (currentBundle != null && shouldUseCachedAchievementBundle(currentBundle)) {
                            game.copy(achievementSummary = currentBundle.summary)
                        } else {
                            achievementCache.remove(game.platformGameId)
                            val bundle = fetchAchievementBundle(linkedAccount.steamId, game)
                            if (bundle != null && bundle.hasAchievements) {
                                achievementCache[game.platformGameId] = bundle
                            }
                            game.copy(achievementSummary = bundle?.summary)
                        }
                    }
                }
            }.awaitAll()
        }

        saveAchievementCache(context, achievementCache)
        saveGames(context, currentUid, updatedGames)

        Result.success(
            updatedGames
                .filter { it.achievementSummary != null }
                .sortedByDescending { it.achievementSummary?.porcentajeCompletado ?: 0f }
        )
    }

    suspend fun getAchievementBundle(context: Context, gameId: String): Result<SteamAchievementBundle> = withContext(Dispatchers.IO) {
        // BUNDLE SIGNIFICA PAQUETE: JUEGO + LISTA DE LOGROS + RESUMEN.
        val game = getCachedGame(context, gameId) ?: publicSteamGame(gameId)
            ?: return@withContext Result.failure(IllegalStateException("Identificador de Steam no valido."))
        val linkedAccount = getLinkedAccount(context)
            ?: return@withContext getPublicAchievementBundle(context, gameId, game.title)

        val updatedCache = loadAchievementCache(context).toMutableMap()
        val cached = updatedCache[gameId]
        if (cached != null && shouldUseCachedAchievementBundle(cached)) {
            return@withContext Result.success(cached)
        }
        updatedCache.remove(gameId)

        val bundle = fetchAchievementBundle(linkedAccount.steamId, game)
            ?: return@withContext getPublicAchievementBundle(context, gameId, game.title)

        updatedCache.apply {
            if (bundle.hasAchievements) {
                put(gameId, bundle)
            }
        }
        saveAchievementCache(context, updatedCache)
        replaceCachedGame(context, game.copy(achievementSummary = bundle.summary))
        Result.success(bundle)
    }

    suspend fun getPublicAchievementBundle(
        context: Context,
        steamAppId: String,
        fallbackTitle: String
    ): Result<SteamAchievementBundle> = withContext(Dispatchers.IO) {
        val appId = steamAppId.toIntOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("AppID de Steam no valido."))
        if (!hasApiKey()) {
            return@withContext Result.failure(IllegalStateException(steamSetupMessage()))
        }

        runCatching {
            val schema = RetrofitInstance.steamApi.getSchemaForGame(BuildConfig.STEAM_API_KEY, appId)
                .game
            val schemaAchievements = schema?.availableGameStats?.achievements.orEmpty()
            val globalPercentages = runCatching {
                RetrofitInstance.steamApi.getGlobalAchievementPercentages(appId)
                    .percentages
                    ?.achievements
                    ?.associateBy { it.apiName }
                    .orEmpty()
            }.getOrDefault(emptyMap())

            buildPublicAchievementBundle(
                appId = steamAppId,
                fallbackTitle = fallbackTitle,
                schema = schema,
                globalPercentages = globalPercentages
            ).also { bundle ->
                if (!bundle.hasAchievements) {
                    throw IllegalStateException("Este juego no expone logros publicos en Steam.")
                }
            }
        }
    }

    fun unlinkSteam(context: Context) {
        val currentUid = currentUserUid() ?: return
        clearScopedStorage(context, currentUid)
    }

    fun steamSetupMessage(): String {
        return "Configura STEAM_API_KEY en gradle.properties o local.properties para activar la sincronizacion con Steam."
    }


    private suspend fun resolveProfileInput(profileInput: String): Result<SteamLinkedAccount> = withContext(Dispatchers.IO) {
        // EL USUARIO PUEDE PEGAR STEAMID64 O URL; AQUI LO CONVIERTO A STEAMID REAL.
        val trimmedInput = profileInput.trim()
        if (trimmedInput.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Introduce un SteamID64 o una URL publica de Steam."))
        }

        val directSteamId = SteamInputParser.extractDirectSteamId(trimmedInput)
        val steamId = if (directSteamId != null) {
            directSteamId
        } else {
            val vanitySegment = SteamInputParser.extractVanitySegment(trimmedInput)
                ?: return@withContext Result.failure(IllegalArgumentException("No se ha podido reconocer ese identificador de Steam."))
            val resolveResponse = RetrofitInstance.steamApi.resolveVanityUrl(BuildConfig.STEAM_API_KEY, vanitySegment)
            if (resolveResponse.response.success != 1 || resolveResponse.response.steamId.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("No se encontro ese perfil de Steam. Revisa la URL o el SteamID.")
                )
            }
            resolveResponse.response.steamId
        }

        val summary = runCatching {
            RetrofitInstance.steamApi.getPlayerSummaries(BuildConfig.STEAM_API_KEY, steamId)
                .response.players.firstOrNull()
        }.getOrNull()

        Result.success(
            SteamLinkedAccount(
                steamId = steamId,
                profileInput = trimmedInput,
                displayName = summary?.personaName,
                avatarUrl = summary?.avatarUrl,
                isPublicProfile = true
            )
        )
    }

    private suspend fun fetchAchievementBundle(steamId: String, game: Juego): SteamAchievementBundle? {
        // PARA MOSTRAR LOGROS NECESITO DOS COSAS: PROGRESO DEL USUARIO Y ESQUEMA/NOMBRES DEL JUEGO.
        val appId = game.platformGameId.toIntOrNull() ?: return null
        val playerAchievements = runCatching {
            RetrofitInstance.steamApi.getPlayerAchievements(BuildConfig.STEAM_API_KEY, steamId, appId)
        }.getOrElse { throwable ->
            if (throwable is HttpException) return null
            throw throwable
        }

        val schemaAchievements = runCatching {
            RetrofitInstance.steamApi.getSchemaForGame(BuildConfig.STEAM_API_KEY, appId)
                .game
                ?.availableGameStats
                ?.achievements
                .orEmpty()
        }.getOrDefault(emptyList())

        val playerStats = playerAchievements.playerStats ?: return null

        val globalPercentages = runCatching {
            RetrofitInstance.steamApi.getGlobalAchievementPercentages(appId)
                .percentages
                ?.achievements
                ?.associateBy { it.apiName }
                .orEmpty()
        }.getOrDefault(emptyMap())

        return buildAchievementBundle(
            game = game,
            playerStats = playerStats,
            schemaAchievements = schemaAchievements,
            globalPercentages = globalPercentages
        )
    }

    internal fun buildAchievementBundle(
        game: Juego,
        playerStats: PlayerStatsPayload,
        schemaAchievements: List<SteamSchemaAchievement>,
        globalPercentages: Map<String, GlobalAchievementPercentage>
    ): SteamAchievementBundle {
        if (schemaAchievements.isEmpty() && playerStats.achievements.isEmpty()) {
            return SteamAchievementBundle(
                gameId = game.platformGameId,
                gameTitle = playerStats.gameName ?: game.title,
                summary = null,
                achievements = emptyList(),
                hasAchievements = false
            )
        }

        val playerAchievementsByName = playerStats.achievements.associateBy { it.apiName }
        val schemaAchievementsByName = schemaAchievements.associateBy { it.apiName }
        val orderedApiNames = if (schemaAchievements.isNotEmpty()) {
            schemaAchievements.map { it.apiName } +
                playerStats.achievements.map { it.apiName }.filterNot(schemaAchievementsByName::containsKey)
        } else {
            playerStats.achievements.map { it.apiName }
        }

        val achievements = orderedApiNames.distinct().map { apiName ->
            val schema = schemaAchievementsByName[apiName]
            val playerAchievement = playerAchievementsByName[apiName]
            SteamAchievement(
                apiName = apiName,
                title = schema?.displayName?.takeIf { it.isNotBlank() }
                    ?: playerAchievement?.displayName?.takeIf { it.isNotBlank() }
                    ?: apiName,
                description = schema?.description?.takeIf { it.isNotBlank() }
                    ?: playerAchievement?.description?.takeIf { it.isNotBlank() }
                    ?: "",
                unlocked = playerAchievement?.achieved == 1,
                unlockTime = playerAchievement?.unlockTime?.takeIf { it > 0 },
                globalPercentage = globalPercentages[apiName]?.percent,
                iconUrl = schema?.icon,
                lockedIconUrl = schema?.iconGray
            )
        }

        val unlocked = achievements.count { it.unlocked }
        val total = achievements.size
        val summary = ResumenTrofeos(
            total = total,
            desbloqueados = unlocked,
            porcentajeCompletado = if (total == 0) 0f else unlocked.toFloat() / total.toFloat()
        )

        return SteamAchievementBundle(
            gameId = game.platformGameId,
            gameTitle = playerStats.gameName ?: game.title,
            summary = summary,
            achievements = achievements,
            hasAchievements = true
        )
    }

    internal fun buildPublicAchievementBundle(
        appId: String,
        fallbackTitle: String,
        schema: SteamGameSchema?,
        globalPercentages: Map<String, GlobalAchievementPercentage>
    ): SteamAchievementBundle {
        val schemaAchievements = schema?.availableGameStats?.achievements.orEmpty()
        val achievements = schemaAchievements.map { achievement ->
            SteamAchievement(
                apiName = achievement.apiName,
                title = achievement.displayName.takeIf { it.isNotBlank() } ?: achievement.apiName,
                description = achievement.description.orEmpty(),
                unlocked = false,
                unlockTime = null,
                globalPercentage = globalPercentages[achievement.apiName]?.percent,
                iconUrl = achievement.icon,
                lockedIconUrl = achievement.iconGray ?: achievement.icon
            )
        }
        val total = achievements.size
        return SteamAchievementBundle(
            gameId = appId,
            gameTitle = schema?.gameName?.takeIf { it.isNotBlank() } ?: fallbackTitle,
            summary = if (total > 0) {
                ResumenTrofeos(total = total, desbloqueados = 0, porcentajeCompletado = 0f)
            } else {
                null
            },
            achievements = achievements,
            hasAchievements = achievements.isNotEmpty(),
            isPersonal = false
        )
    }

    internal fun shouldUseCachedAchievementBundle(bundle: SteamAchievementBundle?): Boolean {
        return bundle != null && bundle.hasAchievements && bundle.achievements.isNotEmpty()
    }

    private fun SteamGame.toJuego(): Juego {
        val appIdText = appid.toString()
        val iconUrl = if (!iconHash.isNullOrBlank()) {
            "https://media.steampowered.com/steamcommunity/public/images/apps/$appid/$iconHash.jpg"
        } else {
            null
        }

        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = appIdText,
            title = name.ifBlank { "Steam App $appid" },
            headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appid/header.jpg",
            capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appid/library_600x900.jpg",
            heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$appid/capsule_616x353.jpg",
            iconImageUrl = iconUrl,
            playtimeMinutes = playtimeMinutes,
            lastPlayedEpochSeconds = lastPlayedEpochSeconds,
            steamAppId = appIdText
        )
    }

    private fun publicSteamGame(gameId: String): Juego? {
        val appId = gameId.toIntOrNull() ?: return null
        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = appId.toString(),
            title = "Steam App $appId",
            headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg",
            capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg",
            heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$appId/capsule_616x353.jpg",
            iconImageUrl = null,
            steamAppId = appId.toString()
        )
    }

    private fun Juego.mergeWithDetails(details: SteamStoreAppDetails?): Juego {
        if (details == null) return this

        return copy(
            title = details.name ?: title,
            headerImageUrl = details.headerImage ?: headerImageUrl,
            heroImageUrl = details.backgroundRawImage ?: details.backgroundImage ?: heroImageUrl,
            shortDescription = details.shortDescription ?: shortDescription,
            ageRating = ageRating ?: pegiAgeRatingFromStoreDetails(details),
            developers = details.developers.ifEmpty { developers },
            genres = details.genres.map { it.description }.ifEmpty { genres },
            releaseDate = details.releaseDate?.date ?: releaseDate,
            screenshots = details.screenshots.mapNotNull { it.pathFull }.ifEmpty { screenshots }
        )
    }

    internal fun pegiAgeRatingFromStoreDetails(details: SteamStoreAppDetails?): PegiAgeRating? {
        if (details == null) return null

        parsePegiAge(details.ratings?.pegi?.rating)?.let { age ->
            return PegiAgeRating(
                label = "PEGI $age",
                minimumAge = age,
                imageUrl = null
            )
        }

        val requiredAge = details.requiredAge
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != "0" }
            ?.let(::parseAnyAge)
            ?.takeIf { it in 1..21 }
            ?: return null

        return PegiAgeRating(
            label = "+$requiredAge",
            minimumAge = requiredAge,
            imageUrl = null
        )
    }

    private fun parsePegiAge(value: String?): Int? {
        val age = parseAnyAge(value)
        return age?.takeIf { it in setOf(3, 7, 12, 16, 18) }
    }

    private fun parseAnyAge(value: String?): Int? {
        val match = value?.let { Regex("""\d{1,2}""").find(it) } ?: return null
        return match.value.toIntOrNull()
    }

    private fun List<Juego>.sortedForHome(): List<Juego> {
        return sortedWith(
            compareByDescending<Juego> { it.lastPlayedEpochSeconds ?: 0L }
                .thenByDescending { it.playtimeMinutes }
                .thenBy { it.title }
        )
    }

    internal fun scopedPreferenceKey(baseKey: String, uid: String?): String? {
        val normalizedUid = uid?.takeIf { it.isNotBlank() } ?: return null
        return "$baseKey::$normalizedUid"
    }

    internal fun scopedPreferenceKeys(uid: String?): List<String> {
        return legacyStorageKeys.mapNotNull { scopedPreferenceKey(it, uid) }
    }

    private fun prefs(context: Context): SharedPreferences {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureLegacyStorageCleared(sharedPreferences)
        return sharedPreferences
    }

    private fun currentUserUid(): String? {
        return FirebaseManager.currentUser?.uid?.takeIf { it.isNotBlank() }
    }

    private fun ensureLegacyStorageCleared(sharedPreferences: SharedPreferences) {
        if (sharedPreferences.getBoolean(KEY_LEGACY_STORAGE_CLEARED, false)) return

        sharedPreferences.edit().apply {
            legacyStorageKeys.forEach(::remove)
            putBoolean(KEY_LEGACY_STORAGE_CLEARED, true)
        }.apply()
    }

    private fun readScopedJson(context: Context, baseKey: String): String? {
        // SCOPED SIGNIFICA QUE CADA USUARIO TIENE SUS PROPIAS CLAVES DE CACHE.
        val currentUid = currentUserUid() ?: return null
        val key = scopedPreferenceKey(baseKey, currentUid) ?: return null
        return prefs(context).getString(key, null)
    }

    private fun writeScopedJson(context: Context, uid: String, baseKey: String, value: String) {
        val key = scopedPreferenceKey(baseKey, uid) ?: return
        prefs(context).edit().putString(key, value).apply()
    }

    private fun clearScopedStorage(context: Context, uid: String) {
        prefs(context).edit().apply {
            scopedPreferenceKeys(uid).forEach(::remove)
        }.apply()
    }

    private fun saveLinkedAccount(context: Context, uid: String, account: SteamLinkedAccount) {
        writeScopedJson(context, uid, KEY_LINKED_ACCOUNT, gson.toJson(account))
    }

    private fun saveGames(context: Context, uid: String, games: List<Juego>) {
        writeScopedJson(context, uid, KEY_CACHED_GAMES, gson.toJson(games))
    }

    private fun replaceCachedGame(context: Context, juego: Juego) {
        val currentUid = currentUserUid() ?: return
        val updated = getCachedGames(context).map {
            if (it.platformGameId == juego.platformGameId) juego else it
        }
        saveGames(context, currentUid, updated)
    }

    private fun saveDetailsCache(context: Context, gameId: String, details: SteamStoreAppDetails?) {
        if (details == null) return
        val currentUid = currentUserUid() ?: return
        val updated = loadDetailsCache(context).toMutableMap().apply {
            put(gameId, details)
        }
        writeScopedJson(context, currentUid, KEY_CACHED_DETAILS, gson.toJson(updated))
    }

    private fun loadDetailsCache(context: Context): Map<String, SteamStoreAppDetails> {
        val json = readScopedJson(context, KEY_CACHED_DETAILS) ?: return emptyMap()
        val type = object : TypeToken<Map<String, SteamStoreAppDetails>>() {}.type
        return runCatching { gson.fromJson<Map<String, SteamStoreAppDetails>>(json, type) }.getOrDefault(emptyMap())
    }

    private fun saveAchievementCache(context: Context, cache: Map<String, SteamAchievementBundle>) {
        val currentUid = currentUserUid() ?: return
        writeScopedJson(context, currentUid, KEY_CACHED_ACHIEVEMENTS, gson.toJson(cache))
    }

    private fun loadAchievementCache(context: Context): Map<String, SteamAchievementBundle> {
        val json = readScopedJson(context, KEY_CACHED_ACHIEVEMENTS) ?: return emptyMap()
        val type = object : TypeToken<Map<String, SteamAchievementBundle>>() {}.type
        return runCatching { gson.fromJson<Map<String, SteamAchievementBundle>>(json, type) }.getOrDefault(emptyMap())
    }

    private fun errorMessageFor(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> "Steam ha respondido con un error (${throwable.code()}). Intentalo de nuevo."
            else -> throwable.message ?: "No se pudo conectar con Steam."
        }
    }
}

object SteamInputParser {
    // ESTE PARSER ACEPTA TANTO ID DIRECTO COMO URL PUBLICA DE STEAM.
    private val steamIdRegex = Regex("""^\d{17}$""")
    private val profilesRegex = Regex("""steamcommunity\.com/profiles/(\d{17})""", RegexOption.IGNORE_CASE)
    private val vanityRegex = Regex("""steamcommunity\.com/id/([^/?#]+)""", RegexOption.IGNORE_CASE)
    private val bareVanityRegex = Regex("""^[A-Za-z0-9_-]{2,64}$""")

    fun extractDirectSteamId(input: String): String? {
        val trimmed = input.trim()
        if (steamIdRegex.matches(trimmed)) return trimmed
        return profilesRegex.find(trimmed)?.groupValues?.getOrNull(1)
    }

    fun extractVanitySegment(input: String): String? {
        val trimmed = input.trim()
        return when {
            vanityRegex.containsMatchIn(trimmed) -> vanityRegex.find(trimmed)?.groupValues?.getOrNull(1)
            bareVanityRegex.matches(trimmed) -> trimmed
            else -> null
        }
    }
}
