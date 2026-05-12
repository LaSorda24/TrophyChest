package com.gonzalez.trophychest.data

import kotlin.math.log10
import kotlin.math.max

object SteamExploreMapper {
    fun featuredItemToJuego(item: SteamFeaturedItem): Juego? {
        val appId = item.id ?: return null
        if (item.type != null && item.type != 0) return null

        val title = item.name?.takeIf { it.isNotBlank() } ?: return null
        val appIdText = appId.toString()
        val header = item.headerImage ?: "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg"
        val capsule = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg"
        val hero = item.largeCapsuleImage ?: header

        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = appIdText,
            title = title,
            headerImageUrl = header,
            capsuleImageUrl = capsule,
            heroImageUrl = hero,
            iconImageUrl = item.smallCapsuleImage
        )
    }

    fun detailsToJuego(appId: String, details: SteamStoreAppDetails): Juego? {
        val title = details.name?.takeIf { it.isNotBlank() } ?: return null
        val header = details.headerImage ?: "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg"
        val hero = details.backgroundRawImage ?: details.backgroundImage ?: header

        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = appId,
            title = title,
            headerImageUrl = header,
            capsuleImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg",
            heroImageUrl = hero,
            iconImageUrl = null,
            shortDescription = details.shortDescription,
            developers = details.developers,
            genres = details.genres.map { it.description },
            releaseDate = details.releaseDate?.date,
            screenshots = details.screenshots.mapNotNull { it.pathFull }
        )
    }

    fun recommendFromLibrary(
        ownedGames: List<Juego>,
        candidates: List<Juego>,
        detailsByGameId: Map<String, SteamStoreAppDetails>,
        limit: Int
    ): List<Juego> {
        val ownedIds = ownedGames.map { it.platformGameId }.toSet()
        val genreWeights = mutableMapOf<String, Int>()

        ownedGames
            .sortedByDescending { it.playtimeMinutes }
            .take(25)
            .forEach { game ->
                val genres = game.genres.ifEmpty {
                    detailsByGameId[game.platformGameId]?.genres?.map { it.description }.orEmpty()
                }
                val weight = max(1, game.playtimeMinutes / 60)
                genres.forEach { genre ->
                    val key = genre.normalizedGenre()
                    genreWeights[key] = (genreWeights[key] ?: 0) + weight
                }
            }

        if (genreWeights.isEmpty()) return emptyList()

        return candidates
            .filterNot { it.platformGameId in ownedIds }
            .map { game ->
                val genres = game.genres.ifEmpty {
                    detailsByGameId[game.platformGameId]?.genres?.map { it.description }.orEmpty()
                }
                val score = genres.sumOf { genre -> genreWeights[genre.normalizedGenre()] ?: 0 }
                game to score
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<Juego, Int>> { it.second }
                    .thenBy { it.first.title }
            )
            .take(limit)
            .map { it.first }
    }

    fun sortByQuality(
        candidates: List<Juego>,
        reviewSummaries: Map<String, SteamReviewQuerySummary>,
        limit: Int
    ): List<Juego> {
        return candidates
            .mapNotNull { game ->
                val summary = reviewSummaries[game.platformGameId] ?: return@mapNotNull null
                if (summary.totalReviews < 50 || summary.positivePercent < 70) return@mapNotNull null
                game to summary
            }
            .sortedWith(
                compareByDescending<Pair<Juego, SteamReviewQuerySummary>> { (_, summary) -> qualityScore(summary) }
                    .thenBy { it.first.title }
            )
            .take(limit)
            .map { it.first }
    }

    fun reviewBadge(summary: SteamReviewQuerySummary?): String? {
        if (summary == null || summary.totalReviews <= 0) return null
        return "${summary.positivePercent}% positivas"
    }

    fun recommendationSubtitle(game: Juego, detailsByGameId: Map<String, SteamStoreAppDetails>): String {
        val genres = game.genres.ifEmpty {
            detailsByGameId[game.platformGameId]?.genres?.map { it.description }.orEmpty()
        }
        return if (genres.isNotEmpty()) {
            "Afin a tus juegos de ${genres.take(2).joinToString(" / ")}"
        } else {
            "Basado en tu biblioteca de Steam"
        }
    }

    private fun qualityScore(summary: SteamReviewQuerySummary): Double {
        return summary.positivePercent + summary.reviewScore + (log10(summary.totalReviews.toDouble() + 1.0) * 4.0)
    }

    private fun String.normalizedGenre(): String = trim().lowercase()
}
