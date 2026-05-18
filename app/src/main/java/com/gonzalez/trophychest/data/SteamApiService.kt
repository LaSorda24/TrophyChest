package com.gonzalez.trophychest.data

import retrofit2.http.GET
import retrofit2.http.Query

// ESTE INTERFACE NO IMPLEMENTA LOGICA; SOLO DECLARA LAS RUTAS HTTP QUE RETROFIT SABE LLAMAR.
interface SteamApiService {
    // SUSPEND PORQUE CADA LLAMADA HTTP SE HACE DESDE CORRUTINA.
    @GET("IPlayerService/GetOwnedGames/v0001/")
    suspend fun getOwnedGames(
        @Query("key") apiKey: String,
        @Query("steamid") steamId: String,
        @Query("format") format: String = "json",
        @Query("include_appinfo") includeAppInfo: Int = 1,
        @Query("include_played_free_games") includePlayedFreeGames: Int = 1
    ): SteamOwnedGamesResponse

    @GET("ISteamUser/ResolveVanityURL/v1/")
    suspend fun resolveVanityUrl(
        @Query("key") apiKey: String,
        @Query("vanityurl") vanityUrl: String,
        @Query("format") format: String = "json"
    ): ResolveVanityUrlResponse

    @GET("ISteamUser/GetPlayerSummaries/v0002/")
    suspend fun getPlayerSummaries(
        @Query("key") apiKey: String,
        @Query("steamids") steamIds: String
    ): PlayerSummariesResponse

    @GET("ISteamUserStats/GetPlayerAchievements/v0001/")
    suspend fun getPlayerAchievements(
        @Query("key") apiKey: String,
        @Query("steamid") steamId: String,
        @Query("appid") appId: Int,
        @Query("l") language: String = "spanish"
    ): PlayerAchievementsResponse

    @GET("ISteamUserStats/GetSchemaForGame/v2/")
    suspend fun getSchemaForGame(
        @Query("key") apiKey: String,
        @Query("appid") appId: Int,
        @Query("l") language: String = "spanish"
    ): SchemaForGameResponse

    @GET("ISteamUserStats/GetGlobalAchievementPercentagesForApp/v0002/")
    suspend fun getGlobalAchievementPercentages(
        @Query("gameid") appId: Int,
        @Query("format") format: String = "json"
    ): GlobalAchievementPercentagesResponse
}

interface SteamStoreApiService {
    // STEAM STORE ES OTRA BASE URL DISTINTA A STEAM API, POR ESO ESTA EN OTRO SERVICIO.
    @GET("api/appdetails")
    suspend fun getAppDetails(
        @Query("appids") appIds: String,
        @Query("l") language: String = "spanish",
        @Query("cc") countryCode: String = "ES"
    ): Map<String, SteamStoreAppDetailsEnvelope>
}
