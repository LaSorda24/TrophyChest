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
fun SetupNavGraph(navController: NavHostController) {//NAV CONTROLER DE MAIN
    // EL NAVHOST ES EL MAPA DE PANTALLAS; CADA COMPOSABLE ASOCIA UNA RUTA CON UNA UI.
    NavHost(
        navController = navController,
        // AQUI ARRANCO LA APP CON EL VIDEO DE PRESENTACION
        startDestination = "video_splash"
    ) {
        // PRIMERO ENSEÑO EL SPLASH Y DESPUES YA PASO AL LOGIN O AL INICIO
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
            route = Screen.DetalleJuego.route,//RUTA DEL JUEGO
            arguments = listOf(
                //DATOS QUE NECESITA EL METODO
                navArgument("platform") { type = NavType.StringType },
                navArgument("gameId") { type = NavType.StringType }
            )
        ) { backStackEntry ->//NAVEGACION ACTUAL MAS ARGUMENTOS ---------------------------------------
            // BACKSTACKENTRY CONTIENE LOS PARAMETROS QUE VIAJAN EN LA RUTA.
            val rawPlatform = backStackEntry.arguments?.getString("platform")
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            DetalleJuegosScreen(
                navController = navController,
                platform = rawPlatform.toPlatformOrDefault(),//CONVIERTE LA PLATAFORMA EN TIPO PLATAFORMA , VARIABLE ABAJO
                gameId = gameId,
                onBack = { navController.popBackStack() }//VUELVE ATRAS
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
                //NO HAY ON BACK
            )
        }

        // ESTA RUTA LLEVA A LA ZONA DE EXPLORAR JUEGOS
        composable(route = "explorar") {
            ExplorarScreen(navController = navController)
        }

        // AQUI ABRO EL CHAT CON UN AMIGO CONCRETO
        composable(
            route = "chat/{connectionId}",//REGISTRA RUTA
            arguments = listOf(
                navArgument("connectionId") { type = NavType.StringType }//ARGUMENTO ID TIPO STRING
            )
        ) { backStackEntry ->
            val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
            ChatScreen(navController, connectionId)//ABRIMOS PANTALLA
        }

        //ENSEÑO LOS JUEGOS DE UNA CATEGORIA DE IGDB
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

private fun String?.toPlatformOrDefault(): PlataformaJuego {//METODO PARA PASAR DE STRONG A PLATAFORMA
    // SI LA RUTA VIENE MAL, USO STEAM POR DEFECTO PARA EVITAR QUE LA APP CRASHEE.
    val platformName = this.orEmpty()
    return PlataformaJuego.entries.firstOrNull { it.name == platformName } ?: PlataformaJuego.STEAM//RECIBE STEAM POR DEFECTO
}
