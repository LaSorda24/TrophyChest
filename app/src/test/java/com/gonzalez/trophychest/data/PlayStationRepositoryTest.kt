package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStationRepositoryTest {
    @Test
    fun parseIsoDurationMinutes_handlesPlayStationDurations() {
        assertEquals(13736, PlayStationRepository.parseIsoDurationMinutes("PT228H56M33S"))
        assertEquals(1563, PlayStationRepository.parseIsoDurationMinutes("P1DT2H3M4S"))
        assertEquals(0, PlayStationRepository.parseIsoDurationMinutes("not-a-duration"))
    }

    @Test
    fun playedTitle_mapsToPsnJuegoForHome() {
        val title = PlayStationPlayedTitle(
            titleId = "PPSA00001_00",
            name = "Astro",
            localizedName = "Astro Bot",
            imageUrl = "https://example.com/icon.jpg",
            heroImageUrl = "https://example.com/hero.jpg",
            lastPlayedEpochSeconds = 1000,
            playDuration = "PT2H30M",
            supportedPlatforms = listOf("PlayStation 5")
        )

        val game = PlayStationRepository.run { title.toJuego() }

        requireNotNull(game)
        assertEquals(PlataformaJuego.PSN, game.platform)
        assertEquals("PPSA00001_00", game.platformGameId)
        assertEquals("Astro Bot", game.title)
        assertEquals(150, game.playtimeMinutes)
        assertEquals("https://example.com/hero.jpg", game.heroImageUrl)
    }

    @Test
    fun playedTitle_withoutTitleIdOrName_isIgnored() {
        val missingId = PlayStationPlayedTitle(titleId = "", name = "Astro")
        val missingName = PlayStationPlayedTitle(titleId = "PPSA00001_00", name = "")

        assertNull(PlayStationRepository.run { missingId.toJuego() })
        assertNull(PlayStationRepository.run { missingName.toJuego() })
    }

    @Test
    fun trophyTitle_mapsToPsnJuegoWithAchievementSummary() {
        val title = PlayStationTrophyTitle(
            npCommunicationId = "NPWR20188_00",
            npServiceName = "trophy2",
            title = "Astro's Playroom",
            iconUrl = "https://example.com/astro.png",
            platform = "PS5",
            lastUpdatedEpochSeconds = 2000,
            summary = PlayStationSummaryPayload(total = 46, unlocked = 23, progress = 0.5f)
        )

        val game = PlayStationRepository.run { title.toJuego() }

        requireNotNull(game)
        assertEquals(PlataformaJuego.PSN, game.platform)
        assertEquals("NPWR20188_00", game.platformGameId)
        assertEquals("NPWR20188_00", game.achievementGameId)
        assertEquals(46, game.achievementSummary?.total)
        assertEquals(23, game.achievementSummary?.desbloqueados)
        assertEquals(0.5f, game.achievementSummary?.porcentajeCompletado)
    }

    @Test
    fun buildAchievementBundle_countsUnlockedTrophies() {
        val game = testPlayStationGame()
        val response = PlayStationAchievementResponse(
            npCommunicationId = game.platformGameId,
            trophies = listOf(
                PlayStationAchievement(trophyId = 1, title = "A", unlocked = true),
                PlayStationAchievement(trophyId = 2, title = "B", unlocked = false),
                PlayStationAchievement(trophyId = 3, title = "C", unlocked = true)
            )
        )

        val bundle = PlayStationRepository.buildAchievementBundle(game, response)

        assertTrue(bundle.hasAchievements)
        assertEquals(3, bundle.summary?.total)
        assertEquals(2, bundle.summary?.desbloqueados)
    }

    @Test
    fun scopedPreferenceKeys_areIsolatedPerUser() {
        assertEquals(
            "linked_account::uid_123",
            PlayStationRepository.scopedPreferenceKey("linked_account", "uid_123")
        )
        assertEquals(
            listOf(
                "linked_account::uid_123",
                "cached_games::uid_123",
                "cached_trophy_games::uid_123",
                "cached_achievements::uid_123"
            ),
            PlayStationRepository.scopedPreferenceKeys("uid_123")
        )
        assertTrue(PlayStationRepository.scopedPreferenceKeys(null).isEmpty())
    }

    @Test
    fun accountDisplayName_prefersOnlineIdAndActionChangesWhenLinked() {
        val linked = PlayStationLinkedAccount(
            accountId = "123",
            onlineId = "MR.Anti-Anarquia",
            displayName = "Cuenta PlayStation",
            refreshToken = "refresh"
        )
        val fallbackLinked = linked.copy(onlineId = null, displayName = null)

        assertEquals("MR.Anti-Anarquia", PlayStationRepository.accountDisplayName(linked))
        assertEquals("Cuenta PlayStation vinculada", PlayStationRepository.accountDisplayName(fallbackLinked))
        assertEquals("No vinculada", PlayStationRepository.accountDisplayName(null))
        assertEquals("Desvincular PlayStation", PlayStationRepository.accountActionLabel(linked))
        assertEquals("Vincular", PlayStationRepository.accountActionLabel(null))
    }

    @Test
    fun profileStatusText_countsUniqueRecentAndTrophyGames() {
        val account = PlayStationLinkedAccount(
            accountId = "123",
            onlineId = "MR.Anti-Anarquia",
            refreshToken = "refresh"
        )
        val recentGame = testPlayStationGame().copy(platformGameId = "PPSA00001_00")
        val trophyGame = testPlayStationGame().copy(
            platformGameId = "NPWR20188_00",
            achievementGameId = "NPWR20188_00"
        )
        val duplicateTrophyGame = trophyGame.copy(title = "Astro Duplicate")

        assertEquals(
            "No vinculada",
            PlayStationRepository.profileStatusText(
                account = null,
                recentGames = listOf(recentGame),
                trophyGames = listOf(trophyGame)
            )
        )
        assertEquals(
            "Vinculada con 0 juegos",
            PlayStationRepository.profileStatusText(
                account = account,
                recentGames = emptyList(),
                trophyGames = emptyList()
            )
        )
        assertEquals(
            2,
            PlayStationRepository.uniqueProfileGameCount(
                recentGames = listOf(recentGame),
                trophyGames = listOf(trophyGame, duplicateTrophyGame)
            )
        )
        assertEquals(
            "Vinculada con 2 juegos",
            PlayStationRepository.profileStatusText(
                account = account,
                recentGames = listOf(recentGame),
                trophyGames = listOf(trophyGame, duplicateTrophyGame)
            )
        )
    }

    private fun testPlayStationGame(): Juego {
        return Juego(
            platform = PlataformaJuego.PSN,
            platformGameId = "NPWR20188_00",
            title = "Astro's Playroom",
            headerImageUrl = "",
            capsuleImageUrl = "",
            heroImageUrl = "",
            iconImageUrl = null
        )
    }
}
