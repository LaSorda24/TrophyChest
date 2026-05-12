package com.gonzalez.trophychest.data

import android.content.Context
import com.gonzalez.trophychest.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object ReleaseCalendarRepository {
    private const val PREFS_NAME = "release_calendar_repository"
    private const val KEY_CACHED_UPCOMING_RELEASES = "cached_upcoming_releases"
    private const val RELEASE_CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    private const val DEFAULT_DAYS = 60
    private const val DEFAULT_LIMIT = 60
    private val gson = Gson()

    fun hasEndpoint(): Boolean = BuildConfig.RELEASE_CALENDAR_BASE_URL.isNotBlank()

    suspend fun getUpcomingReleases(
        context: Context,
        from: LocalDate = LocalDate.now(),
        days: Int = DEFAULT_DAYS,
        limit: Int? = DEFAULT_LIMIT,
        forceRefresh: Boolean = false
    ): Result<List<ReleaseCalendarItem>> = withContext(Dispatchers.IO) {
        val fromText = from.toString()
        val cached = loadUpcomingReleasesCache(context, fromText, days, limit)
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && isCacheFresh(cached.fetchedAtMillis, now, RELEASE_CACHE_TTL_MILLIS)) {
            return@withContext Result.success(cached.releases.mapNotNull(ReleaseCalendarMapper::map))
        }

        val baseUrl = BuildConfig.RELEASE_CALENDAR_BASE_URL.trim()
        if (baseUrl.isBlank()) {
            if (cached != null) {
                return@withContext Result.success(cached.releases.mapNotNull(ReleaseCalendarMapper::map))
            }
            return@withContext Result.failure(
                IllegalStateException(
                    "Configura RELEASE_CALENDAR_BASE_URL en local.properties para activar el calendario de estrenos."
                )
            )
        }

        val result = runCatching {
            val response = RetrofitInstance.releaseCalendarApi(baseUrl)
                .getReleaseCalendar(from = fromText, days = days, limit = limit)
            saveUpcomingReleasesCache(context, fromText, days, limit, response.releases)
            response.releases.mapNotNull(ReleaseCalendarMapper::map)
        }

        result.getOrNull()?.let { releases -> return@withContext Result.success(releases) }
        cached?.let { return@withContext Result.success(it.releases.mapNotNull(ReleaseCalendarMapper::map)) }
        result
    }

    internal fun isCacheFresh(fetchedAtMillis: Long, nowMillis: Long, ttlMillis: Long): Boolean {
        return fetchedAtMillis > 0L && nowMillis - fetchedAtMillis in 0 until ttlMillis
    }

    internal fun cacheKey(from: String, days: Int, limit: Int?): String {
        return "$KEY_CACHED_UPCOMING_RELEASES::$from::$days::${limit ?: "all"}"
    }

    private fun saveUpcomingReleasesCache(
        context: Context,
        from: String,
        days: Int,
        limit: Int?,
        releases: List<ReleaseCalendarDto>
    ) {
        val entry = ReleaseCalendarCacheEntry(
            fetchedAtMillis = System.currentTimeMillis(),
            from = from,
            days = days,
            limit = limit,
            releases = releases
        )
        prefs(context).edit()
            .putString(cacheKey(from, days, limit), gson.toJson(entry))
            .apply()
    }

    private fun loadUpcomingReleasesCache(
        context: Context,
        from: String,
        days: Int,
        limit: Int?
    ): ReleaseCalendarCacheEntry? {
        val json = prefs(context).getString(cacheKey(from, days, limit), null) ?: return null
        return runCatching { gson.fromJson(json, ReleaseCalendarCacheEntry::class.java) }.getOrNull()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
