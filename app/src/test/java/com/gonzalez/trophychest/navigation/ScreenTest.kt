package com.gonzalez.trophychest.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {
    @Test
    fun detalleTrofeosRoute_containsPlatformAndGameIdArguments() {
        assertEquals(
            "detalle_trofeos/{platform}/{gameId}",
            Screen.DetalleTrofeos.route
        )
    }
}
