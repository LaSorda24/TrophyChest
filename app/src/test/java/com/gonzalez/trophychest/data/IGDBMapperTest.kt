package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IGDBMapperTest {
    @Test
    fun mapSearchGame_normalizesNameCoverAndPlatforms() {
        val game = IGDBMapper.mapSearchGame(
            IGDBGameDto(
                id = 77L,
                name = "  Hollow Knight  ",
                coverUrl = " https://images.igdb.com/cover.jpg ",
                platforms = listOf("PC (Microsoft Windows)", "Nintendo Switch", "PC (Microsoft Windows)")
            )
        )

        requireNotNull(game)
        assertEquals(77L, game.id)
        assertEquals("Hollow Knight", game.name)
        assertEquals("https://images.igdb.com/cover.jpg", game.coverUrl)
        assertEquals(listOf("PC (Microsoft Windows)", "Nintendo Switch"), game.platforms)
    }

    @Test
    fun mapSearchGame_returnsNullWhenEssentialDataIsMissing() {
        assertNull(
            IGDBMapper.mapSearchGame(
                IGDBGameDto(
                    id = 0L,
                    name = "Broken",
                    coverUrl = null,
                    platforms = emptyList()
                )
            )
        )

        assertNull(
            IGDBMapper.mapSearchGame(
                IGDBGameDto(
                    id = 8L,
                    name = "   ",
                    coverUrl = null,
                    platforms = emptyList()
                )
            )
        )
    }

    @Test
    fun mapDetailToJuego_keepsIgdbIdentityAndOptionalFields() {
        val juego = IGDBMapper.mapDetailToJuego(
            IGDBGameDetailDto(
                id = 101L,
                name = "Clair Obscur",
                coverUrl = "https://images.igdb.com/cover.jpg",
                heroImageUrl = "https://images.igdb.com/hero.jpg",
                summary = "  Un RPG por turnos. ",
                platforms = listOf("PlayStation 5", "Xbox Series X|S"),
                developers = listOf("Sandfall Interactive", "Sandfall Interactive"),
                genres = listOf("RPG", "Adventure"),
                releaseDate = "2026-04-12",
                screenshots = listOf("https://images.igdb.com/s1.jpg", "https://images.igdb.com/s1.jpg"),
                ageRating = IGDBAgeRatingDto(
                    label = "PEGI 16",
                    minimumAge = 16,
                    imageUrl = "https://images.igdb.com/pegi16.png"
                ),
                steamAppId = " 1903340 "
            )
        )

        requireNotNull(juego)
        assertEquals(PlataformaJuego.IGDB, juego.platform)
        assertEquals("101", juego.platformGameId)
        assertEquals("Clair Obscur", juego.title)
        assertEquals("https://images.igdb.com/cover.jpg", juego.headerImageUrl)
        assertEquals("https://images.igdb.com/hero.jpg", juego.heroImageUrl)
        assertEquals("Un RPG por turnos.", juego.shortDescription)
        assertEquals(listOf("Sandfall Interactive"), juego.developers)
        assertEquals(listOf("RPG", "Adventure"), juego.genres)
        assertEquals(listOf("PlayStation 5", "Xbox Series X|S"), juego.supportedPlatforms)
        assertEquals(listOf("https://images.igdb.com/s1.jpg"), juego.screenshots)
        assertEquals("PEGI 16", juego.ageRating?.label)
        assertEquals(16, juego.ageRating?.minimumAge)
        assertEquals("https://images.igdb.com/pegi16.png", juego.ageRating?.imageUrl)
        assertEquals("1903340", juego.steamAppId)
    }

    @Test
    fun mapExploreContent_mapsSectionsToIgdbGames() {
        val content = IGDBMapper.mapExploreContent(
            IGDBExploreResponse(
                topSellers = listOf(
                    IGDBExploreGameDto(
                        id = 501L,
                        name = "Forza Horizon",
                        coverUrl = "https://images.igdb.com/cover.jpg",
                        heroImageUrl = "https://images.igdb.com/hero.jpg",
                        summary = "Carreras de mundo abierto.",
                        platforms = listOf("PC (Microsoft Windows)", "Xbox Series X|S"),
                        genres = listOf("Racing"),
                        releaseDate = "2025-10-12",
                        rating = 88.4,
                        ratingCount = 200
                    )
                ),
                recommended = listOf(
                    IGDBExploreGameDto(
                        id = 502L,
                        name = "Spider-Man",
                        platforms = listOf("PlayStation 5"),
                        hypes = 16
                    )
                ),
                recommendationMessage = "Seleccion de IGDB para PC, PlayStation y Xbox."
            )
        )

        assertEquals(1, content.topSellers.size)
        assertEquals(PlataformaJuego.IGDB, content.topSellers.first().game.platform)
        assertEquals("501", content.topSellers.first().game.platformGameId)
        assertEquals(listOf("PC (Microsoft Windows)", "Xbox Series X|S"), content.topSellers.first().game.supportedPlatforms)
        assertEquals("88/100 IGDB", content.topSellers.first().badgeText)
        assertEquals("16 en seguimiento", content.recommended.first().badgeText)
        assertEquals("Seleccion de IGDB para PC, PlayStation y Xbox.", content.recommendationMessage)
    }

    @Test
    fun mapCategoryContent_mapsGamesToIgdbCategory() {
        val category = IGDBCategoryDefinition(
            slug = "accion",
            displayName = "ACCION",
            igdbGenreName = "Action"
        )
        val content = IGDBMapper.mapCategoryContent(
            response = IGDBCategoryResponse(
                genre = "Action",
                games = listOf(
                    IGDBExploreGameDto(
                        id = 700L,
                        name = "Action Hit",
                        coverUrl = "https://images.igdb.com/action.jpg",
                        platforms = listOf("PlayStation 5", "Xbox Series X|S"),
                        genres = listOf("Action")
                    )
                )
            ),
            category = category
        )

        assertEquals(category, content.category)
        assertEquals(1, content.games.size)
        assertEquals(PlataformaJuego.IGDB, content.games.first().platform)
        assertEquals("700", content.games.first().platformGameId)
        assertEquals(listOf("PlayStation 5", "Xbox Series X|S"), content.games.first().supportedPlatforms)
    }
}
