package com.gonzalez.trophychest.data

data class AchievementGameKey(
    val platform: PlataformaJuego,
    val platformGameId: String
) {
    val value: String = "${platform.name}:$platformGameId"
}

data class PlatformAchievement(
    val apiName: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockTime: Long?,
    val globalPercentage: Double?,
    val iconUrl: String?,
    val lockedIconUrl: String?
)

data class PlatformAchievementBundle(
    val platform: PlataformaJuego,
    val gameId: String,
    val gameTitle: String,
    val summary: ResumenTrofeos?,
    val achievements: List<PlatformAchievement>,
    val hasAchievements: Boolean,
    val isPersonal: Boolean = true
)

object AchievementFilters {
    val trophyPlatforms = listOf(
        PlataformaJuego.STEAM,
        PlataformaJuego.PSN,
        PlataformaJuego.GAME_PASS
    )

    fun uniqueByPlatformGameId(games: List<Juego>): List<Juego> {
        return games.distinctBy { AchievementGameKey(it.platform, it.platformGameId).value }
    }

    fun filterGames(
        games: List<Juego>,
        minCompletion: Float,
        selectedPlatforms: Set<PlataformaJuego>
    ): List<Juego> {
        return games.filter { game ->
            game.platform in selectedPlatforms &&
                (game.achievementSummary?.porcentajeCompletado ?: 0f) >= minCompletion
        }
    }
}
