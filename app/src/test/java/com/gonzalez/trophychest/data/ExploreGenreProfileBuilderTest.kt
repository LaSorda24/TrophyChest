package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExploreGenreProfileBuilderTest {
    @Test
    fun buildFromCachedGames_deduplicatesSteamAndPlayStationGenres() {
        val steamGames = listOf(
            testGame(
                platform = PlataformaJuego.STEAM,
                id = "10",
                title = "Steam Adventure",
                playtimeMinutes = 900,
                genres = listOf("Adventure", "Action")
            )
        )
        val playStationGames = listOf(
            testGame(
                platform = PlataformaJuego.PSN,
                id = "ps-1",
                title = "PS Adventure",
                playtimeMinutes = 200,
                genres = listOf(" adventure ", "Action", "RPG")
            )
        )

        val genres = ExploreGenreProfileBuilder.buildFromCachedGames(steamGames, playStationGames)

        assertEquals(listOf("Adventure", "Action", "Role-playing (RPG)"), genres)
    }

    @Test
    fun buildFromCachedGames_prioritizesPlayedAndRecentGames() {
        val steamGames = listOf(
            testGame(
                platform = PlataformaJuego.STEAM,
                id = "low",
                title = "Low Priority",
                playtimeMinutes = 5,
                lastPlayedEpochSeconds = 100L,
                genres = listOf("Puzzle")
            ),
            testGame(
                platform = PlataformaJuego.STEAM,
                id = "high",
                title = "High Priority",
                playtimeMinutes = 100,
                lastPlayedEpochSeconds = 50L,
                genres = listOf("Shooter")
            )
        )
        val playStationGames = listOf(
            testGame(
                platform = PlataformaJuego.PSN,
                id = "recent",
                title = "Recent Priority",
                playtimeMinutes = 100,
                lastPlayedEpochSeconds = 200L,
                genres = listOf("Adventure")
            )
        )

        val genres = ExploreGenreProfileBuilder.buildFromCachedGames(steamGames, playStationGames)

        assertEquals(listOf("Adventure", "Shooter", "Puzzle"), genres)
    }

    @Test
    fun buildFromCachedGames_returnsEmptyWhenNoGenresExist() {
        val genres = ExploreGenreProfileBuilder.buildFromCachedGames(
            steamGames = listOf(testGame(PlataformaJuego.STEAM, "1", "No Genre")),
            playStationGames = emptyList()
        )

        assertEquals(emptyList<String>(), genres)
    }

    @Test
    fun toGenreQueryParameter_serializesUniqueGenresForRetrofit() {
        val query = with(IGDBRepository) {
            listOf("Action", "Adventure", "action", "RPG").toGenreQueryParameter()
        }

        assertEquals("Action,Adventure,Role-playing (RPG)", query)
    }

    @Test
    fun toGenreQueryParameter_returnsNullForEmptyFilters() {
        val query = with(IGDBRepository) {
            emptyList<String>().toGenreQueryParameter()
        }

        assertNull(query)
    }

    private fun testGame(
        platform: PlataformaJuego,
        id: String,
        title: String,
        playtimeMinutes: Int = 0,
        lastPlayedEpochSeconds: Long? = null,
        genres: List<String> = emptyList()
    ): Juego {
        return Juego(
            platform = platform,
            platformGameId = id,
            title = title,
            headerImageUrl = "",
            capsuleImageUrl = "",
            heroImageUrl = "",
            iconImageUrl = null,
            playtimeMinutes = playtimeMinutes,
            lastPlayedEpochSeconds = lastPlayedEpochSeconds,
            genres = genres
        )
    }
}
