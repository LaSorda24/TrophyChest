package com.gonzalez.trophychest.data

import android.content.Context
import android.content.SharedPreferences
import com.gonzalez.trophychest.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

// PLAYSTATION PASA POR EL PROXY PORQUE LA APP NO DEBE GUARDAR LOGICA SENSIBLE DE PSN.
object PlayStationRepository {
    private const val PREFS_NAME = "playstation_repository_prefs"
    private const val KEY_LINKED_ACCOUNT = "linked_account"
    private const val KEY_CACHED_GAMES = "cached_games"
    private const val KEY_CACHED_TROPHY_GAMES = "cached_trophy_games"
    private const val KEY_CACHED_ACHIEVEMENTS = "cached_achievements"
    private const val KEY_LEGACY_STORAGE_CLEARED = "legacy_storage_cleared_v1"

    private val gson = Gson()
    private val storageKeys = listOf(
        KEY_LINKED_ACCOUNT,
        KEY_CACHED_GAMES,
        KEY_CACHED_TROPHY_GAMES,
        KEY_CACHED_ACHIEVEMENTS
    )

    fun hasProxyEndpoint(): Boolean = BuildConfig.PLAYSTATION_PROXY_BASE_URL.isNotBlank()

    fun setupMessage(): String {
        // PLAYSTATION DEPENDE DEL PROXY; SIN URL NO HAY FORMA SEGURA DE LLAMAR A PSN.
        return if (hasProxyEndpoint()) {
            "PlayStation se vincula con un NPSSO generado desde PlayStation.com."
        } else {
            "Configura PLAYSTATION_PROXY_BASE_URL o IGDB_PROXY_BASE_URL para activar PlayStation."
        }
    }

    fun getLinkedAccount(context: Context): PlayStationLinkedAccount? {
        val json = readScopedJson(context, KEY_LINKED_ACCOUNT) ?: return null
        return runCatching { gson.fromJson(json, PlayStationLinkedAccount::class.java) }.getOrNull()
    }

    fun getCachedGames(context: Context): List<Juego> {
        val json = readScopedJson(context, KEY_CACHED_GAMES) ?: return emptyList()
        val type = object : TypeToken<List<Juego>>() {}.type
        return runCatching { gson.fromJson<List<Juego>>(json, type) }.getOrDefault(emptyList())
    }

    fun getCachedTrophyGames(context: Context): List<Juego> {
        val json = readScopedJson(context, KEY_CACHED_TROPHY_GAMES) ?: return emptyList()
        val type = object : TypeToken<List<Juego>>() {}.type
        return runCatching { gson.fromJson<List<Juego>>(json, type) }.getOrDefault(emptyList())
    }

    fun getCachedGame(context: Context, gameId: String): Juego? {
        return getCachedGames(context).firstOrNull { it.platformGameId == gameId } ?:
            getCachedTrophyGames(context).firstOrNull { it.platformGameId == gameId }
    }

    suspend fun linkWithNpsso(context: Context, npsso: String): PlayStationSyncResult = withContext(Dispatchers.IO) {
        // EL NPSSO NO LO RESUELVE ANDROID; SE MANDA AL PROXY Y ESTE DEVUELVE TOKENS NORMALIZADOS.
        if (!hasProxyEndpoint()) return@withContext PlayStationSyncResult.MissingProxyEndpoint
        val currentUid = currentUserUid()
            ?: return@withContext PlayStationSyncResult.Failure("Inicia sesion para vincular PlayStation.")

        val api = api()
        val authResponse = runCatching {
            api.authWithNpsso(PlayStationNpssoAuthRequest(npsso.trim()))
        }.getOrElse { throwable ->
            return@withContext PlayStationSyncResult.Failure(errorMessageFor(throwable))
        }

        var linkedAccount = authResponse.toLinkedAccount()
        saveLinkedAccount(context, currentUid, linkedAccount)

        val history = runCatching {
            // TITLE HISTORY TRAE LOS JUEGOS JUGADOS RECIENTEMENTE EN PLAYSTATION.
            api.getTitleHistory(PlayStationRefreshRequest(linkedAccount.refreshToken))
        }.getOrElse { throwable ->
            return@withContext PlayStationSyncResult.LinkedOnly(
                linkedAccount = linkedAccount,
                message = "PlayStation vinculada como ${accountDisplayName(linkedAccount)}, pero no se pudieron sincronizar juegos: ${errorMessageFor(throwable)}"
            )
        }
        linkedAccount = linkedAccount.withAuth(history.auth)
        val games = history.titles.mapNotNull { it.toJuego() }.sortedForHome()
        saveLinkedAccount(context, currentUid, linkedAccount)
        saveGames(context, currentUid, games)

        val trophies = runCatching {
            // TROPHY TITLES TRAE LOS JUEGOS QUE TIENEN TROFEOS DISPONIBLES.
            api.getTrophyTitles(PlayStationRefreshRequest(linkedAccount.refreshToken))
        }.getOrElse { throwable ->
            return@withContext PlayStationSyncResult.Success(
                linkedAccount = linkedAccount,
                games = games,
                trophyGames = emptyList(),
                warningMessage = "PlayStation vinculada como ${accountDisplayName(linkedAccount)}, pero no se pudieron sincronizar trofeos: ${errorMessageFor(throwable)}"
            )
        }
            linkedAccount = linkedAccount.withAuth(trophies.auth)
            val trophyGames = trophies.titles.mapNotNull { it.toJuego() }.sortedForTrophies()
            saveLinkedAccount(context, currentUid, linkedAccount)
            saveTrophyGames(context, currentUid, trophyGames)

            PlayStationSyncResult.Success(
                linkedAccount = linkedAccount,
                games = games,
                trophyGames = trophyGames
            )
    }

