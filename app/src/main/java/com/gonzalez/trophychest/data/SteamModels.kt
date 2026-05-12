package com.gonzalez.trophychest.data

import com.google.gson.annotations.SerializedName

data class SteamOwnedGamesResponse(
    @SerializedName("response") val response: SteamOwnedGamesPayload = SteamOwnedGamesPayload()
)

data class SteamOwnedGamesPayload(
    @SerializedName("game_count") val gameCount: Int = 0,
    @SerializedName("games") val games: List<SteamGame> = emptyList()
)

data class SteamGame(
    val appid: Int,
    val name: String = "",
    @SerializedName("playtime_forever") val playtimeMinutes: Int = 0,
    @SerializedName("rtime_last_played") val lastPlayedEpochSeconds: Long? = null,
    @SerializedName("img_icon_url") val iconHash: String? = null
)

data class ResolveVanityUrlResponse(
    @SerializedName("response") val response: ResolveVanityUrlPayload
)

data class ResolveVanityUrlPayload(
    @SerializedName("success") val success: Int,
    @SerializedName("steamid") val steamId: String? = null,
    @SerializedName("message") val message: String? = null
)

data class PlayerSummariesResponse(
    @SerializedName("response") val response: PlayerSummariesPayload
)

data class PlayerSummariesPayload(
    @SerializedName("players") val players: List<SteamPlayerSummary> = emptyList()
)

data class SteamPlayerSummary(
    @SerializedName("steamid") val steamId: String,
    @SerializedName("personaname") val personaName: String,
    @SerializedName("avatarfull") val avatarUrl: String? = null
)

data class PlayerAchievementsResponse(
    @SerializedName("playerstats") val playerStats: PlayerStatsPayload? = null
)

data class PlayerStatsPayload(
    @SerializedName("steamID") val steamId: String? = null,
    @SerializedName("gameName") val gameName: String? = null,
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("achievements") val achievements: List<PlayerAchievement> = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class PlayerAchievement(
    @SerializedName("apiname") val apiName: String,
    @SerializedName("achieved") val achieved: Int = 0,
    @SerializedName("unlocktime") val unlockTime: Long = 0,
    @SerializedName("name") val displayName: String? = null,
    @SerializedName("description") val description: String? = null
)

data class SchemaForGameResponse(
    @SerializedName("game") val game: SteamGameSchema? = null
)

data class SteamGameSchema(
    @SerializedName("gameName") val gameName: String? = null,
    @SerializedName("availableGameStats") val availableGameStats: SteamAvailableGameStats? = null
)

data class SteamAvailableGameStats(
    @SerializedName("achievements") val achievements: List<SteamSchemaAchievement> = emptyList()
)

data class SteamSchemaAchievement(
    @SerializedName("name") val apiName: String,
    @SerializedName("displayName") val displayName: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("icongray") val iconGray: String? = null
)

data class GlobalAchievementPercentagesResponse(
    @SerializedName("achievementpercentages") val percentages: GlobalAchievementPercentagesPayload? = null
)

data class GlobalAchievementPercentagesPayload(
    @SerializedName("achievements") val achievements: List<GlobalAchievementPercentage> = emptyList()
)

data class GlobalAchievementPercentage(
    @SerializedName("name") val apiName: String,
    @SerializedName("percent") val percent: Double = 0.0
)

data class SteamStoreAppDetailsEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: SteamStoreAppDetails? = null
)

data class SteamStoreAppDetails(
    @SerializedName("steam_appid") val steamAppId: Int? = null,
    @SerializedName("required_age") val requiredAge: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    @SerializedName("header_image") val headerImage: String? = null,
    @SerializedName("background") val backgroundImage: String? = null,
    @SerializedName("background_raw") val backgroundRawImage: String? = null,
    @SerializedName("ratings") val ratings: SteamStoreRatings? = null,
    @SerializedName("developers") val developers: List<String> = emptyList(),
    @SerializedName("genres") val genres: List<SteamStoreGenre> = emptyList(),
    @SerializedName("categories") val categories: List<SteamStoreCategory> = emptyList(),
    @SerializedName("release_date") val releaseDate: SteamStoreReleaseDate? = null,
    @SerializedName("screenshots") val screenshots: List<SteamStoreScreenshot> = emptyList()
)

