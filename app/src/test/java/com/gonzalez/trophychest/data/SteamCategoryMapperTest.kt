package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamCategoryMapperTest {
    @Test
    fun categoryForSlug_mapsKnownCategoriesToSteamTags() {
        val action = SteamCategoryMapper.categoryForSlug("accion")
        val simulation = SteamCategoryMapper.categoryForSlug("simulacion")

        requireNotNull(action)
        requireNotNull(simulation)
        assertEquals("ACCION", action.displayName)
        assertEquals(19, action.tagId)
        assertEquals("SIMULACION", simulation.displayName)
        assertEquals(599, simulation.tagId)
    }

    @Test
    fun parseSearchResultsHtml_extractsAppIdsAndTitles() {
        val games = SteamCategoryMapper.parseSearchResultsHtml(
            """
            <a class="search_result_row" data-ds-appid="1245620" href="https://store.steampowered.com/app/1245620/ELDEN_RING/">
                <span class="title">ELDEN RING</span>
                <img src="https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/1245620/capsule_sm_120.jpg" />
            </a>
            <a class="search_result_row" href="https://store.steampowered.com/app/367520/Hollow_Knight/">
                <span class="title">Hollow Knight</span>
            </a>
            """.trimIndent()
        )

        assertEquals(listOf("1245620", "367520"), games.map { it.platformGameId })
        assertEquals(listOf("ELDEN RING", "Hollow Knight"), games.map { it.title })
    }

    @Test
    fun parseSearchResultsHtml_excludesRowsWithoutAppIdOrTitle() {
        val games = SteamCategoryMapper.parseSearchResultsHtml(
            """
            <a class="search_result_row" href="https://store.steampowered.com/sub/123/">
                <span class="title">A Bundle</span>
            </a>
            <a class="search_result_row" data-ds-appid="730"></a>
            """.trimIndent()
        )

        assertTrue(games.isEmpty())
    }

    @Test
    fun applyFilters_hidesMultiplayerUsingStoreCategories() {
        val singlePlayer = juego("1", "Single")
        val multiplayer = juego("2", "Online")
        val details = mapOf(
            "1" to SteamStoreAppDetails(categories = listOf(SteamStoreCategory(description = "Single-player"))),
            "2" to SteamStoreAppDetails(categories = listOf(SteamStoreCategory(description = "Online PvP")))
        )

        val filtered = SteamCategoryMapper.applyFilters(
            games = listOf(singlePlayer, multiplayer),
            detailsByAppId = details,
            filters = SteamCategoryFilters(hideMultiplayer = true)
        )

        assertEquals(listOf("1"), filtered.map { it.platformGameId })
    }

    @Test
    fun applyFilters_keepsGamesWhenSoftFilterDataIsMissing() {
        val games = SteamCategoryMapper.applyFilters(
            games = listOf(juego("1", "Unknown")),
            detailsByAppId = emptyMap(),
            filters = SteamCategoryFilters(
                trophyLevel = "Alto",
                playtimeBucket = "Largo",
                hideMultiplayer = false
            )
        )

        assertEquals(listOf("1"), games.map { it.platformGameId })
    }

    private fun juego(id: String, title: String): Juego {
        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = id,
            title = title,
            headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$id/header.jpg",
            capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$id/library_600x900.jpg",
            heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$id/capsule_616x353.jpg",
            iconImageUrl = null
        )
    }
}
