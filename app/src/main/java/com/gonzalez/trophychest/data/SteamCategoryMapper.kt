package com.gonzalez.trophychest.data

import org.jsoup.Jsoup

object SteamCategoryMapper {
    val categories: List<SteamCategoryDefinition> = listOf(
        SteamCategoryDefinition(slug = "accion", displayName = "ACCION", tagId = 19),
        SteamCategoryDefinition(slug = "aventura", displayName = "AVENTURA", tagId = 21),
        SteamCategoryDefinition(slug = "rpg", displayName = "RPG", tagId = 122),
        SteamCategoryDefinition(slug = "terror", displayName = "TERROR", tagId = 1667),
        SteamCategoryDefinition(slug = "indie", displayName = "INDIE", tagId = 492),
        SteamCategoryDefinition(slug = "estrategia", displayName = "ESTRATEGIA", tagId = 9),
        SteamCategoryDefinition(slug = "deportes", displayName = "DEPORTES", tagId = 701),
        SteamCategoryDefinition(slug = "simulacion", displayName = "SIMULACION", tagId = 599),
        SteamCategoryDefinition(slug = "carreras", displayName = "CARRERAS", tagId = 699)
    )

    fun categoryForSlug(slug: String): SteamCategoryDefinition? {
        return categories.firstOrNull { it.slug == slug.trim().lowercase() }
    }

    fun parseSearchResultsHtml(resultsHtml: String): List<Juego> {
        if (resultsHtml.isBlank()) return emptyList()

        return Jsoup.parse(resultsHtml)
            .select("a.search_result_row")
            .mapNotNull { element ->
                val appId = element.attr("data-ds-appid")
                    .split(",")
                    .firstOrNull()
                    ?.trim()
                    ?.takeIf { it.toIntOrNull() != null }
                    ?: appIdFromHref(element.attr("href"))
                    ?: return@mapNotNull null
                val title = element.selectFirst(".title")
                    ?.text()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val imageUrl = element.selectFirst("img")?.attr("src")?.takeIf { it.isNotBlank() }

                Juego(
                    platform = PlataformaJuego.STEAM,
                    platformGameId = appId,
                    title = title,
                    headerImageUrl = imageUrl ?: "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg",
                    capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg",
                    heroImageUrl = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/$appId/capsule_616x353.jpg",
                    iconImageUrl = imageUrl
                )
            }
            .distinctBy { it.platformGameId }
    }

    fun applyFilters(
        games: List<Juego>,
        detailsByAppId: Map<String, SteamStoreAppDetails>,
        filters: SteamCategoryFilters
    ): List<Juego> {
        return games
            .filter { game -> !filters.hideMultiplayer || !game.isMultiplayer(detailsByAppId[game.platformGameId]) }
            .filter { game -> game.matchesTrophyLevel(filters.trophyLevel) }
            .filter { game -> game.matchesPlaytimeBucket(filters.playtimeBucket) }
    }

    private fun appIdFromHref(href: String): String? {
        return Regex("""/app/(\d+)""").find(href)?.groupValues?.getOrNull(1)
    }

    private fun Juego.isMultiplayer(details: SteamStoreAppDetails?): Boolean {
        val categoryNames = details?.categories.orEmpty().map { it.description.trim().lowercase() }
        return categoryNames.any { category ->
            category == "multi-player" ||
                category == "online pvp" ||
                category == "online co-op" ||
                category == "mmo" ||
                category == "massively multiplayer" ||
                category.contains("multi-player") ||
                category.contains("multiplayer")
        }
    }

    private fun Juego.matchesTrophyLevel(level: String): Boolean {
        val total = achievementSummary?.total ?: return true
        return when (level.trim().lowercase()) {
            "bajo" -> total <= 30
            "alto" -> total >= 70
            else -> total in 31..69
        }
    }

    private fun Juego.matchesPlaytimeBucket(bucket: String): Boolean {
        if (playtimeMinutes <= 0) return true
        return when (bucket.trim().lowercase()) {
            "express" -> playtimeMinutes <= 5 * 60
            "largo" -> playtimeMinutes >= 25 * 60
            else -> playtimeMinutes in (5 * 60 + 1)..(25 * 60 - 1)
        }
    }
}
