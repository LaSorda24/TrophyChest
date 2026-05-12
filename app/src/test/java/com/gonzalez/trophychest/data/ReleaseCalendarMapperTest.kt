package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReleaseCalendarMapperTest {
    @Test
    fun map_parsesDateAndFamilies() {
        val item = ReleaseCalendarMapper.map(
            ReleaseCalendarDto(
                igdbGameId = 42L,
                name = "Future Game",
                releaseDate = "2026-05-10",
                humanDate = "May 10, 2026",
                coverUrl = "https://images.igdb.com/cover.jpg",
                platformFamilies = listOf("PC", "PLAYSTATION"),
                platformNames = listOf("PC (Microsoft Windows)", "PlayStation 5", "PlayStation 5")
            )
        )

        requireNotNull(item)
        assertEquals(42L, item.igdbGameId)
        assertEquals(LocalDate.of(2026, 5, 10), item.releaseDate)
        assertEquals(setOf(ReleasePlatformFamily.PC, ReleasePlatformFamily.PLAYSTATION), item.platformFamilies)
        assertEquals(listOf("PC (Microsoft Windows)", "PlayStation 5"), item.platformNames)
    }

    @Test
    fun map_returnsNullForInvalidDate() {
        val item = ReleaseCalendarMapper.map(
            ReleaseCalendarDto(
                igdbGameId = 42L,
                name = "Broken Game",
                releaseDate = "soon"
            )
        )

        assertNull(item)
    }

    @Test
    fun filterByPlatform_returnsOnlyMatchingFamily() {
        val pcRelease = ReleaseCalendarItem(
            igdbGameId = 1L,
            name = "PC Game",
            releaseDate = LocalDate.of(2026, 5, 10),
            humanDate = "May 10, 2026",
            coverUrl = null,
            summary = null,
            platformFamilies = setOf(ReleasePlatformFamily.PC),
            platformNames = listOf("PC (Microsoft Windows)")
        )
        val xboxRelease = pcRelease.copy(
            igdbGameId = 2L,
            name = "Xbox Game",
            platformFamilies = setOf(ReleasePlatformFamily.XBOX),
            platformNames = listOf("Xbox Series X|S")
        )

        val filtered = ReleaseCalendarMapper.filterByPlatform(
            listOf(pcRelease, xboxRelease),
            ReleasePlatformFamily.XBOX
        )

        assertEquals(listOf(xboxRelease), filtered)
        assertTrue(ReleaseCalendarMapper.filterByPlatform(listOf(pcRelease), null).contains(pcRelease))
    }

    @Test
    fun repositoryCacheFreshness_acceptsEntriesInsideTtlOnly() {
        val now = 10_000L
        val ttl = 6_000L

        assertTrue(ReleaseCalendarRepository.isCacheFresh(fetchedAtMillis = 5_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, ReleaseCalendarRepository.isCacheFresh(fetchedAtMillis = 4_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, ReleaseCalendarRepository.isCacheFresh(fetchedAtMillis = 11_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, ReleaseCalendarRepository.isCacheFresh(fetchedAtMillis = 0L, nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun repositoryCacheKeyIncludesWindowAndLimit() {
        assertEquals(
            "cached_upcoming_releases::2026-05-02::60::60",
            ReleaseCalendarRepository.cacheKey(from = "2026-05-02", days = 60, limit = 60)
        )
        assertEquals(
            "cached_upcoming_releases::2026-05-02::90::all",
            ReleaseCalendarRepository.cacheKey(from = "2026-05-02", days = 90, limit = null)
        )
    }
}