    suspend fun syncLibrary(context: Context): PlayStationSyncResult = withContext(Dispatchers.IO) {
        // SINCRONIZAR REUTILIZA EL REFRESH TOKEN GUARDADO PARA ACTUALIZAR JUEGOS Y TROFEOS.
        if (!hasProxyEndpoint()) return@withContext PlayStationSyncResult.MissingProxyEndpoint
        val currentUid = currentUserUid()
            ?: return@withContext PlayStationSyncResult.Failure("Inicia sesion para sincronizar PlayStation.")
        var linkedAccount = getLinkedAccount(context)
            ?: return@withContext PlayStationSyncResult.MissingSession

        runCatching {
            val api = api()
            val history = api.getTitleHistory(PlayStationRefreshRequest(linkedAccount.refreshToken))
            linkedAccount = linkedAccount.withAuth(history.auth)
            val games = history.titles.mapNotNull { it.toJuego() }.sortedForHome()
            saveGames(context, currentUid, games)

            val trophies = api.getTrophyTitles(PlayStationRefreshRequest(linkedAccount.refreshToken))
            linkedAccount = linkedAccount.withAuth(trophies.auth)
            val trophyGames = trophies.titles.mapNotNull { it.toJuego() }.sortedForTrophies()
            saveLinkedAccount(context, currentUid, linkedAccount)
            saveTrophyGames(context, currentUid, trophyGames)

            PlayStationSyncResult.Success(
                linkedAccount = linkedAccount,
                games = games,
                trophyGames = trophyGames
            )
        }.getOrElse { throwable ->
            PlayStationSyncResult.Failure(errorMessageFor(throwable))
        }
    }

    suspend fun getGamesForHome(context: Context): Result<List<Juego>> = withContext(Dispatchers.IO) {
        Result.success(getCachedGames(context).sortedForHome())
    }

    suspend fun getGamesWithAchievementSummaries(context: Context): Result<List<Juego>> = withContext(Dispatchers.IO) {
        if (getLinkedAccount(context) == null) return@withContext Result.success(emptyList())
        Result.success(getCachedTrophyGames(context).sortedForTrophies())
    }

    suspend fun getGameDetails(context: Context, gameId: String): Result<Juego> = withContext(Dispatchers.IO) {
        val cachedGame = getCachedGame(context, gameId)
            ?: return@withContext Result.failure(IllegalStateException("No se encontro el juego de PlayStation seleccionado."))

        Result.success(enrichWithIgdbDetails(cachedGame))
    }

