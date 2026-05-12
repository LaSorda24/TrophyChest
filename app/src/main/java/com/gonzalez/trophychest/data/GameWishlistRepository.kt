package com.gonzalez.trophychest.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GameWishlistRepository {
    private const val PREFS_NAME = "game_wishlist_repository_prefs"
    private const val KEY_SAVED_GAMES = "saved_games"

    private val gson = Gson()

    fun getSavedGames(context: Context): List<Juego> {
        val currentUid = currentUserUid() ?: return emptyList()
        val json = readScopedJson(context, KEY_SAVED_GAMES) ?: return emptyList()
        val type = object : TypeToken<List<Juego>>() {}.type
        val savedGames = runCatching { gson.fromJson<List<Juego>>(json, type) }.getOrDefault(emptyList())
        val visibleGames = visibleSavedGames(
            savedGames = savedGames,
            ownedSteamGameIds = ownedSteamGameIds(context)
        )

        if (visibleGames.size != savedGames.size) {
            writeScopedJson(context, currentUid, KEY_SAVED_GAMES, gson.toJson(visibleGames))
        }

        return visibleGames
    }

    fun isSaved(context: Context, game: Juego): Boolean {
        return getSavedGames(context).any { it.hasSameIdentityAs(game) }
    }

    fun toggleSaved(context: Context, game: Juego): Boolean {
        val currentUid = currentUserUid() ?: return false
        val updatedGames = toggledSavedGames(
            savedGames = getSavedGames(context),
            game = game,
            ownedSteamGameIds = ownedSteamGameIds(context)
        )

        writeScopedJson(context, currentUid, KEY_SAVED_GAMES, gson.toJson(updatedGames))
        return updatedGames.any { it.hasSameIdentityAs(game) }
    }

    internal fun toggledSavedGames(
        savedGames: List<Juego>,
        game: Juego,
        ownedSteamGameIds: Set<String>
    ): List<Juego> {
        val visibleGames = visibleSavedGames(savedGames, ownedSteamGameIds)
        if (game.isOwnedSteamGame(ownedSteamGameIds)) return visibleGames

        return if (visibleGames.any { it.hasSameIdentityAs(game) }) {
            visibleGames.filterNot { it.hasSameIdentityAs(game) }
        } else {
            listOf(game) + visibleGames
        }
    }

    internal fun visibleSavedGames(
        savedGames: List<Juego>,
        ownedSteamGameIds: Set<String>
    ): List<Juego> {
        return savedGames.filterNot { it.isOwnedSteamGame(ownedSteamGameIds) }
    }

    internal fun scopedPreferenceKey(baseKey: String, uid: String?): String? {
        val normalizedUid = uid?.takeIf { it.isNotBlank() } ?: return null
        return "$baseKey::$normalizedUid"
    }

    private fun Juego.isOwnedSteamGame(ownedSteamGameIds: Set<String>): Boolean {
        return platform == PlataformaJuego.STEAM && platformGameId in ownedSteamGameIds
    }

    private fun Juego.hasSameIdentityAs(other: Juego): Boolean {
        return platform == other.platform && platformGameId == other.platformGameId
    }

    private fun ownedSteamGameIds(context: Context): Set<String> {
        return SteamRepository.getCachedGames(context)
            .map { it.platformGameId }
            .toSet()
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun currentUserUid(): String? {
        return FirebaseManager.currentUser?.uid?.takeIf { it.isNotBlank() }
    }

    private fun readScopedJson(context: Context, baseKey: String): String? {
        val currentUid = currentUserUid() ?: return null
        val key = scopedPreferenceKey(baseKey, currentUid) ?: return null
        return prefs(context).getString(key, null)
    }

    private fun writeScopedJson(context: Context, uid: String, baseKey: String, value: String) {
        val key = scopedPreferenceKey(baseKey, uid) ?: return
        prefs(context).edit().putString(key, value).apply()
    }
}
