package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {
    @Test
    fun sortRecentGamesForHome_ordersMixedProvidersByRecency() {
        val oldSteam = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "10",
            lastPlayed = 100,
            playtime = 999
        )
        val newerSteam = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "20",
            lastPlayed = 200,
            playtime = 0
        )
        val newerPlayStation = testGame(
            platform = PlataformaJuego.PSN,
            gameId = "PPSA00001_00",
            lastPlayed = 300,
            playtime = 10
        )
        val neverPlayedSteam = testGame(
            platform = PlataformaJuego.STEAM,
            gameId = "30",
            lastPlayed = null,
            playtime = 120
        )

        val sorted = LibraryRepository.sortRecentGamesForHome(
            listOf(oldSteam, neverPlayedSteam, newerSteam, newerPlayStation)
        )

        assertEquals(listOf(newerPlayStation, newerSteam, oldSteam, neverPlayedSteam), sorted)
    }

    @Test
    fun selectRecentGamesForHome_keepsSteamVisibleWhenPlayStationHasMostRecentGames() {
        val playStationGames = (1..10).map { index ->
            testGame(
                platform = PlataformaJuego.PSN,
                gameId = "ps-$index",
                lastPlayed = 1_000L - index,
                playtime = 20
            )
        }
        val steamGames = listOf(
            testGame(
                platform = PlataformaJuego.STEAM,
                gameId = "steam-1",
                lastPlayed = 100,
                playtime = 120
            )
        )

        val selected = LibraryRepository.selectRecentGamesForHome(playStationGames + steamGames)

        assertEquals(10, selected.size)
        assertTrue(selected.any { it.platform == PlataformaJuego.PSN })
        assertTrue(selected.any { it.platform == PlataformaJuego.STEAM })
    }

    @Test
    fun selectRecentGamesForHome_interleavesLinkedPlatformsByRound() {
        val steamGames = listOf(
            testGame(PlataformaJuego.STEAM, "steam-1", lastPlayed = 300, playtime = 1),
            testGame(PlataformaJuego.STEAM, "steam-2", lastPlayed = 30, playtime = 1)
        )
        val playStationGames = listOf(
            testGame(PlataformaJuego.PSN, "ps-1", lastPlayed = 200, playtime = 1),
            testGame(PlataformaJuego.PSN, "ps-2", lastPlayed = 20, playtime = 1)
        )
        val selected = LibraryRepository.selectRecentGamesForHome(
            steamGames + playStationGames,
            limit = 4
        )

        assertEquals(
            listOf(
                "steam-1",
                "ps-1",
                "steam-2",
                "ps-2"
            ),
            selected.map { it.platformGameId }
        )
    }

    @Test
    fun selectRecentGamesForHome_singlePlatformKeepsStrictRecencyOrder() {
        val games = listOf(
            testGame(PlataformaJuego.STEAM, "old", lastPlayed = 100, playtime = 999),
            testGame(PlataformaJuego.STEAM, "new", lastPlayed = 200, playtime = 1),
            testGame(PlataformaJuego.STEAM, "never", lastPlayed = null, playtime = 1)
        )

        val selected = LibraryRepository.selectRecentGamesForHome(games, limit = 3)

        assertEquals(listOf("new", "old", "never"), selected.map { it.platformGameId })
    }

    @Test
    fun selectRecentGamesForHome_fillsLimitWhenAPlatformRunsOut() {
        val steamGames = (1..4).map { index ->
            testGame(
                platform = PlataformaJuego.STEAM,
                gameId = "steam-$index",
                lastPlayed = 100L - index,
                playtime = 1
            )
        }
        val playStationGames = listOf(
            testGame(PlataformaJuego.PSN, "ps-1", lastPlayed = 200, playtime = 1)
        )

        val selected = LibraryRepository.selectRecentGamesForHome(
            steamGames + playStationGames,
            limit = 5
        )

        assertEquals(5, selected.size)
        assertEquals(listOf("ps-1", "steam-1", "steam-2", "steam-3", "steam-4"), selected.map { it.platformGameId })
    }

    private fun testGame(
        platform: PlataformaJuego,
        gameId: String,
        lastPlayed: Long?,
        playtime: Int
    ): Juego {
        return Juego(
            platform = platform,
            platformGameId = gameId,
            title = "${platform.displayName} $gameId",
            headerImageUrl = "",
            capsuleImageUrl = "",
            heroImageUrl = "",
            iconImageUrl = null,
            playtimeMinutes = playtime,
            lastPlayedEpochSeconds = lastPlayed
        )
    }
}
