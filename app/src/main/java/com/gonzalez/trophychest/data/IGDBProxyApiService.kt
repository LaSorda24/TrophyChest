package com.gonzalez.trophychest.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ANDROID NO LLAMA A IGDB DIRECTAMENTE; LLAMA A MI PROXY Y EL PROXY HABLA CON IGDB/TWITCH.
interface IGDBProxyApiService {
    // @GET INDICA LA RUTA DEL WORKER Y @QUERY METE PARAMETROS EN LA URL.
    @GET("explore-games")
    suspend fun getExploreGames(
        @Query("genres") genres: String? = null,
        @Query("_cb") cacheBuster: String? = null
    ): IGDBExploreResponse

    @GET("category-games")
    suspend fun getCategoryGames(
        @Query("genre") genre: String
    ): IGDBCategoryResponse

    @GET("search-games")
    suspend fun searchGames(
        @Query("q") query: String,
        @Query("_cb") cacheBuster: String? = null
    ): IGDBSearchResponse

    @GET("games/{gameId}")
    suspend fun getGameDetails(
        @Path("gameId") gameId: Long,
        @Query("_cb") cacheBuster: String? = null
    ): IGDBGameDetailDto
}
