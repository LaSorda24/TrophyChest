package com.gonzalez.trophychest.data

import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt

data class IGDBSearchResponse(
    @SerializedName("query") val query: String = "",
    @SerializedName("games") val games: List<IGDBGameDto> = emptyList()
)

data class IGDBGameDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("platforms") val platforms: List<String> = emptyList()
)

data class IGDBGameDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("heroImageUrl") val heroImageUrl: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("platforms") val platforms: List<String> = emptyList(),
    @SerializedName("developers") val developers: List<String> = emptyList(),
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("screenshots") val screenshots: List<String> = emptyList(),
    @SerializedName("ageRating") val ageRating: IGDBAgeRatingDto? = null,
    @SerializedName("steamAppId") val steamAppId: String? = null
)

data class IGDBAgeRatingDto(
    @SerializedName("label") val label: String? = null,
    @SerializedName("minimumAge") val minimumAge: Int? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

data class IGDBGame(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val platforms: List<String>
)

data class IGDBExploreResponse(
    @SerializedName("generatedAt") val generatedAt: String = "",
    @SerializedName("topSellers") val topSellers: List<IGDBExploreGameDto> = emptyList(),
    @SerializedName("recommended") val recommended: List<IGDBExploreGameDto> = emptyList(),
    @SerializedName("newReleases") val newReleases: List<IGDBExploreGameDto> = emptyList(),
    @SerializedName("qualityTime") val qualityTime: List<IGDBExploreGameDto> = emptyList(),
    @SerializedName("recommendationMessage") val recommendationMessage: String? = null
)

data class IGDBCategoryResponse(
    @SerializedName("genre") val genre: String = "",
    @SerializedName("generatedAt") val generatedAt: String = "",
    @SerializedName("games") val games: List<IGDBExploreGameDto> = emptyList()
)

data class IGDBExploreGameDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("heroImageUrl") val heroImageUrl: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("platforms") val platforms: List<String> = emptyList(),
    @SerializedName("developers") val developers: List<String> = emptyList(),
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("ratingCount") val ratingCount: Int? = null,
    @SerializedName("hypes") val hypes: Int? = null
)

data class IGDBExploreContent(
    val topSellers: List<IGDBExploreGame> = emptyList(),
    val recommended: List<IGDBExploreGame> = emptyList(),
    val newReleases: List<IGDBExploreGame> = emptyList(),
    val qualityTime: List<IGDBExploreGame> = emptyList(),
    val recommendationMessage: String? = null
)

data class IGDBExploreCacheEntry(
    val fetchedAtMillis: Long = 0L,
    val genreQuery: String? = null,
    val content: IGDBExploreContent = IGDBExploreContent()
)

data class IGDBCategoryDefinition(
    val slug: String,
    val displayName: String,
    val igdbGenreName: String
)

data class IGDBCategoryContent(
    val category: IGDBCategoryDefinition,
    val games: List<Juego> = emptyList()
)

data class IGDBExploreGame(
    val game: Juego,
    val subtitle: String? = null,
    val badgeText: String? = null
)

object IGDBMapper {
    fun mapSearchGame(dto: IGDBGameDto): IGDBGame? {
        if (dto.id <= 0) return null
        val name = dto.name.trim()
        if (name.isBlank()) return null

        return IGDBGame(
            id = dto.id,
            name = name,
            coverUrl = dto.coverUrl.sanitizedUrl(),
            platforms = dto.platforms.sanitizedNames()
        )
    }

