package com.gonzalez.trophychest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gonzalez.trophychest.data.IGDBGame
import com.gonzalez.trophychest.data.IGDBRepository
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.navigation.SetupNavGraph
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.BottomNavigationBar
import com.gonzalez.trophychest.ui.components.RemoteUiState
import com.gonzalez.trophychest.ui.components.TopHeader
import com.gonzalez.trophychest.ui.theme.TrophyChestTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TrophyChestTheme {
                MiApp()
            }
        }
    }
}

// APUNTE: ESTA ES LA ESTRUCTURA GENERAL DE LA APP: CABECERA, MENU INFERIOR Y NAVEGACION.
@Composable
fun MiApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val esVideo = currentRoute == "video_splash"
    val esLogin = currentRoute == "login"
    val esExplorar = currentRoute == "explorar"
    val esChat = currentRoute?.startsWith("chat") == true
    val esCategoria = currentRoute?.startsWith("categoria") == true
    val esTrofeos = currentRoute?.startsWith("detalle_trofeos") == true
    val esDetalleJuego = currentRoute?.startsWith("detalle_juego") == true
    val esHelpCenter = currentRoute == Screen.HelpCenter.route

    val mostrarMenuInferior = !esVideo && !esLogin && !esExplorar && !esChat && !esCategoria &&
        !esTrofeos && !esDetalleJuego && !esHelpCenter

    fun clearSearch() {
        searchQuery = ""
    }

    LaunchedEffect(esVideo, esLogin, esHelpCenter) {
        if (esVideo || esLogin || esHelpCenter) {
            clearSearch()
        }
    }

    val trimmedSearchQuery = searchQuery.trim()
    val searchResultsState by produceState<RemoteUiState<List<IGDBGame>>>(
        initialValue = RemoteUiState.Empty(""),
        key1 = trimmedSearchQuery
    ) {
        if (trimmedSearchQuery.isBlank()) {
            value = RemoteUiState.Empty("")
            return@produceState
        }

        value = RemoteUiState.Loading
        delay(350)

        val result = IGDBRepository.searchGames(trimmedSearchQuery)
        value = result.fold(
            onSuccess = { games ->
                if (games.isEmpty()) {
                    RemoteUiState.Empty("No se encuentran resultados.")
                } else {
                    RemoteUiState.Success(games)
                }
            },
            onFailure = { throwable ->
                RemoteUiState.Error(throwable.message ?: "No se pudo completar la busqueda.")
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F0F0F),
        topBar = {
            if (!esVideo && !esLogin && !esHelpCenter) {
                TopHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onClearSearch = { clearSearch() },
                    searchResultsState = searchResultsState,
                    onGameClick = { game ->
                        clearSearch()
                        navController.navigate(
                            Screen.DetalleJuego.createRoute(
                                platform = PlataformaJuego.IGDB,
                                gameId = game.id.toString()
                            )
                        )
                    },
                    onHelpClick = {
                        clearSearch()
                        navController.navigate(Screen.HelpCenter.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = mostrarMenuInferior,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        val paddingAjustado = when {
            esVideo || esLogin -> PaddingValues(0.dp)
            esExplorar || esChat || esCategoria || esDetalleJuego || esTrofeos || esHelpCenter -> PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 0.dp
            )
            else -> innerPadding
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingAjustado)
        ) {
            SetupNavGraph(navController = navController)
        }
    }
}
