package com.gonzalez.trophychest.data

import android.content.Context

enum class GameDetailSource {
    STEAM,
    PLAYSTATION,
    IGDB,
    UNSUPPORTED
}

object GameDetailsRepository {
    suspend fun getGameDetails(
        context: Context,
        platform: PlataformaJuego,
        gameId: String
    ): Result<Juego> {
        return when (resolveSource(platform)) {
            GameDetailSource.STEAM -> SteamRepository.getGameDetails(context, gameId)
            GameDetailSource.PLAYSTATION -> PlayStationRepository.getGameDetails(context, gameId)
            GameDetailSource.IGDB -> IGDBRepository.getGameDetails(gameId)
            GameDetailSource.UNSUPPORTED -> Result.failure(unsupportedPlatformError(platform))
        }
    }

    internal fun resolveSource(platform: PlataformaJuego): GameDetailSource {
        return when (platform) {
            PlataformaJuego.STEAM -> GameDetailSource.STEAM
            PlataformaJuego.PSN -> GameDetailSource.PLAYSTATION
            PlataformaJuego.GAME_PASS -> GameDetailSource.UNSUPPORTED
            PlataformaJuego.IGDB -> GameDetailSource.IGDB
        }
    }

    //PARA GAMEPASS
    private fun unsupportedPlatformError(platform: PlataformaJuego): IllegalArgumentException {
        val message = if (platform == PlataformaJuego.GAME_PASS) {
            GAME_PASS_UNAVAILABLE_MESSAGE
        } else {
            "Aun no hay soporte de detalle para ${platform.displayName}."
        }
        return IllegalArgumentException(message)
    }
}
