package com.gonzalez.trophychest.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

enum class ReleasePlatformFamily(val label: String) {
    PC("PC"),
    PLAYSTATION("PlayStation"),
    XBOX("Xbox")
}

data class ReleaseCalendarResponse(
    @SerializedName("generatedAt") val generatedAt: String? = null,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("releases") val releases: List<ReleaseCalendarDto> = emptyList()
)

data class ReleaseCalendarDto(
    @SerializedName("igdbGameId") val igdbGameId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("humanDate") val humanDate: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("platformFamilies") val platformFamilies: List<String> = emptyList(),
    @SerializedName("platformNames") val platformNames: List<String> = emptyList()
)

data class ReleaseCalendarItem(
    val igdbGameId: Long,
    val name: String,
    val releaseDate: LocalDate,
    val humanDate: String,
    val coverUrl: String?,
    val summary: String?,
    val platformFamilies: Set<ReleasePlatformFamily>,
    val platformNames: List<String>
)

data class ReleaseCalendarCacheEntry(
    val fetchedAtMillis: Long = 0L,
    val from: String = "",
    val days: Int = 0,
    val limit: Int? = null,
    val releases: List<ReleaseCalendarDto> = emptyList()
)

object ReleaseCalendarMapper {
    fun map(dto: ReleaseCalendarDto): ReleaseCalendarItem? {
        val parsedDate = runCatching { LocalDate.parse(dto.releaseDate) }.getOrNull() ?: return null
        val families = dto.platformFamilies.mapNotNull { rawFamily ->
            runCatching { ReleasePlatformFamily.valueOf(rawFamily) }.getOrNull()
        }.toSet()

        return ReleaseCalendarItem(
            igdbGameId = dto.igdbGameId,
            name = dto.name,
            releaseDate = parsedDate,
            humanDate = dto.humanDate?.takeIf { it.isNotBlank() } ?: dto.releaseDate,
            coverUrl = dto.coverUrl?.takeIf { it.isNotBlank() },
            summary = dto.summary?.takeIf { it.isNotBlank() },
            platformFamilies = families,
            platformNames = dto.platformNames.distinct()
        )
    }

    fun filterByPlatform(
        releases: List<ReleaseCalendarItem>,
        selectedFamily: ReleasePlatformFamily?
    ): List<ReleaseCalendarItem> {
        if (selectedFamily == null) return releases
        return releases.filter { selectedFamily in it.platformFamilies }
    }
}
