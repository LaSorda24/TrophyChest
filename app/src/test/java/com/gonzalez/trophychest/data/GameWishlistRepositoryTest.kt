package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameWishlistRepositoryTest {
    @Test
    fun toggledSavedGames_doesNotAddOwnedSteamGame() {
        val ownedSteamGame = testGame(PlataformaJuego.STEAM, "10")

        val updatedGames = GameWishlistRepository.toggledSavedGames(
            savedGames = emptyList(),
            game = ownedSteamGame,
            ownedSteamGameIds = setOf("10")
        )

        assertTrue(updatedGames.isEmpty())
    }

    @Test
    fun toggledSavedGames_addsAndRemovesNonOwnedSteamGame() {
        val steamGame = testGame(PlataformaJuego.STEAM, "20")

        val savedGames = GameWishlistRepository.toggledSavedGames(
            savedGames = emptyList(),
            game = steamGame,
            ownedSteamGameIds = setOf("10")
        )
        val removedGames = GameWishlistRepository.toggledSavedGames(
            savedGames = savedGames,
            game = steamGame,
            ownedSteamGameIds = setOf("10")
        )

        assertEquals(listOf(steamGame), savedGames)
        assertTrue(removedGames.isEmpty())
    }

    @Test
    fun visibleSavedGames_filtersOwnedSteamGames() {
        val ownedSteamGame = testGame(PlataformaJuego.STEAM, "10")
        val nonOwnedSteamGame = testGame(PlataformaJuego.STEAM, "20")
        val igdbGame = testGame(PlataformaJuego.IGDB, "10")

        val visibleGames = GameWishlistRepository.visibleSavedGames(
            savedGames = listOf(ownedSteamGame, nonOwnedSteamGame, igdbGame),
            ownedSteamGameIds = setOf("10")
        )

        assertFalse(ownedSteamGame in visibleGames)
        assertEquals(listOf(nonOwnedSteamGame, igdbGame), visibleGames)
    }

    @Test
    fun toggledSavedGames_allowsIgdbGamesEvenWhenIdMatchesOwnedSteamGame() {
        val igdbGame = testGame(PlataformaJuego.IGDB, "10")

        val savedGames = GameWishlistRepository.toggledSavedGames(
            savedGames = emptyList(),
            game = igdbGame,
            ownedSteamGameIds = setOf("10")
        )

        assertEquals(listOf(igdbGame), savedGames)
    }

    private fun testGame(platform: PlataformaJuego, gameId: String): Juego {
        return Juego(
            platform = platform,
            platformGameId = gameId,
            title = "${platform.displayName} $gameId",
            headerImageUrl = "header-$gameId",
            capsuleImageUrl = "capsule-$gameId",
            heroImageUrl = "hero-$gameId",
            iconImageUrl = null
        )
    }
}
