package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GameDetailsRepositoryTest {
    @Test
    fun resolveSource_mapsSupportedPlatformsToTheirRepository() {
        assertEquals(GameDetailSource.STEAM, GameDetailsRepository.resolveSource(PlataformaJuego.STEAM))
        assertEquals(GameDetailSource.PLAYSTATION, GameDetailsRepository.resolveSource(PlataformaJuego.PSN))
        assertEquals(GameDetailSource.UNSUPPORTED, GameDetailsRepository.resolveSource(PlataformaJuego.GAME_PASS))
        assertEquals(GameDetailSource.IGDB, GameDetailsRepository.resolveSource(PlataformaJuego.IGDB))
    }

}