    suspend fun getAchievementBundle(context: Context, gameId: String): Result<PlayStationAchievementBundle> = withContext(Dispatchers.IO) {
        // PARA TROFEOS DE PSN USO EL NOMBRE DE SERVICIO Y EL NPCOMMUNICATIONID DEL JUEGO.
        var linkedAccount = getLinkedAccount(context)
            ?: return@withContext Result.failure(IllegalStateException("Vincula PlayStation para ver tus trofeos."))
        val currentUid = currentUserUid()
            ?: return@withContext Result.failure(IllegalStateException("Inicia sesion para ver tus trofeos."))
        val game = getCachedTrophyGames(context).firstOrNull { it.platformGameId == gameId }
            ?: return@withContext Result.failure(IllegalStateException("No se encontro el titulo de PlayStation seleccionado."))

        val cached = loadAchievementCache(context)[gameId]
        if (cached != null && cached.hasAchievements) {
            return@withContext Result.success(cached)
        }

        runCatching {
            val response = api().getAchievements(
                npCommunicationId = gameId,
                request = PlayStationAchievementsRequest(
                    refreshToken = linkedAccount.refreshToken,
                    npServiceName = game.supportedPlatforms.toNpServiceName()
                )
            )
            linkedAccount = linkedAccount.withAuth(response.auth)
            saveLinkedAccount(context, currentUid, linkedAccount)

            val bundle = buildAchievementBundle(game, response)
            if (bundle.hasAchievements) {
                saveAchievementCache(
                    context,
                    loadAchievementCache(context).toMutableMap().apply { put(gameId, bundle) }
                )
                replaceCachedTrophyGame(context, game.copy(achievementSummary = bundle.summary))
            }
            bundle
        }.recoverCatching { throwable ->
            throw IllegalStateException(errorMessageFor(throwable))
        }
    }

    fun unlinkPlayStation(context: Context) {
        val currentUid = currentUserUid() ?: return
        clearScopedStorage(context, currentUid)
    }

    internal fun accountDisplayName(account: PlayStationLinkedAccount?): String {
        return account?.onlineId?.takeIf { it.isNotBlank() }
            ?: account?.displayName?.takeIf { it.isNotBlank() }
            ?: if (account != null) "Cuenta PlayStation vinculada" else "No vinculada"
    }

    internal fun accountActionLabel(account: PlayStationLinkedAccount?): String {
        return if (account == null) "Vincular" else "Desvincular PlayStation"
    }

    internal fun uniqueProfileGameCount(
        recentGames: List<Juego>,
        trophyGames: List<Juego>
    ): Int {
        return (recentGames + trophyGames)
            .map { it.achievementGameId ?: it.platformGameId }
            .filter { it.isNotBlank() }
            .distinct()
            .size
    }

    internal fun profileStatusText(
        account: PlayStationLinkedAccount?,
        recentGames: List<Juego>,
        trophyGames: List<Juego>
    ): String {
        if (account == null) return "No vinculada"
        val count = uniqueProfileGameCount(recentGames, trophyGames)
        return "Vinculada con $count juegos"
    }

    internal fun PlayStationPlayedTitle.toJuego(): Juego? {
        val normalizedTitleId = titleId.trim()
        val normalizedTitle = localizedName?.takeIf { it.isNotBlank() } ?: name.trim()
        if (normalizedTitleId.isBlank() || normalizedTitle.isBlank()) return null

        val primaryImage = imageUrl?.takeIf { it.isNotBlank() }
            ?: heroImageUrl?.takeIf { it.isNotBlank() }
            ?: ""

        return Juego(
            platform = PlataformaJuego.PSN,
            platformGameId = normalizedTitleId,
            title = normalizedTitle,
            headerImageUrl = primaryImage,
            capsuleImageUrl = primaryImage,
            heroImageUrl = heroImageUrl?.takeIf { it.isNotBlank() } ?: primaryImage,
            iconImageUrl = imageUrl,
            playtimeMinutes = playtimeMinutes.takeIf { it > 0 } ?: parseIsoDurationMinutes(playDuration),
            lastPlayedEpochSeconds = lastPlayedEpochSeconds,
            supportedPlatforms = supportedPlatforms.ifEmpty { listOf("PlayStation") }
        )
    }