data class SteamStoreRatings(
    @SerializedName("pegi") val pegi: SteamStorePegiRating? = null
)

data class SteamStorePegiRating(
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("descriptors") val descriptors: String? = null
)

data class SteamStoreGenre(
    @SerializedName("description") val description: String
)

data class SteamStoreCategory(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("description") val description: String = ""
)

data class SteamStoreReleaseDate(
    @SerializedName("date") val date: String? = null
)

data class SteamStoreScreenshot(
    @SerializedName("path_full") val pathFull: String? = null
)

data class SteamFeaturedCategoriesResponse(
    @SerializedName("top_sellers") val topSellers: SteamFeaturedCategory? = null,
    @SerializedName("new_releases") val newReleases: SteamFeaturedCategory? = null,
    @SerializedName("specials") val specials: SteamFeaturedCategory? = null,
    @SerializedName("coming_soon") val comingSoon: SteamFeaturedCategory? = null
)

data class SteamFeaturedCategory(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("items") val items: List<SteamFeaturedItem> = emptyList()
)

data class SteamFeaturedItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("type") val type: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("header_image") val headerImage: String? = null,
    @SerializedName("large_capsule_image") val largeCapsuleImage: String? = null,
    @SerializedName("small_capsule_image") val smallCapsuleImage: String? = null,
    @SerializedName("discount_percent") val discountPercent: Int? = null
)

data class SteamAppReviewsResponse(
    @SerializedName("success") val success: Int = 0,
    @SerializedName("query_summary") val querySummary: SteamReviewQuerySummary? = null
)

data class SteamReviewQuerySummary(
    @SerializedName("review_score") val reviewScore: Int = 0,
    @SerializedName("review_score_desc") val reviewScoreDescription: String? = null,
    @SerializedName("total_positive") val totalPositive: Int = 0,
    @SerializedName("total_negative") val totalNegative: Int = 0,
    @SerializedName("total_reviews") val totalReviews: Int = 0
) {
    val positivePercent: Int
        get() = if (totalReviews <= 0) 0 else (totalPositive * 100) / totalReviews
}

data class SteamExploreGame(
    val game: Juego,
    val subtitle: String? = null,
    val badgeText: String? = null
)

data class SteamExploreContent(
    val topSellers: List<SteamExploreGame> = emptyList(),
    val recommended: List<SteamExploreGame> = emptyList(),
    val newReleases: List<SteamExploreGame> = emptyList(),
    val qualityTime: List<SteamExploreGame> = emptyList(),
    val recommendationMessage: String? = null
)

data class SteamExploreCacheEntry(
    val fetchedAtMillis: Long = 0L,
    val content: SteamExploreContent = SteamExploreContent()
)

data class SteamSearchResultsResponse(
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("results_html") val resultsHtml: String = ""
)

data class SteamCategoryDefinition(
    val slug: String,
    val displayName: String,
    val tagId: Int
)

data class SteamCategoryFilters(
    val trophyLevel: String = "Medio",
    val playtimeBucket: String = "Estandar",
    val hideMultiplayer: Boolean = false
)

data class SteamCategoryContent(
    val category: SteamCategoryDefinition,
    val games: List<Juego> = emptyList()
)

data class SteamCategoryCacheEntry(
    val fetchedAtMillis: Long = 0L,
    val content: SteamCategoryContent = SteamCategoryContent(
        category = SteamCategoryDefinition(slug = "", displayName = "", tagId = 0)
    )
)

data class SteamLinkedAccount(
    val steamId: String,
    val profileInput: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isPublicProfile: Boolean = true
)

data class SteamAchievement(
    val apiName: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockTime: Long?,
    val globalPercentage: Double?,
    val iconUrl: String?,
    val lockedIconUrl: String?
)

data class SteamAchievementBundle(
    val gameId: String,
    val gameTitle: String,
    val summary: ResumenTrofeos?,
    val achievements: List<SteamAchievement>,
    val hasAchievements: Boolean,
    val isPersonal: Boolean = true
)

sealed interface SteamSyncResult {
    data object MissingApiKey : SteamSyncResult
    data class Success(
        val linkedAccount: SteamLinkedAccount,
        val games: List<Juego>,
        val warningMessage: String? = null
    ) : SteamSyncResult
    data class Failure(val message: String) : SteamSyncResult
}
