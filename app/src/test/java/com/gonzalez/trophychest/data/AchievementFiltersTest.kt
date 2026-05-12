package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementFiltersTest {
    @Test
    fun uniqueByPlatformGameId_keepsGamesWithSameTitleOnDifferentPlatforms() {
        val steamGame = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "100",
            title = "Shared Game"
        )
        val psnGame = testGame(
            platform = PlataformaJuego.PSN,
            gameId = "100",
            title = "Shared Game"
        )
        val gamePassGame = testGame(
            platform = PlataformaJuego.GAME_PASS,
            gameId = "100",
            title = "Shared Game"
        )
        val duplicateSteamGame = steamGame.copy(headerImageUrl = "new-header")

        val uniqueGames = AchievementFilters.uniqueByPlatformGameId(
            listOf(steamGame, psnGame, gamePassGame, duplicateSteamGame)
        )

        assertEquals(listOf(steamGame, psnGame, gamePassGame), uniqueGames)
    }

    @Test
    fun filterGames_appliesCompletionAndPlatformSelection() {
        val steamHigh = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "10",
            completion = 0.75f
        )
        val steamLow = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "20",
            completion = 0.25f
        )
        val psnHigh = testGame(
            platform = PlataformaJuego.PSN,
            gameId = "30",
            completion = 0.9f
        )

        val filteredGames = AchievementFilters.filterGames(
            games = listOf(steamHigh, steamLow, psnHigh),
            minCompletion = 0.5f,
            selectedPlatforms = setOf(PlataformaJuego.STEAM)
        )

        assertEquals(listOf(steamHigh), filteredGames)
    }

    @Test
    fun trophyPlatforms_includePreparedProviders() {
        assertEquals(
            listOf(
                PlataformaJuego.STEAM,
                PlataformaJuego.PSN,
                PlataformaJuego.GAME_PASS
            ),
            AchievementFilters.trophyPlatforms
        )
        assertTrue(PlataformaJuego.IGDB !in AchievementFilters.trophyPlatforms)
    }

    private fun testGame(
        platform: PlataformaJuego,
        gameId: String,
        title: String = "${platform.displayName} $gameId",
        completion: Float = 0f
    ): Juego {
        return Juego(
            platform = platform,
            platformGameId = gameId,
            title = title,
            headerImageUrl = "header-$gameId",
            capsuleImageUrl = "capsule-$gameId",
            heroImageUrl = "hero-$gameId",
            iconImageUrl = null,
            achievementSummary = ResumenTrofeos(
                total = 10,
                desbloqueados = (completion * 10).toInt(),
                porcentajeCompletado = completion
            )
        )
    }
}
