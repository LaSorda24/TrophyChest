package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileStatsCalculatorTest {
    @Test
    fun calculate_sumsGamesAcrossPlatforms() {
        val stats = ProfileStatsCalculator.calculate(
            steamGames = listOf(testGame(PlataformaJuego.STEAM, "10")),
            playStationRecentGames = listOf(testGame(PlataformaJuego.PSN, "PPSA00001_00")),
            playStationTrophyGames = emptyList(),
            connections = emptyList()
        )

        assertEquals(2, stats.totalGames)
    }

    @Test
    fun calculate_doesNotDuplicateGamesWithinSamePlatform() {
        val steamGame = testGame(PlataformaJuego.STEAM, "10")
        val duplicateSteamGame = testGame(PlataformaJuego.STEAM, "10", title = "Duplicate")
        val trophyDuplicate = testGame(
            platform = PlataformaJuego.PSN,
            gameId = "NPWR20188_00",
            achievementGameId = "NPWR20188_00"
        )

        val stats = ProfileStatsCalculator.calculate(
            steamGames = listOf(steamGame, duplicateSteamGame),
            playStationRecentGames = emptyList(),
            playStationTrophyGames = listOf(trophyDuplicate, trophyDuplicate.copy(title = "Duplicate")),
            connections = emptyList()
        )

        assertEquals(2, stats.totalGames)
    }

    @Test
    fun calculate_sumsUnlockedTrophiesAcrossPlatformsWithoutDuplicates() {
        val steamGame = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "10",
            unlockedTrophies = 4
        )
        val duplicatedSteamGame = steamGame.copy(title = "Duplicate")
        val playStationTrophyGame = testGame(
            platform = PlataformaJuego.PSN,
            gameId = "NPWR20188_00",
            achievementGameId = "NPWR20188_00",
            unlockedTrophies = 7
        )
        val stats = ProfileStatsCalculator.calculate(
            steamGames = listOf(steamGame, duplicatedSteamGame),
            playStationRecentGames = emptyList(),
            playStationTrophyGames = listOf(playStationTrophyGame),
            connections = emptyList()
        )

        assertEquals(11, stats.totalTrophies)
    }

    @Test
    fun calculate_countsOnlyAcceptedFriends() {
        val stats = ProfileStatsCalculator.calculate(
            steamGames = emptyList(),
            playStationRecentGames = emptyList(),
            playStationTrophyGames = emptyList(),
            connections = listOf(
                FriendConnection(status = CONNECTION_STATUS_ACCEPTED),
                FriendConnection(status = CONNECTION_STATUS_PENDING),
                FriendConnection(status = CONNECTION_STATUS_REJECTED),
                FriendConnection(status = CONNECTION_STATUS_ACCEPTED)
            )
        )

        assertEquals(2, stats.friendCount)
    }

    private fun testGame(
        platform: PlataformaJuego,
        gameId: String,
        title: String = "${platform.displayName} $gameId",
        achievementGameId: String? = null,
        unlockedTrophies: Int = 0
    ): Juego {
        return Juego(
            platform = platform,
            platformGameId = gameId,
            title = title,
            headerImageUrl = "",
            capsuleImageUrl = "",
            heroImageUrl = "",
            iconImageUrl = null,
            achievementGameId = achievementGameId,
            achievementSummary = ResumenTrofeos(
                total = unlockedTrophies,
                desbloqueados = unlockedTrophies,
                porcentajeCompletado = if (unlockedTrophies > 0) 1f else 0f
            )
        )
    }
}