    internal fun PlayStationTrophyTitle.toJuego(): Juego? {
        val normalizedId = npCommunicationId.trim()
        val normalizedTitle = title.trim()
        if (normalizedId.isBlank() || normalizedTitle.isBlank()) return null

        val image = iconUrl.orEmpty()
        return Juego(
            platform = PlataformaJuego.PSN,
            platformGameId = normalizedId,
            title = normalizedTitle,
            headerImageUrl = image,
            capsuleImageUrl = image,
            heroImageUrl = image,
            iconImageUrl = iconUrl,
            lastPlayedEpochSeconds = lastUpdatedEpochSeconds,
            achievementGameId = normalizedId,
            achievementSummary = summary?.toResumenTrofeos(),
            supportedPlatforms = platform
                .split(",", "/", "|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("PlayStation") }
        )
    }

    internal fun buildAchievementBundle(
        game: Juego,
        response: PlayStationAchievementResponse
    ): PlayStationAchievementBundle {
        // AQUI NORMALIZO TROFEOS DE PLAYSTATION AL MODELO COMUN QUE USA LA UI.
        val achievements = response.trophies
        val total = achievements.size
        val unlocked = achievements.count { it.unlocked }
        val summary = response.summary?.toResumenTrofeos() ?: if (total > 0) {
            ResumenTrofeos(
                total = total,
                desbloqueados = unlocked,
                porcentajeCompletado = unlocked.toFloat() / total.toFloat()
            )
        } else {
            null
        }

        return PlayStationAchievementBundle(
            gameId = response.npCommunicationId.ifBlank { game.platformGameId },
            gameTitle = game.title,
            summary = summary,
            achievements = achievements,
            hasAchievements = achievements.isNotEmpty()
        )
    }

    internal fun PlayStationAchievement.toPlatformAchievement(): PlatformAchievement {
        return PlatformAchievement(
            apiName = apiName.ifBlank { trophyId.toString() },
            title = title.ifBlank { "Trofeo $trophyId" },
            description = description,
            unlocked = unlocked,
            unlockTime = unlockTime?.takeIf { it > 0 },
            globalPercentage = globalPercentage,
            iconUrl = iconUrl,
            lockedIconUrl = lockedIconUrl ?: iconUrl
        )
    }

    internal fun parseIsoDurationMinutes(duration: String?): Int {
        if (duration.isNullOrBlank()) return 0
        val match = Regex("""^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$""")
            .matchEntire(duration)
            ?: return 0
        val days = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
        val hours = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val minutes = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        val seconds = match.groupValues.getOrNull(4)?.toFloatOrNull() ?: 0f
        return (days * 24 * 60 + hours * 60 + minutes + seconds / 60f).toInt()
    }

    internal fun scopedPreferenceKey(baseKey: String, uid: String?): String? {
        val normalizedUid = uid?.takeIf { it.isNotBlank() } ?: return null
        return "$baseKey::$normalizedUid"
    }

    internal fun scopedPreferenceKeys(uid: String?): List<String> {
        return storageKeys.mapNotNull { scopedPreferenceKey(it, uid) }
    }

    private fun api(): PlayStationApiService {
        // RETROFIT CREA EL SERVICIO APUNTANDO AL PROXY, NO DIRECTAMENTE A PLAYSTATION.
        return RetrofitInstance.playStationApi(BuildConfig.PLAYSTATION_PROXY_BASE_URL)
    }

    private fun PlayStationAuthResponse.toLinkedAccount(): PlayStationLinkedAccount {
        return PlayStationLinkedAccount(
            accountId = linkedAccount.accountId.ifBlank { "me" },
            onlineId = linkedAccount.onlineId,
            displayName = linkedAccount.displayName?.takeIf { it.isNotBlank() }
                ?: linkedAccount.onlineId
                ?: "Cuenta PlayStation vinculada",
            avatarUrl = linkedAccount.avatarUrl,
            isPlus = linkedAccount.isPlus,
            refreshToken = auth.refreshToken,
            refreshTokenExpiresAtEpochSeconds = auth.refreshTokenExpiresAtEpochSeconds()
        )
    }

    private fun PlayStationLinkedAccount.withAuth(auth: PlayStationSessionPayload): PlayStationLinkedAccount {
        return if (auth.refreshToken.isBlank()) {
            this
        } else {
            copy(
                refreshToken = auth.refreshToken,
                refreshTokenExpiresAtEpochSeconds = auth.refreshTokenExpiresAtEpochSeconds()
                    ?: refreshTokenExpiresAtEpochSeconds
            )
        }
    }

    private fun PlayStationSessionPayload.refreshTokenExpiresAtEpochSeconds(): Long? {
        val expiresIn = refreshTokenExpiresIn?.takeIf { it > 0 } ?: return null
        return System.currentTimeMillis() / 1000L + expiresIn
    }

    private fun PlayStationSummaryPayload.toResumenTrofeos(): ResumenTrofeos {
        val boundedTotal = total.coerceAtLeast(0)
        val boundedUnlocked = unlocked.coerceIn(0, boundedTotal.takeIf { it > 0 } ?: unlocked.coerceAtLeast(0))
        return ResumenTrofeos(
            total = boundedTotal,
            desbloqueados = boundedUnlocked,
            porcentajeCompletado = progress.coerceIn(0f, 1f)
        )
    }

    private suspend fun enrichWithIgdbDetails(game: Juego): Juego {
        // PLAYSTATION NO SIEMPRE DA BUENAS PORTADAS/DESCRIPCION; IGDB AYUDA A COMPLETAR EL DETALLE.
        if (!IGDBRepository.hasEndpoint()) return game

        val igdbId = IGDBRepository.searchGames(game.title)
            .getOrNull()
            ?.firstOrNull()
            ?.id
            ?: return game

        val igdbGame = IGDBRepository.getGameDetails(igdbId.toString()).getOrNull() ?: return game
        return game.copy(
            headerImageUrl = game.headerImageUrl.ifBlank { igdbGame.headerImageUrl },
            capsuleImageUrl = game.capsuleImageUrl.ifBlank { igdbGame.capsuleImageUrl },
            heroImageUrl = game.heroImageUrl.ifBlank { igdbGame.heroImageUrl },
            shortDescription = igdbGame.shortDescription ?: game.shortDescription,
            developers = igdbGame.developers.ifEmpty { game.developers },
            genres = igdbGame.genres.ifEmpty { game.genres },
            releaseDate = igdbGame.releaseDate ?: game.releaseDate,
            screenshots = igdbGame.screenshots.ifEmpty { game.screenshots },
            supportedPlatforms = game.supportedPlatforms.ifEmpty { igdbGame.supportedPlatforms },
            ageRating = igdbGame.ageRating ?: game.ageRating,
            steamAppId = igdbGame.steamAppId ?: game.steamAppId
        )
    }

    private fun List<Juego>.sortedForHome(): List<Juego> {
        return sortedWith(
            compareByDescending<Juego> { it.lastPlayedEpochSeconds ?: 0L }
                .thenByDescending { it.playtimeMinutes }
                .thenBy { it.title }
        )
    }

    private fun List<Juego>.sortedForTrophies(): List<Juego> {
        return sortedWith(
            compareByDescending<Juego> { it.lastPlayedEpochSeconds ?: 0L }
                .thenByDescending { it.achievementSummary?.porcentajeCompletado ?: 0f }
                .thenBy { it.title }
        )
    }

    private fun List<String>.toNpServiceName(): String {
        return if (any { it.contains("PS5", ignoreCase = true) || it.contains("PlayStation 5", ignoreCase = true) }) {
            "trophy2"
        } else {
            "trophy"
        }
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
            storageKeys.forEach(::remove)
            putBoolean(KEY_LEGACY_STORAGE_CLEARED, true)
        }.apply()
    }

