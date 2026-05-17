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

// CABECERA, MENU INFERIOR Y NAVEGACION.
@Composable
fun MiApp() {
    // ESTA FUNCION HACE DE "RAIZ" DE LA UI. NO ES VIEWMODEL; SOLO COORDINA ESTADO GLOBAL DE PANTALLA.
    val navController = rememberNavController()//CONTROLLER PARA LA NAVEGACION
    //VARIABLE PARA SABER LA PANTALLA ACTUAL
    val navBackStackEntry by navController.currentBackStackEntryAsState()//REVISA LA PANTALLA ACTUAL
    val currentRoute = navBackStackEntry?.destination?.route//GUARDA LA RUTA EN TEXTO
    //VARIABLE PARA EL TEXTO DE BUSQUEDA
    var searchQuery by rememberSaveable { mutableStateOf("") }//GUARDA CON SAVEABLE PARA SOBREVIVIR A CAMBIOS

    //VARIABLES PARA DETECTAR PANTALLAS
    val esVideo = currentRoute == "video_splash"
    val esLogin = currentRoute == "login"
    val esExplorar = currentRoute == "explorar"
    val esChat = currentRoute?.startsWith("chat") == true
    val esCategoria = currentRoute?.startsWith("categoria") == true
    val esTrofeos = currentRoute?.startsWith("detalle_trofeos") == true
    val esDetalleJuego = currentRoute?.startsWith("detalle_juego") == true
    val esHelpCenter = currentRoute == Screen.HelpCenter.route

    //MOSTRAR EL MENU INFERIOR
    val mostrarMenuInferior = !esVideo && !esLogin && !esExplorar && !esChat && !esCategoria &&
        !esTrofeos && !esDetalleJuego && !esHelpCenter

    //FUNCION PARA LIMPIAR LA BUSQUEDA
    fun clearSearch() {
        searchQuery = ""
    }
    //DECIDE CUANDO SE LIMPIA LA BUSQUEDA
    LaunchedEffect(esVideo, esLogin, esHelpCenter) {
        if (esVideo || esLogin || esHelpCenter) {
            clearSearch()
        }
    }
    //QUITA ESPACIOS PARA LA BUSQUEDA
    val trimmedSearchQuery = searchQuery.trim()

    //CREA UN ESTADO PARA ACTUALIZAR LOS RESULTADOS
    // PRODUCESTATE LANZA UNA CORRUTINA Y CONVIERTE EL RESULTADO DE IGDB EN ESTADO PARA REDIBUJAR LA UI.
    val searchResultsState by produceState<RemoteUiState<List<IGDBGame>>>(
        initialValue = RemoteUiState.Empty(""),//VLOR INICIAL VACIO
        key1 = trimmedSearchQuery//CADA VEZ QUE ESCRIBIMOS SE EJECUTA DE NUEVO
    ) {
        //SI LA LISTA ESTA VACIA NO LLAMA A LA BASE DE DATOS
        if (trimmedSearchQuery.isBlank()) {
            value = RemoteUiState.Empty("")
            return@produceState
        }

        value = RemoteUiState.Loading//CARGANDO
        delay(350)//TIEMPO DE ESPERA

        //LLAMAMOS A IGDB PARA BUSCAR JUEGOS
        val result = IGDBRepository.searchGames(trimmedSearchQuery)
        value = result.fold(//LO TRANSFORMA EN INTERFAZ
            onSuccess = { games ->
                if (games.isEmpty()) {
                    RemoteUiState.Empty("No se encuentran resultados.")//SI ESTA VACIA MUSTRA MENSAJE
                } else {
                    RemoteUiState.Success(games)//SI HAY RESULTADOS DEVUELVE LA LISTA
                }
            },
            onFailure = { throwable ->
                RemoteUiState.Error(throwable.message ?: "No se pudo completar la busqueda.")
                //SI LA BUSQUEDA TIENE ALGUN FALLO DEVUELVE UN MENSAJE
            }
        )
    }

    Scaffold(
        // SCAFFOLD ES LA ESTRUCTURA VISUAL: ARRIBA CABECERA, ABAJO MENU, EN MEDIO NAVGRAPH.
        containerColor = Color(0xFF0F0F0F),
        topBar = {
            if (!esVideo && !esLogin && !esHelpCenter) {//MUESTRA LA CABECERA CUANDO NO ESTAMOS EN ...
                TopHeader(
                    //CONECTAMOS LA CABECERA CON EL CONTROLLER
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onClearSearch = { clearSearch() },
                    searchResultsState = searchResultsState,//COGEMOS LOS RESULTADOS
                    onGameClick = { game ->//SI SE DA CLICK EN UN JUEGO...
                        clearSearch()
                        navController.navigate(
                            Screen.DetalleJuego.createRoute(//SE LLAMA AL METODO DE SCREEN PARA CREAR RUTA
                                platform = PlataformaJuego.IGDB,
                                gameId = game.id.toString()
                                //CREA LA RUTA CON LA PLANTILLA DETALE Y LOS DATOS DEL JUEGO
                            )
                        )
                    },
                    //AL PULSAR AYUDA....
                    onHelpClick = {
                        clearSearch()
                        navController.navigate(Screen.HelpCenter.route) {
                            launchSingleTop = true//EVITAMOS ABRIR VARIAS AYUDAS
                        }
                    }
                )
            }
        },

        //MENU INFERIOR
        bottomBar = {
            AnimatedVisibility(
                visible = mostrarMenuInferior,//ESTADO PARA MOSTRAR
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })//ANIMACION PARA SALIR
            ) {
                BottomNavigationBar(navController = navController)//MOSTRAMOS EL MENU
            }
        }
        //INDICAMOS EL TAMAÑO SEGUN SITUACION
    ) { innerPadding ->
        val paddingAjustado = when {
            esVideo || esLogin -> PaddingValues(0.dp)//SIN MENUS
            esExplorar || esChat || esCategoria || esDetalleJuego || esTrofeos || esHelpCenter -> PaddingValues(//SOLO SUPEIOR
                top = innerPadding.calculateTopPadding(),
                bottom = 0.dp
            )
            else -> innerPadding
        }

        //RESTO DE PANTALLAS
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingAjustado)
        ) {
            SetupNavGraph(navController = navController)
            //CARGAMOS NAVEGACION PARA SABER EN QUE PANTALLA ESTAMOS
        }
    }
}
