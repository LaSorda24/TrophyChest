package com.gonzalez.trophychest.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IGDBProxyApiService {
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
