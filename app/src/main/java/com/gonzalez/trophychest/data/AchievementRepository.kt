package com.gonzalez.trophychest.data

import android.content.Context

object AchievementRepository {
    suspend fun getGamesWithAchievementSummaries(context: Context): Result<List<Juego>> {
        val steamGames = if (SteamRepository.getLinkedAccount(context) != null) {
            SteamRepository.getGamesWithAchievementSummaries(context).getOrElse { throwable ->
                return Result.failure(throwable)
            }
        } else {
            emptyList()
        }
        val playStationGames = if (PlayStationRepository.getLinkedAccount(context) != null) {
            PlayStationRepository.getGamesWithAchievementSummaries(context).getOrElse { throwable ->
                return Result.failure(throwable)
            }
        } else {
            emptyList()
        }

        return Result.success(
            AchievementFilters.uniqueByPlatformGameId(steamGames + playStationGames)
                .sortedWith(
                    compareByDescending<Juego> { it.achievementSummary?.porcentajeCompletado ?: 0f }
                        .thenBy { it.platform.displayName }
                        .thenBy { it.title }
                )
        )
    }

    suspend fun getAchievementBundle(
        context: Context,
        platform: PlataformaJuego,
        gameId: String
    ): Result<PlatformAchievementBundle> {
        return when (platform) {
            PlataformaJuego.STEAM -> SteamRepository.getAchievementBundle(context, gameId)
                .map { it.toPlatformBundle(PlataformaJuego.STEAM) }
            PlataformaJuego.PSN -> PlayStationRepository.getAchievementBundle(context, gameId)
                .map { it.toPlatformBundle(PlataformaJuego.PSN) }
            PlataformaJuego.GAME_PASS -> Result.failure(
                IllegalStateException(GAME_PASS_UNAVAILABLE_MESSAGE)
            )
            PlataformaJuego.IGDB -> Result.failure(
                IllegalStateException("IGDB es una fuente de catalogo y no expone trofeos de usuario.")
            )
        }
    }

    suspend fun getAchievementBundleForGameDetail(
        context: Context,
        game: Juego
    ): Result<PlatformAchievementBundle> {
        val personalResult = when (game.platform) {
            PlataformaJuego.STEAM,
            PlataformaJuego.PSN -> getAchievementBundle(
                context = context,
                platform = game.platform,
                gameId = game.achievementGameId ?: game.platformGameId
            )
            PlataformaJuego.GAME_PASS -> Result.failure(
                IllegalStateException(GAME_PASS_UNAVAILABLE_MESSAGE)
            )
            PlataformaJuego.IGDB -> Result.failure(
                IllegalStateException("IGDB no expone trofeos directamente.")
            )
        }

        if (personalResult.isSuccess) {
            return personalResult
        }

        val steamAppId = game.steamAppId
            ?: if (game.platform == PlataformaJuego.STEAM) game.platformGameId else null

        if (steamAppId.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException("No hay AppID de Steam para consultar trofeos publicos de este juego.")
            )
        }

        return steamAppId.let { appId ->
            SteamRepository.getPublicAchievementBundle(
                context = context,
                steamAppId = appId,
                fallbackTitle = game.title
            ).map { it.toPlatformBundle(PlataformaJuego.STEAM) }
        }
    }

    private fun SteamAchievementBundle.toPlatformBundle(platform: PlataformaJuego): PlatformAchievementBundle {
        return PlatformAchievementBundle(
            platform = platform,
            gameId = gameId,
            gameTitle = gameTitle,
            summary = summary,
            achievements = achievements.map { it.toPlatformAchievement() },
            hasAchievements = hasAchievements,
            isPersonal = isPersonal
        )
    }

    private fun SteamAchievement.toPlatformAchievement(): PlatformAchievement {
        return PlatformAchievement(
            apiName = apiName,
            title = title,
            description = description,
            unlocked = unlocked,
            unlockTime = unlockTime,
            globalPercentage = globalPercentage,
            iconUrl = iconUrl,
            lockedIconUrl = lockedIconUrl
        )
    }

    private fun PlayStationAchievementBundle.toPlatformBundle(platform: PlataformaJuego): PlatformAchievementBundle {
        return PlatformAchievementBundle(
            platform = platform,
            gameId = gameId,
            gameTitle = gameTitle,
            summary = summary,
            achievements = achievements.map { it.toPlatformAchievement() },
            hasAchievements = hasAchievements
        )
    }

    private fun PlayStationAchievement.toPlatformAchievement(): PlatformAchievement {
        return PlayStationRepository.run { this@toPlatformAchievement.toPlatformAchievement() }
    }
}
