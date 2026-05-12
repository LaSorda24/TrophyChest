package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IGDBRepositoryTest {
    @Test
    fun proxyBaseUrl_prefersDedicatedIgdbProxyValue() {
        val resolved = IGDBRepository.proxyBaseUrl(
            igdbProxyBaseUrl = " https://igdb.example.com/ ",
            releaseCalendarBaseUrl = "https://legacy.example.com/"
        )

        assertEquals("https://igdb.example.com/", resolved)
    }

    @Test
    fun proxyBaseUrl_fallsBackToLegacyReleaseCalendarBaseUrl() {
        val resolved = IGDBRepository.proxyBaseUrl(
            igdbProxyBaseUrl = "   ",
            releaseCalendarBaseUrl = " https://legacy.example.com/ "
        )

        assertEquals("https://legacy.example.com/", resolved)
    }

    @Test
    fun proxyBaseUrl_returnsBlankWhenNoEndpointExists() {
        val resolved = IGDBRepository.proxyBaseUrl(
            igdbProxyBaseUrl = " ",
            releaseCalendarBaseUrl = " "
        )

        assertEquals("", resolved)
    }

    @Test
    fun categoryForSlug_mapsCurrentCategorySlugsToIgdbGenres() {
        assertEquals("Action", IGDBRepository.categoryForSlug("accion")?.igdbGenreName)
        assertEquals("Role-playing (RPG)", IGDBRepository.categoryForSlug("rpg")?.igdbGenreName)
        assertEquals("Horror", IGDBRepository.categoryForSlug("terror")?.igdbGenreName)
        assertEquals("Simulator", IGDBRepository.categoryForSlug("simulacion")?.igdbGenreName)
        assertEquals(null, IGDBRepository.categoryForSlug("desconocida"))
    }

    @Test
    fun isCacheFresh_acceptsEntriesInsideTtlOnly() {
        val now = 10_000L
        val ttl = 6_000L

        assertEquals(true, IGDBRepository.isCacheFresh(fetchedAtMillis = 5_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, IGDBRepository.isCacheFresh(fetchedAtMillis = 4_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, IGDBRepository.isCacheFresh(fetchedAtMillis = 11_000L, nowMillis = now, ttlMillis = ttl))
        assertEquals(false, IGDBRepository.isCacheFresh(fetchedAtMillis = 0L, nowMillis = now, ttlMillis = ttl))
    }
}
