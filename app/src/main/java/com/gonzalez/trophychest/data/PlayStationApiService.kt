package com.gonzalez.trophychest.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlayStationApiService {
    @GET("psn/status")
    suspend fun getStatus(): PlayStationStatusResponse

    @POST("psn/auth/npsso")
    suspend fun authWithNpsso(
        @Body request: PlayStationNpssoAuthRequest
    ): PlayStationAuthResponse

    @POST("psn/title-history")
    suspend fun getTitleHistory(
        @Body request: PlayStationRefreshRequest
    ): PlayStationTitleHistoryResponse

    @POST("psn/trophy-titles")
    suspend fun getTrophyTitles(
        @Body request: PlayStationRefreshRequest
    ): PlayStationTrophyTitlesResponse

    @POST("psn/achievements/{npCommunicationId}")
    suspend fun getAchievements(
        @Path("npCommunicationId") npCommunicationId: String,
        @Body request: PlayStationAchievementsRequest
    ): PlayStationAchievementResponse
}
