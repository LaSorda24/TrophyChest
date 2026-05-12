package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamAchievementBundleBuilderTest {
    @Test
    fun buildAchievementBundle_usesPlayerAchievementsWhenSchemaIsEmpty() {
        val bundle = SteamRepository.buildAchievementBundle(
            game = testGame(),
            playerStats = PlayerStatsPayload(
                gameName = "Portal 2",
                achievements = listOf(
                    PlayerAchievement(
                        apiName = "ACH_WIN",
                        achieved = 1,
                        unlockTime = 1_700_000_000,
                        displayName = "Victoria",
                        description = "Completa el juego"
                    ),
                    PlayerAchievement(
                        apiName = "ACH_SECRET",
                        achieved = 0,
                        displayName = "Secreto",
                        description = "Encuentra una sala oculta"
                    )
                )
            ),
            schemaAchievements = emptyList(),
            globalPercentages = emptyMap()
        )

        assertTrue(bundle.hasAchievements)
        assertEquals(2, bundle.summary?.total)
        assertEquals(1, bundle.summary?.desbloqueados)
        assertEquals(0.5f, bundle.summary?.porcentajeCompletado ?: 0f)
        assertEquals("Victoria", bundle.achievements[0].title)
        assertEquals("Completa el juego", bundle.achievements[0].description)
        assertNull(bundle.achievements[0].globalPercentage)
    }

    @Test
    fun buildAchievementBundle_keepsAchievementsWhenGlobalPercentagesAreMissing() {
        val bundle = SteamRepository.buildAchievementBundle(
            game = testGame(),
            playerStats = PlayerStatsPayload(
                achievements = listOf(
                    PlayerAchievement(apiName = "ACH_ONE", achieved = 1)
                )
            ),
            schemaAchievements = listOf(
                SteamSchemaAchievement(
                    apiName = "ACH_ONE",
                    displayName = "Primer logro",
                    description = "Desbloqueado",
                    icon = "icon-open",
                    iconGray = "icon-closed"
                ),
                SteamSchemaAchievement(
                    apiName = "ACH_TWO",
                    displayName = "Segundo logro",
                    description = "Pendiente"
                )
            ),
            globalPercentages = emptyMap()
        )

        assertTrue(bundle.hasAchievements)
        assertEquals(2, bundle.summary?.total)
        assertEquals(1, bundle.summary?.desbloqueados)
        assertEquals("Primer logro", bundle.achievements[0].title)
        assertEquals("icon-open", bundle.achievements[0].iconUrl)
        assertEquals("icon-closed", bundle.achievements[0].lockedIconUrl)
        assertNull(bundle.achievements[0].globalPercentage)
        assertFalse(bundle.achievements[1].unlocked)
    }

    @Test
    fun buildAchievementBundle_combinesSchemaPlayerProgressAndGlobalPercentage() {
        val bundle = SteamRepository.buildAchievementBundle(
            game = testGame(),
            playerStats = PlayerStatsPayload(
                gameName = "Half-Life",
                achievements = listOf(
                    PlayerAchievement(apiName = "ACH_ONE", achieved = 1),
                    PlayerAchievement(apiName = "ACH_EXTRA", achieved = 1, displayName = "Extra")
                )
            ),
            schemaAchievements = listOf(
                SteamSchemaAchievement(apiName = "ACH_ONE", displayName = "Uno"),
                SteamSchemaAchievement(apiName = "ACH_TWO", displayName = "Dos")
            ),
            globalPercentages = mapOf(
                "ACH_ONE" to GlobalAchievementPercentage(apiName = "ACH_ONE", percent = 42.5)
            )
        )

        assertEquals(listOf("ACH_ONE", "ACH_TWO", "ACH_EXTRA"), bundle.achievements.map { it.apiName })
        assertEquals(3, bundle.summary?.total)
        assertEquals(2, bundle.summary?.desbloqueados)
        assertEquals(42.5, bundle.achievements[0].globalPercentage ?: 0.0, 0.0)
        assertFalse(bundle.achievements[1].unlocked)
        assertEquals("Extra", bundle.achievements[2].title)
    }

    @Test
    fun buildAchievementBundle_returnsEmptyBundleOnlyWhenNoSourcesHaveAchievements() {
        val bundle = SteamRepository.buildAchievementBundle(
            game = testGame(),
            playerStats = PlayerStatsPayload(),
            schemaAchievements = emptyList(),
            globalPercentages = emptyMap()
        )

        assertFalse(bundle.hasAchievements)
        assertNull(bundle.summary)
        assertTrue(bundle.achievements.isEmpty())
    }

    @Test
    fun buildPublicAchievementBundle_marksAchievementsAsReadOnly() {
        val bundle = SteamRepository.buildPublicAchievementBundle(
            appId = "620",
            fallbackTitle = "Portal 2",
            schema = SteamGameSchema(
                gameName = "Portal 2",
                availableGameStats = SteamAvailableGameStats(
                    achievements = listOf(
                        SteamSchemaAchievement(
                            apiName = "ACH_PUBLIC",
                            displayName = "Logro publico",
                            description = "Visible sin biblioteca",
                            icon = "icon",
                            iconGray = "locked"
                        )
                    )
                )
            ),
            globalPercentages = mapOf(
                "ACH_PUBLIC" to GlobalAchievementPercentage("ACH_PUBLIC", 25.0)
            )
        )

        assertTrue(bundle.hasAchievements)
        assertFalse(bundle.isPersonal)
        assertEquals(1, bundle.summary?.total)
        assertEquals(0, bundle.summary?.desbloqueados)
        assertFalse(bundle.achievements.first().unlocked)
        assertEquals(25.0, bundle.achievements.first().globalPercentage ?: 0.0, 0.0)
    }

    @Test
    fun pegiAgeRatingFromStoreDetails_prefersSteamPegiRating() {
        val ageRating = SteamRepository.pegiAgeRatingFromStoreDetails(
            SteamStoreAppDetails(
                requiredAge = "0",
                ratings = SteamStoreRatings(
                    pegi = SteamStorePegiRating(rating = "16")
                )
            )
        )

        requireNotNull(ageRating)
        assertEquals("PEGI 16", ageRating.label)
        assertEquals(16, ageRating.minimumAge)
        assertNull(ageRating.imageUrl)
    }

    @Test
    fun pegiAgeRatingFromStoreDetails_fallsBackToRequiredAge() {
        val ageRating = SteamRepository.pegiAgeRatingFromStoreDetails(
            SteamStoreAppDetails(requiredAge = "18")
        )

        requireNotNull(ageRating)
        assertEquals("+18", ageRating.label)
        assertEquals(18, ageRating.minimumAge)
    }

    @Test
    fun shouldUseCachedAchievementBundle_ignoresEmptyOrFalseBundles() {
        val staleBundle = SteamAchievementBundle(
            gameId = "444090",
            gameTitle = "Paladins",
            summary = null,
            achievements = emptyList(),
            hasAchievements = false
        )

        val emptyButFlaggedBundle = staleBundle.copy(hasAchievements = true)

        val validBundle = SteamRepository.buildAchievementBundle(
            game = testGame().copy(platformGameId = "444090", title = "Paladins"),
            playerStats = PlayerStatsPayload(
                gameName = "Paladins",
                achievements = listOf(PlayerAchievement(apiName = "ACH_WIN_MATCH", achieved = 1))
            ),
            schemaAchievements = emptyList(),
            globalPercentages = emptyMap()
        )

        assertFalse(SteamRepository.shouldUseCachedAchievementBundle(staleBundle))
        assertFalse(SteamRepository.shouldUseCachedAchievementBundle(emptyButFlaggedBundle))
        assertTrue(SteamRepository.shouldUseCachedAchievementBundle(validBundle))
    }

    private fun testGame(): Juego {
        return Juego(
            platform = PlataformaJuego.STEAM,
            platformGameId = "620",
            title = "Portal 2",
            headerImageUrl = "header",
            capsuleImageUrl = "capsule",
            heroImageUrl = "hero",
            iconImageUrl = null
        )
    }
}
