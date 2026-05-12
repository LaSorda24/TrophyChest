package com.gonzalez.trophychest.data

import retrofit2.http.GET
import retrofit2.http.Query

interface ReleaseCalendarApiService {
    @GET("release-calendar")
    suspend fun getReleaseCalendar(
        @Query("from") from: String,
        @Query("days") days: Int = 60,
        @Query("limit") limit: Int? = null
    ): ReleaseCalendarResponse
}
