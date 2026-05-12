package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamRepositoryStorageTest {
    @Test
    fun scopedPreferenceKey_buildsKeysPerUserUid() {
        val key = SteamRepository.scopedPreferenceKey(
            baseKey = "linked_account",
            uid = "uid_123"
        )

        assertEquals("linked_account::uid_123", key)
    }

    @Test
    fun scopedPreferenceKey_returnsNullWithoutAuthenticatedUser() {
        val key = SteamRepository.scopedPreferenceKey(
            baseKey = "linked_account",
            uid = null
        )

        assertNull(key)
    }

    @Test
    fun scopedPreferenceKeys_returnsEmptyListWithoutAuthenticatedUser() {
        val keys = SteamRepository.scopedPreferenceKeys(uid = null)

        assertTrue(keys.isEmpty())
    }

    @Test
    fun scopedPreferenceKeys_includesAllSteamStorageBucketsForUser() {
        val keys = SteamRepository.scopedPreferenceKeys(uid = "player_a")

        assertEquals(
            listOf(
                "linked_account::player_a",
                "cached_games::player_a",
                "cached_details_v3::player_a",
                "cached_achievements::player_a",
                "cached_achievements_v2::player_a",
                "cached_explore::player_a",
                "cached_categories::player_a"
            ),
            keys
        )
    }

    @Test
    fun scopedPreferenceKeys_includesLegacyAndCurrentAchievementCacheBuckets() {
        val keys = SteamRepository.scopedPreferenceKeys(uid = "player_a")

        assertTrue("cached_achievements::player_a" in keys)
        assertTrue("cached_achievements_v2::player_a" in keys)
    }

}
