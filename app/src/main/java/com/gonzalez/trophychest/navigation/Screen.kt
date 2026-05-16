package com.gonzalez.trophychest.navigation

import android.net.Uri
import com.gonzalez.trophychest.data.PlataformaJuego

// AQUI GUARDO LAS RUTAS QUE USA LA APP PARA MOVERSE ENTRE PANTALLAS
// SEALED CLASS ME PERMITE TENER RUTAS CONTROLADAS EN UN SOLO SITIO EN VEZ DE TEXTOS SUELTOS.
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
        fun createRoute(platform: PlataformaJuego, gameId: String): String {//CREAMOS RUTAS PARA JUGOS
            // URI.ENCODE EVITA PROBLEMAS SI EL ID TRAE CARACTERES RAROS EN LA URL DE NAVEGACION.
            return "detalle_juego/${platform.name}/${Uri.encode(gameId)}"
        }
    }
    object DetalleTrofeos : Screen("detalle_trofeos/{platform}/{gameId}") {//OBJETO PARA RUTA DE TROFEOS
        fun createRoute(platform: PlataformaJuego, gameId: String): String {//CREAMOS RUTA DE TROFEOS
            return "detalle_trofeos/${platform.name}/${Uri.encode(gameId)}"
        }
    }
}