    private fun readScopedJson(context: Context, baseKey: String): String? {
        // IGUAL QUE STEAM, LA CACHE VA SEPARADA POR USUARIO DE FIREBASE.
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

    private fun saveLinkedAccount(context: Context, uid: String, account: PlayStationLinkedAccount) {
        writeScopedJson(context, uid, KEY_LINKED_ACCOUNT, gson.toJson(account))
    }

    private fun saveGames(context: Context, uid: String, games: List<Juego>) {
        writeScopedJson(context, uid, KEY_CACHED_GAMES, gson.toJson(games))
    }

    private fun saveTrophyGames(context: Context, uid: String, games: List<Juego>) {
        writeScopedJson(context, uid, KEY_CACHED_TROPHY_GAMES, gson.toJson(games))
    }

    private fun replaceCachedTrophyGame(context: Context, juego: Juego) {
        val currentUid = currentUserUid() ?: return
        val updated = getCachedTrophyGames(context).map {
            if (it.platformGameId == juego.platformGameId) juego else it
        }
        saveTrophyGames(context, currentUid, updated)
    }

    private fun saveAchievementCache(context: Context, cache: Map<String, PlayStationAchievementBundle>) {
        val currentUid = currentUserUid() ?: return
        writeScopedJson(context, currentUid, KEY_CACHED_ACHIEVEMENTS, gson.toJson(cache))
    }

    private fun loadAchievementCache(context: Context): Map<String, PlayStationAchievementBundle> {
        val json = readScopedJson(context, KEY_CACHED_ACHIEVEMENTS) ?: return emptyMap()
        val type = object : TypeToken<Map<String, PlayStationAchievementBundle>>() {}.type
        return runCatching {
            gson.fromJson<Map<String, PlayStationAchievementBundle>>(json, type)
        }.getOrDefault(emptyMap())
    }

    private fun errorMessageFor(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> when (throwable.code()) {
                400 -> "El NPSSO o titulo de PlayStation no es valido."
                401, 403 -> "PlayStation ha rechazado la sesion. Vuelve a vincular con un NPSSO nuevo."
                404 -> "PlayStation no encontro datos visibles para esta cuenta o juego."
                else -> "PlayStation ha respondido con un error (${throwable.code()}). Intentalo de nuevo."
            }
            else -> throwable.message ?: "No se pudo conectar con PlayStation."
        }
    }
}
