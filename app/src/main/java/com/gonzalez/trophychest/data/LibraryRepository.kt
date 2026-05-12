package com.gonzalez.trophychest.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// APUNTE: AQUI MEZCLO BIBLIOTECAS DE STEAM Y PLAYSTATION PARA ENSENARLAS COMO UNA SOLA LISTA.
object LibraryRepository {
    suspend fun getRecentGamesForHome(context: Context): Result<List<Juego>> = withContext(Dispatchers.IO) {
        val steamGames = if (SteamRepository.getLinkedAccount(context) != null) {
            SteamRepository.getGamesForHome(context).getOrElse { throwable ->
                return@withContext Result.failure(throwable)
            }
        } else {
            emptyList()
        }

        val playStationGames = if (PlayStationRepository.getLinkedAccount(context) != null) {
            PlayStationRepository.getGamesForHome(context).getOrElse { throwable ->
                return@withContext Result.failure(throwable)
            }
        } else {
            emptyList()
        }

        Result.success(selectRecentGamesForHome(steamGames + playStationGames))
    }

    internal fun sortRecentGamesForHome(games: List<Juego>): List<Juego> {
        return games.sortedWith(
            compareByDescending<Juego> { it.lastPlayedEpochSeconds ?: 0L }
                .thenByDescending { it.playtimeMinutes }
                .thenBy { it.platform.displayName }
                .thenBy { it.title }
        )
    }

    internal fun selectRecentGamesForHome(games: List<Juego>, limit: Int = 10): List<Juego> {
        if (limit <= 0 || games.isEmpty()) return emptyList()

        val gamesByPlatform = games
            .groupBy { it.platform }
            .mapValues { (_, platformGames) -> sortRecentGamesForHome(platformGames) }
            .filterValues { it.isNotEmpty() }

        if (gamesByPlatform.size <= 1) {
            return sortRecentGamesForHome(games).take(limit)
        }

        val nextIndexes = gamesByPlatform.keys.associateWith { 0 }.toMutableMap()
        val selectedGames = mutableListOf<Juego>()

        while (selectedGames.size < limit) {
            val platformsForRound = gamesByPlatform.keys
                .filter { platform ->
                    val nextIndex = nextIndexes.getValue(platform)
                    nextIndex < gamesByPlatform.getValue(platform).size
                }
                .sortedWith(
                    compareByDescending<PlataformaJuego> { platform ->
                        gamesByPlatform.getValue(platform)[nextIndexes.getValue(platform)].lastPlayedEpochSeconds ?: 0L
                    }
                        .thenByDescending { platform ->
                            gamesByPlatform.getValue(platform)[nextIndexes.getValue(platform)].playtimeMinutes
                        }
                        .thenBy { it.displayName }
                )

            if (platformsForRound.isEmpty()) break

            platformsForRound.forEach { platform ->
                if (selectedGames.size >= limit) return@forEach

                val nextIndex = nextIndexes.getValue(platform)
                selectedGames += gamesByPlatform.getValue(platform)[nextIndex]
                nextIndexes[platform] = nextIndex + 1
            }
        }

        return selectedGames
    }
}
