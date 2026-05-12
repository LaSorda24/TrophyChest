package com.gonzalez.trophychest.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.ui.screens.AmigosScreen
import com.gonzalez.trophychest.ui.screens.BusquedaScreen
import com.gonzalez.trophychest.ui.screens.CategoriaScreen
import com.gonzalez.trophychest.ui.screens.ChatScreen
import com.gonzalez.trophychest.ui.screens.DetalleJuegosScreen
import com.gonzalez.trophychest.ui.screens.DetalleTrofeosScreen
import com.gonzalez.trophychest.ui.screens.ExplorarScreen
import com.gonzalez.trophychest.ui.screens.HelpCenterScreen
import com.gonzalez.trophychest.ui.screens.LoginScreen
import com.gonzalez.trophychest.ui.screens.PerfilScreen
import com.gonzalez.trophychest.ui.screens.PrincipalScreen
import com.gonzalez.trophychest.ui.screens.ProfileSettingsScreen
import com.gonzalez.trophychest.ui.screens.TrofeosScreen
import com.gonzalez.trophychest.ui.screens.VideoSplashScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        // AQUI ARRANCO LA APP CON EL VIDEO DE PRESENTACION
        startDestination = "video_splash"
    ) {
        // PRIMERO ENSENO EL SPLASH Y DESPUES YA PASO AL LOGIN O AL INICIO
        composable(route = "video_splash") {
            VideoSplashScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // ESTAS SON LAS PANTALLAS PRINCIPALES QUE APARECEN EN EL MENU INFERIOR
        composable(route = Screen.Principal.route) {
            PrincipalScreen(navController = navController)
        }

        composable(route = Screen.Mensajes.route) {
            AmigosScreen(navController = navController)
        }

        composable(route = Screen.Busqueda.route) {
            BusquedaScreen(navController = navController)
        }

        composable(route = Screen.Trofeos.route) {
            TrofeosScreen(navController = navController)
        }

        composable(route = Screen.Perfil.route) {
            PerfilScreen(navController = navController)
        }

        composable(route = Screen.ProfileSettings.route) {
            ProfileSettingsScreen(navController = navController)
        }

        composable(route = Screen.HelpCenter.route) {
            HelpCenterScreen(navController = navController)
        }

        // AQUI ABRO LA FICHA COMPLETA DE UN JUEGO
        composable(
            route = Screen.DetalleJuego.route,
            arguments = listOf(
                navArgument("platform") { type = NavType.StringType },
                navArgument("gameId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPlatform = backStackEntry.arguments?.getString("platform")
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            DetalleJuegosScreen(
                navController = navController,
                platform = rawPlatform.toPlatformOrDefault(),
                gameId = gameId,
                onBack = { navController.popBackStack() }
            )
        }

        // AQUI ENSENO LOS TROFEOS DE UN JUEGO CONCRETO
        composable(
            route = Screen.DetalleTrofeos.route,
            arguments = listOf(
                navArgument("platform") { type = NavType.StringType },
                navArgument("gameId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPlatform = backStackEntry.arguments?.getString("platform")
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            DetalleTrofeosScreen(
                navController = navController,
                platform = rawPlatform.toPlatformOrDefault(),
                gameId = gameId
            )
        }

        // ESTA RUTA LLEVA A LA ZONA DE EXPLORAR JUEGOS
        composable(route = "explorar") {
            ExplorarScreen(navController = navController)
        }

        // AQUI ABRO EL CHAT CON UN AMIGO CONCRETO
        composable(
            route = "chat/{connectionId}",
            arguments = listOf(
                navArgument("connectionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
            ChatScreen(navController, connectionId)
        }

        // AQUI ENSENO LOS JUEGOS DE UNA CATEGORIA DE IGDB
        composable(
            route = "categoria/{genero}",
            arguments = listOf(
                navArgument("genero") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val genero = backStackEntry.arguments?.getString("genero") ?: ""
            CategoriaScreen(navController, genero)
        }
    }
}

private fun String?.toPlatformOrDefault(): PlataformaJuego {
    val platformName = this.orEmpty()
    return PlataformaJuego.entries.firstOrNull { it.name == platformName } ?: PlataformaJuego.STEAM
}