    fun mapDetailToJuego(dto: IGDBGameDetailDto): Juego? {
        val searchGame = mapSearchGame(
            IGDBGameDto(
                id = dto.id,
                name = dto.name,
                coverUrl = dto.coverUrl,
                platforms = dto.platforms
            )
        ) ?: return null

        val coverUrl = searchGame.coverUrl
        val heroImageUrl = dto.heroImageUrl.sanitizedUrl() ?: coverUrl.orEmpty()
        val primaryImage = coverUrl ?: heroImageUrl

        return Juego(
            platform = PlataformaJuego.IGDB,
            platformGameId = dto.id.toString(),
            title = searchGame.name,
            headerImageUrl = primaryImage.orEmpty(),
            capsuleImageUrl = primaryImage.orEmpty(),
            heroImageUrl = heroImageUrl,
            iconImageUrl = null,
            shortDescription = dto.summary.sanitizedText(),
            developers = dto.developers.sanitizedNames(),
            genres = dto.genres.sanitizedNames(),
            releaseDate = dto.releaseDate.sanitizedText(),
            screenshots = dto.screenshots.sanitizedUrls(),
            supportedPlatforms = searchGame.platforms,
            ageRating = dto.ageRating.toPegiAgeRating(),
            steamAppId = dto.steamAppId.sanitizedSteamAppId()
        )
    }

    fun mapExploreContent(response: IGDBExploreResponse): IGDBExploreContent {
        return IGDBExploreContent(
            topSellers = response.topSellers.mapNotNull(::mapExploreGame),
            recommended = response.recommended.mapNotNull(::mapExploreGame),
            newReleases = response.newReleases.mapNotNull(::mapExploreGame),
            qualityTime = response.qualityTime.mapNotNull(::mapExploreGame),
            recommendationMessage = response.recommendationMessage.sanitizedText()
        )
    }

    fun mapCategoryContent(
        response: IGDBCategoryResponse,
        category: IGDBCategoryDefinition
    ): IGDBCategoryContent {
        return IGDBCategoryContent(
            category = category,
            games = response.games.mapNotNull(::mapExploreGame).map { it.game }
        )
    }

    fun mapExploreGame(dto: IGDBExploreGameDto): IGDBExploreGame? {
        val game = mapDetailToJuego(
            IGDBGameDetailDto(
                id = dto.id,
                name = dto.name,
                coverUrl = dto.coverUrl,
                heroImageUrl = dto.heroImageUrl,
                summary = dto.summary,
                platforms = dto.platforms,
                developers = dto.developers,
                genres = dto.genres,
                releaseDate = dto.releaseDate,
                screenshots = listOfNotNull(dto.heroImageUrl),
                ageRating = null,
                steamAppId = null
            )
        ) ?: return null

        return IGDBExploreGame(
            game = game,
            subtitle = dto.summary.sanitizedText()
                ?: dto.platforms.sanitizedNames().take(3).joinToString().takeIf { it.isNotBlank() },
            badgeText = dto.ratingBadge() ?: dto.hypeBadge() ?: dto.releaseDate.sanitizedText()
        )
    }

    private fun List<String>.sanitizedNames(): List<String> {
        return map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun List<String>.sanitizedUrls(): List<String> {
        return mapNotNull { it.sanitizedUrl() }.distinct()
    }

    private fun String?.sanitizedText(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.sanitizedUrl(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.sanitizedSteamAppId(): String? {
        return this?.trim()?.takeIf { it.matches(Regex("""^\d+$""")) }
    }

    private fun IGDBAgeRatingDto?.toPegiAgeRating(): PegiAgeRating? {
        val age = this?.minimumAge?.takeIf { it in listOf(3, 7, 12, 16, 18) } ?: return null
        val cleanLabel = label.sanitizedText() ?: "PEGI $age"
        return PegiAgeRating(
            label = cleanLabel,
            minimumAge = age,
            imageUrl = imageUrl.sanitizedUrl()
        )
    }

    private fun IGDBExploreGameDto.ratingBadge(): String? {
        val safeRating = rating?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val count = ratingCount?.takeIf { it > 0 }
        return if (count != null) {
            "${safeRating.roundToInt()}/100 IGDB"
        } else {
            "${safeRating.roundToInt()}/100"
        }
    }

    private fun IGDBExploreGameDto.hypeBadge(): String? {
        val safeHypes = hypes?.takeIf { it > 0 } ?: return null
        return "$safeHypes en seguimiento"
    }
}
