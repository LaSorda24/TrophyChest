package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gonzalez.trophychest.data.AchievementFilters
import com.gonzalez.trophychest.data.AchievementRepository
import com.gonzalez.trophychest.data.Juego
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.LoadingStateView
import com.gonzalez.trophychest.ui.components.MessageStateView
import com.gonzalez.trophychest.ui.components.RemoteUiState
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary
import com.gonzalez.trophychest.ui.theme.VisibleOutline

// TROFEOS JUNTA LOGROS DE STEAM Y PLAYSTATION Y PERMITE FILTRARLOS.
@Composable
fun TrofeosScreen(navController: NavHostController) {
    val context = LocalContext.current
    val linkedAccount = SteamRepository.getLinkedAccount(context)
    val linkedPlayStationAccount = PlayStationRepository.getLinkedAccount(context)
    // PRODUCESTATE CARGA LOS JUEGOS CON LOGROS CUANDO CAMBIAN LAS CUENTAS VINCULADAS.
    val achievementState by produceState<RemoteUiState<List<Juego>>>(
        initialValue = RemoteUiState.Loading,
        key1 = linkedAccount?.steamId,
        key2 = linkedPlayStationAccount?.accountId
    ) {
        if (linkedAccount == null && linkedPlayStationAccount == null) {
            value = RemoteUiState.Empty("Vincula Steam o PlayStation desde el perfil para importar logros.")
            return@produceState
        }

        val result = AchievementRepository.getGamesWithAchievementSummaries(context)
        value = result.fold(
            onSuccess = { games -> RemoteUiState.Success(games) },
            onFailure = {
                RemoteUiState.Error(it.message ?: "No se pudieron cargar los trofeos.")
            }
        )
    }

    if (linkedAccount == null && linkedPlayStationAccount == null) {
        MessageStateView(
            title = "Sin trofeos disponibles",
            message = "Vincula Steam o PlayStation desde el perfil para importar logros."
        )
    } else {
        TrofeosContent(
            navController = navController,
            achievementState = achievementState
        )
    }
}

@Composable
private fun TrofeosContent(
    navController: NavHostController,
    achievementState: RemoteUiState<List<Juego>>
) {
    // ESTADO LOCAL DE FILTROS; AL CAMBIARLO, SE RECALCULA LA LISTA SIN VOLVER A PEDIR DATOS.
    var mostrarMenuFiltros by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var porcentajeAplicado by remember { mutableFloatStateOf(0f) }
    var plataformasSeleccionadas by remember {
        mutableStateOf(AchievementFilters.trophyPlatforms.toSet())
    }
    val juegosBase = (achievementState as? RemoteUiState.Success)?.data.orEmpty()

    val juegosFiltrados = remember(juegosBase, porcentajeAplicado, plataformasSeleccionadas) {
        // REMEMBER EVITA RECALCULAR FILTROS SI NO CAMBIAN JUEGOS, PORCENTAJE O PLATAFORMAS.
        AchievementFilters.filterGames(
            games = juegosBase,
            minCompletion = porcentajeAplicado,
            selectedPlatforms = plataformasSeleccionadas
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis Trofeos",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { mostrarMenuFiltros = !mostrarMenuFiltros },
                    modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = if (porcentajeAplicado > 0f) Color.Yellow else Color.White
                    )
                }
            }

            AchievementListContent(
                navController = navController,
                achievementState = achievementState,
                juegosFiltrados = juegosFiltrados
            )
        }

        if (mostrarMenuFiltros) {
            TrophyFiltersModal(
                sliderValue = sliderValue,
                onSliderValueChange = { sliderValue = it },
                plataformasSeleccionadas = plataformasSeleccionadas,
                onPlatformCheckedChange = { platform, checked ->
                    plataformasSeleccionadas = if (checked) {
                        plataformasSeleccionadas + platform
                    } else {
                        plataformasSeleccionadas - platform
                    }
                },
                onDismiss = { mostrarMenuFiltros = false },
                onApply = {
                    porcentajeAplicado = sliderValue
                    mostrarMenuFiltros = false
                }
            )
        }
    }
}

@Composable
private fun TrophyFiltersModal(
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    plataformasSeleccionadas: Set<PlataformaJuego>,
    onPlatformCheckedChange: (PlataformaJuego, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        val modalMaxHeight = maxHeight - 48.dp
        val cardInteractionSource = remember { MutableInteractionSource() }

        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(max = modalMaxHeight)
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = null
                ) { },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(10.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Progreso minimo", color = ReadableTertiary, fontSize = 12.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = sliderValue,
                            onValueChange = onSliderValueChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White
                            )
                        )
                        Text(
                            text = "${(sliderValue * 100).toInt()}%",
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Plataformas", color = ReadableTertiary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    AchievementFilters.trophyPlatforms.forEach { platform ->
                        PlatformCheckboxRow(
                            platform = platform,
                            checked = platform in plataformasSeleccionadas,
                            onCheckedChange = { checked ->
                                onPlatformCheckedChange(platform, checked)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aplicar filtros", color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun AchievementListContent(
    navController: NavHostController,
    achievementState: RemoteUiState<List<Juego>>,
    juegosFiltrados: List<Juego>
) {
    when (achievementState) {
        RemoteUiState.Loading -> LoadingStateView("Sincronizando logros...")
        is RemoteUiState.Error -> MessageStateView(
            title = "Error cargando trofeos",
            message = achievementState.message
        )
        is RemoteUiState.Empty -> MessageStateView(
            title = "Sin trofeos disponibles",
            message = achievementState.message
        )
        is RemoteUiState.Success -> {
            if (juegosFiltrados.isEmpty()) {
                MessageStateView(
                    title = "Sin logros disponibles",
                    message = "No hay juegos con logros que coincidan con los filtros actuales."
                )
                return
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(juegosFiltrados) { juego ->
                    ItemJuegoTrofeo(juego) {
                        navController.navigate(
                            Screen.DetalleTrofeos.createRoute(juego.platform, juego.platformGameId)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCheckboxRow(
    platform: PlataformaJuego,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = VisibleOutline,
                checkmarkColor = Color.Black
            )
        )
        Text(
            text = platform.displayName,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ItemJuegoTrofeo(juego: Juego, onClick: () -> Unit) {
    val summary = juego.achievementSummary
    val progress = summary?.porcentajeCompletado ?: 0f

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = juego.headerImageUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp, 56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(juego.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = juego.platform.displayName,
                    color = Color(0xFF66C0F4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (summary == null) {
                        "Sin logros disponibles"
                    } else {
                        "${summary.desbloqueados}/${summary.total} trofeos"
                    },
                    color = ReadableSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 1f) Color.Yellow else Color.White,
                    trackColor = Color(0xFF333333)
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                color = ReadableSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
