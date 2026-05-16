package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gonzalez.trophychest.data.AchievementRepository
import com.gonzalez.trophychest.data.GameDetailsRepository
import com.gonzalez.trophychest.data.GameWishlistRepository
import com.gonzalez.trophychest.data.Juego
import com.gonzalez.trophychest.data.PegiAgeRating
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.data.PlatformAchievementBundle
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.LoadingStateView
import com.gonzalez.trophychest.ui.components.MessageStateView
import com.gonzalez.trophychest.ui.components.RemoteUiState
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary
import com.gonzalez.trophychest.ui.theme.SubtleTrack
import com.gonzalez.trophychest.ui.theme.VisibleOutline

// DETALLE JUEGO RECIBE PLATAFORMA + ID Y DECIDE QUE REPOSITORIO USAR.
@Composable
fun DetalleJuegosScreen(
    navController: NavHostController,
    platform: PlataformaJuego,
    gameId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isInUserLibrary = remember(platform, gameId) {
        when (platform) {
            PlataformaJuego.STEAM -> SteamRepository.getCachedGame(context, gameId) != null
            PlataformaJuego.PSN -> PlayStationRepository.getCachedGame(context, gameId) != null
            PlataformaJuego.GAME_PASS -> false
            else -> false
        }
    }
    val gameState by produceState<RemoteUiState<Juego>>(
        initialValue = RemoteUiState.Loading,
        key1 = platform,
        key2 = gameId
    ) {
        val result = GameDetailsRepository.getGameDetails(context, platform, gameId)
        value = result.fold(
            onSuccess = { RemoteUiState.Success(it) },
            onFailure = { RemoteUiState.Error(it.message ?: "No se pudo cargar el detalle del juego.") }
        )
    }

    when (val state = gameState) {
        RemoteUiState.Loading -> LoadingStateView("Cargando detalle del juego...")
        is RemoteUiState.Error -> MessageStateView(
            title = "Juego no disponible",
            message = state.message,
            actionLabel = "Volver"
        ) { onBack() }
        is RemoteUiState.Empty -> MessageStateView(title = "Sin datos", message = state.message)
        is RemoteUiState.Success -> DetalleJuegoContent(
            navController = navController,
            game = state.data,
            isInUserLibrary = isInUserLibrary,
            onBack = onBack
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun DetalleJuegoContent(
    navController: NavHostController,
    game: Juego,
    isInUserLibrary: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var isSaved by remember(game.platform, game.platformGameId) {
        mutableStateOf(GameWishlistRepository.isSaved(context, game))
    }
    val trophyState by produceState<RemoteUiState<PlatformAchievementBundle>>(
        initialValue = RemoteUiState.Loading,
        key1 = game.platform,
        key2 = game.platformGameId,
        key3 = game.steamAppId
    ) {
        val result = AchievementRepository.getAchievementBundleForGameDetail(context, game)
        value = result.fold(
            onSuccess = { bundle ->
                if (!bundle.hasAchievements || bundle.achievements.isEmpty()) {
                    RemoteUiState.Empty("Steam no ha devuelto logros publicos para este juego.")
                } else {
                    RemoteUiState.Success(bundle)
                }
            },
            onFailure = { throwable ->
                RemoteUiState.Empty(
                    throwable.message ?: "No hay trofeos publicos disponibles para este juego."
                )
            }
        )
    }
    val tagLabel = when {
        game.genres.isNotEmpty() -> "TAGS"
        game.supportedPlatforms.isNotEmpty() -> "PLATAFORMAS"
        else -> null
    }
    val tags = when {
        game.genres.isNotEmpty() -> game.genres.take(3)
        game.supportedPlatforms.isNotEmpty() -> game.supportedPlatforms.take(3)
        else -> emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = game.heroImageUrl.ifBlank { game.headerImageUrl },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = game.headerImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, VisibleOutline, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(game.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (tagLabel != null && tags.isNotEmpty()) {
                            Text(
                                text = tagLabel,
                                color = ReadableTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                maxLines = 2
                            ) {
                                tags.forEach { tag ->
                                    GameTag(tag)
                                }
                            }
                        }
                    }
                    if (!isInUserLibrary) {
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    isSaved = GameWishlistRepository.toggleSaved(context, game)
                                },
                            shape = CircleShape,
                            color = Color(0xFFF6C453)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = if (isSaved) "Juego guardado" else "Guardar juego",
                                tint = Color.Black,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoTecnicaItem(
                        "DESARROLLADOR",
                        null,
                        game.developers.firstOrNull() ?: game.platform.displayName,
                        true
                    )
                    PegiTecnicaItem(game.ageRating)
                    InfoTecnicaItem("LANZAMIENTO", null, game.releaseDate ?: "Sin fecha", true)
                }
            }

            if (game.screenshots.isNotEmpty()) {
                item {
                    Text(
                        "Previsualizacion",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(game.screenshots.take(6)) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SubtleTrack),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Acerca de este juego", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showSheet = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                        }
                    }
                    Text(
                        text = game.shortDescription ?: "No hay descripcion disponible para este juego.",
                        color = ReadableSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            item {
                TrophyInfoSection(
                    trophyState = trophyState,
                    modifier = Modifier.padding(16.dp),
                    onClick = { bundle ->
                        navController.navigate(
                            Screen.DetalleTrofeos.createRoute(bundle.platform, bundle.gameId)
                        )
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                ) {
                    Text("Acerca de este juego", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(game.shortDescription ?: "Sin descripcion disponible.", color = ReadableSecondary)
                    Divider(modifier = Modifier.padding(vertical = 20.dp), color = VisibleOutline)
                    InfoDetalleFila("PEGI", game.ageRating?.label ?: "Sin PEGI")
                    if (game.supportedPlatforms.isNotEmpty()) {
                        InfoDetalleFila("Sistemas", game.supportedPlatforms.joinToString(" · "))
                    }
                    if (game.platform == PlataformaJuego.STEAM || game.platform == PlataformaJuego.PSN) {
                        InfoDetalleFila("Jugado", buildPlaytimeLabel(game.playtimeMinutes))
                    }
                    InfoDetalleFila("Lanzamiento", game.releaseDate ?: "Sin fecha")
                    Button(
                        onClick = { showSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDetalleFila(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, color = ReadableSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp).weight(1f)
        )
    }
}

@Composable
fun RowScope.InfoTecnicaItem(label: String, iconRes: Int?, textValue: String?, isLarge: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(text = label, color = ReadableTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (textValue != null) {
                Text(
                    textValue,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Text("N/D", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun RowScope.PegiTecnicaItem(ageRating: PegiAgeRating?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(text = "PEGI", color = ReadableTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val imageUrl = ageRating?.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = ageRating.label,
                    modifier = Modifier.height(42.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = ageRating?.label ?: "Sin PEGI",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrophyInfoSection(
    trophyState: RemoteUiState<PlatformAchievementBundle>,
    modifier: Modifier = Modifier,
    onClick: (PlatformAchievementBundle) -> Unit
) {
    val bundle = (trophyState as? RemoteUiState.Success)?.data
    val counter = when (trophyState) {
        RemoteUiState.Loading -> "Comprobando..."
        is RemoteUiState.Success -> "${trophyState.data.summary?.total ?: trophyState.data.achievements.size} trofeos"
        is RemoteUiState.Empty -> "Sin datos"
        is RemoteUiState.Error -> "Sin datos"
    }
    val message = when {
        trophyState is RemoteUiState.Success && trophyState.data.isPersonal -> {
            "Progreso sincronizado de tu cuenta."
        }
        trophyState is RemoteUiState.Success -> {
            "Lista publica disponible desde Steam."
        }
        trophyState == RemoteUiState.Loading -> {
            "Buscando logros disponibles para este juego."
        }
        trophyState is RemoteUiState.Empty -> trophyState.message
        trophyState is RemoteUiState.Error -> trophyState.message
        else -> "No hay trofeos publicos disponibles para este juego."
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (bundle != null) Modifier.clickable { onClick(bundle) } else Modifier),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF6C453))
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Trofeos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(counter, color = Color(0xFFF6C453), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Text(message, color = ReadableSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
            if (bundle != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ReadableSecondary)
            }
        }
    }
}

@Composable
fun GameTag(text: String) {
    Surface(
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .widthIn(max = 132.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
