package com.gonzalez.trophychest.data

import com.google.gson.annotations.SerializedName

data class PlayStationStatusResponse(
    @SerializedName("configured") val configured: Boolean = false,
    @SerializedName("authMode") val authMode: String = "",
    @SerializedName("message") val message: String? = null
)

data class PlayStationNpssoAuthRequest(
    @SerializedName("npsso") val npsso: String
)

data class PlayStationRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class PlayStationAchievementsRequest(
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("npServiceName") val npServiceName: String? = null
)

data class PlayStationSessionPayload(
    @SerializedName("refreshToken") val refreshToken: String = "",
    @SerializedName("refreshTokenExpiresIn") val refreshTokenExpiresIn: Long? = null,
    @SerializedName("accessTokenExpiresIn") val accessTokenExpiresIn: Long? = null,
    @SerializedName("tokenType") val tokenType: String? = null
)

data class PlayStationLinkedAccountPayload(
    @SerializedName("accountId") val accountId: String = "me",
    @SerializedName("onlineId") val onlineId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("isPlus") val isPlus: Boolean = false
)

data class PlayStationAuthResponse(
    @SerializedName("linkedAccount") val linkedAccount: PlayStationLinkedAccountPayload = PlayStationLinkedAccountPayload(),
    @SerializedName("auth") val auth: PlayStationSessionPayload = PlayStationSessionPayload()
)

data class PlayStationTitleHistoryResponse(
    @SerializedName("auth") val auth: PlayStationSessionPayload = PlayStationSessionPayload(),
    @SerializedName("totalItemCount") val totalItemCount: Int = 0,
    @SerializedName("titles") val titles: List<PlayStationPlayedTitle> = emptyList()
)

data class PlayStationPlayedTitle(
    @SerializedName("titleId") val titleId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("localizedName") val localizedName: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("heroImageUrl") val heroImageUrl: String? = null,
    @SerializedName("lastPlayedDateTime") val lastPlayedDateTime: String? = null,
    @SerializedName("lastPlayedEpochSeconds") val lastPlayedEpochSeconds: Long? = null,
    @SerializedName("playDuration") val playDuration: String? = null,
    @SerializedName("playtimeMinutes") val playtimeMinutes: Int = 0,
    @SerializedName("playCount") val playCount: Int = 0,
    @SerializedName("category") val category: String = "",
    @SerializedName("service") val service: String = "",
    @SerializedName("conceptId") val conceptId: Long? = null,
    @SerializedName("conceptTitleIds") val conceptTitleIds: List<String> = emptyList(),
    @SerializedName("supportedPlatforms") val supportedPlatforms: List<String> = emptyList()
)

data class PlayStationTrophyTitlesResponse(
    @SerializedName("auth") val auth: PlayStationSessionPayload = PlayStationSessionPayload(),
    @SerializedName("totalItemCount") val totalItemCount: Int = 0,
    @SerializedName("titles") val titles: List<PlayStationTrophyTitle> = emptyList()
)

data class PlayStationTrophyTitle(
    @SerializedName("npCommunicationId") val npCommunicationId: String = "",
    @SerializedName("npServiceName") val npServiceName: String = "",
    @SerializedName("trophySetVersion") val trophySetVersion: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("platform") val platform: String = "",
    @SerializedName("hasTrophyGroups") val hasTrophyGroups: Boolean = false,
    @SerializedName("hidden") val hidden: Boolean = false,
    @SerializedName("lastUpdatedDateTime") val lastUpdatedDateTime: String? = null,
    @SerializedName("lastUpdatedEpochSeconds") val lastUpdatedEpochSeconds: Long? = null,
    @SerializedName("summary") val summary: PlayStationSummaryPayload? = null
)

data class PlayStationSummaryPayload(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("unlocked") val unlocked: Int = 0,
    @SerializedName("progress") val progress: Float = 0f
)

data class PlayStationAchievementResponse(
    @SerializedName("auth") val auth: PlayStationSessionPayload = PlayStationSessionPayload(),
    @SerializedName("npCommunicationId") val npCommunicationId: String = "",
    @SerializedName("npServiceName") val npServiceName: String = "",
    @SerializedName("trophySetVersion") val trophySetVersion: String = "",
    @SerializedName("hasTrophyGroups") val hasTrophyGroups: Boolean = false,
    @SerializedName("lastUpdatedDateTime") val lastUpdatedDateTime: String? = null,
    @SerializedName("summary") val summary: PlayStationSummaryPayload? = null,
    @SerializedName("trophies") val trophies: List<PlayStationAchievement> = emptyList()
)

data class PlayStationAchievement(
    @SerializedName("trophyId") val trophyId: Int = 0,
    @SerializedName("apiName") val apiName: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("hidden") val hidden: Boolean = false,
    @SerializedName("unlocked") val unlocked: Boolean = false,
    @SerializedName("unlockTime") val unlockTime: Long? = null,
    @SerializedName("unlockDateTime") val unlockDateTime: String? = null,
    @SerializedName("globalPercentage") val globalPercentage: Double? = null,
    @SerializedName("rarity") val rarity: Int? = null,
    @SerializedName("trophyType") val trophyType: String = "",
    @SerializedName("groupId") val groupId: String = "default",
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("lockedIconUrl") val lockedIconUrl: String? = null
)

data class PlayStationLinkedAccount(
    val accountId: String,
    val onlineId: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isPlus: Boolean = false,
    val refreshToken: String,
    val refreshTokenExpiresAtEpochSeconds: Long? = null
)

data class PlayStationAchievementBundle(
    val gameId: String,
    val gameTitle: String,
    val summary: ResumenTrofeos?,
    val achievements: List<PlayStationAchievement>,
    val hasAchievements: Boolean
)

sealed interface PlayStationSyncResult {
    data object MissingProxyEndpoint : PlayStationSyncResult
    data object MissingSession : PlayStationSyncResult
    data class LinkedOnly(
        val linkedAccount: PlayStationLinkedAccount,
        val message: String
    ) : PlayStationSyncResult
    data class Success(
        val linkedAccount: PlayStationLinkedAccount,
        val games: List<Juego>,
        val trophyGames: List<Juego>,
        val warningMessage: String? = null
    ) : PlayStationSyncResult
    data class Failure(val message: String) : PlayStationSyncResult
}
