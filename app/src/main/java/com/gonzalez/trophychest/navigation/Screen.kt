package com.gonzalez.trophychest.navigation

import android.net.Uri
import com.gonzalez.trophychest.data.PlataformaJuego

// AQUI GUARDO LAS RUTAS QUE USA LA APP PARA MOVERSE ENTRE PANTALLAS
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Principal : Screen("principal")
    object Mensajes : Screen("mensajes")
    object Busqueda : Screen("busqueda")
    object Trofeos : Screen("trofeos")
    object Perfil : Screen("perfil")
    object ProfileSettings : Screen("profile_settings")
    object HelpCenter : Screen("help_center")
    object DetalleJuego : Screen("detalle_juego/{platform}/{gameId}") {
        fun createRoute(platform: PlataformaJuego, gameId: String): String {
            return "detalle_juego/${platform.name}/${Uri.encode(gameId)}"
        }
    }
    object DetalleTrofeos : Screen("detalle_trofeos/{platform}/{gameId}") {
        fun createRoute(platform: PlataformaJuego, gameId: String): String {
            return "detalle_trofeos/${platform.name}/${Uri.encode(gameId)}"
        }
    }
}
