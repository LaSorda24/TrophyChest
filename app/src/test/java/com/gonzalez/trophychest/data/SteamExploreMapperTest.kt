package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamExploreMapperTest {
    @Test
    fun featuredItemToJuego_mapsSteamImagesAndAppId() {
        val game = SteamExploreMapper.featuredItemToJuego(
            SteamFeaturedItem(
                id = 1245620,
                type = 0,
                name = "Elden Ring",
                headerImage = "https://cdn.akamai.steamstatic.com/steam/apps/1245620/header.jpg",
                largeCapsuleImage = "https://cdn.akamai.steamstatic.com/steam/apps/1245620/capsule_616x353.jpg",
                smallCapsuleImage = "https://cdn.akamai.steamstatic.com/steam/apps/1245620/capsule_231x87.jpg"
            )
        )

        requireNotNull(game)
        assertEquals(PlataformaJuego.STEAM, game.platform)
        assertEquals("1245620", game.platformGameId)
        assertEquals("Elden Ring", game.title)
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/1245620/library_600x900.jpg", game.capsuleImageUrl)
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/1245620/header.jpg", game.headerImageUrl)
    }

    @Test
    fun recommendFromLibrary_prioritizesGenresAndExcludesOwnedGames() {
        val owned = listOf(
            juego("1", "Owned RPG", playtime = 900, genres = listOf("RPG", "Adventure"))
        )
        val candidates = listOf(
            juego("1", "Owned RPG", genres = listOf("RPG")),
            juego("2", "Strong Match", genres = listOf("RPG")),
            juego("3", "No Match", genres = listOf("Sports"))
        )

        val recommended = SteamExploreMapper.recommendFromLibrary(
            ownedGames = owned,
            candidates = candidates,
            detailsByGameId = emptyMap(),
            limit = 5
        )

        assertEquals(listOf("2"), recommended.map { it.platformGameId })
    }

    @Test
    fun sortByQuality_discardsInvalidReviewsAndSortsBestGames() {
        val candidates = listOf(
            juego("10", "Excellent"),
            juego("20", "Mixed"),
            juego("30", "Too Few Reviews"),
            juego("40", "Very Good")
        )
        val reviews = mapOf(
            "10" to SteamReviewQuerySummary(reviewScore = 9, totalPositive = 950, totalNegative = 50, totalReviews = 1000),
            "20" to SteamReviewQuerySummary(reviewScore = 6, totalPositive = 650, totalNegative = 350, totalReviews = 1000),
            "30" to SteamReviewQuerySummary(reviewScore = 9, totalPositive = 44, totalNegative = 5, totalReviews = 49),
            "40" to SteamReviewQuerySummary(reviewScore = 8, totalPositive = 850, totalNegative = 150, totalReviews = 1000)
        )

        val sorted = SteamExploreMapper.sortByQuality(candidates, reviews, limit = 3)

        assertEquals(listOf("10", "40"), sorted.map { it.platformGameId })
        assertTrue(sorted.none { it.platformGameId == "20" })
        assertTrue(sorted.none { it.platformGameId == "30" })
    }

    private fun juego(
        id: String,
        title: String,
        playtime: Int = 0,
        genres: List<String> = emptyList()
    ): Juego {
        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = id,
            title = title,
            headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$id/header.jpg",
            capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$id/library_600x900.jpg",
            heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$id/capsule_616x353.jpg",
            iconImageUrl = null,
            playtimeMinutes = playtime,
            genres = genres
        )
    }
}
