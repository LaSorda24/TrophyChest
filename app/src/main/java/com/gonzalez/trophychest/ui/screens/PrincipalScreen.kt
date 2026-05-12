package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.Juego
import com.gonzalez.trophychest.data.LibraryRepository
import com.gonzalez.trophychest.data.ReleaseCalendarItem
import com.gonzalez.trophychest.data.ReleaseCalendarMapper
import com.gonzalez.trophychest.data.ReleaseCalendarRepository
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.ReleasePlatformFamily
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.FallbackAsyncImage
import com.gonzalez.trophychest.ui.components.RemoteUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanishLocale = Locale.forLanguageTag("es-ES")
private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", spanishLocale)
private val monthFormatter = DateTimeFormatter.ofPattern("MMM", spanishLocale)
private val TrophyAccent = Color(0xFFF6C453)
private val TrophySurface = Color(0xFF1A1A1A)
private val TrophySurfaceElevated = Color(0xFF242424)
private val TrophySurfaceMuted = Color(0xFF303030)

// APUNTE: PANTALLA DE INICIO; UNE JUEGOS RECIENTES Y CALENDARIO DE ESTRENOS.
@Composable
fun PrincipalScreen(navController: NavHostController) {
    val context = LocalContext.current
    val linkedAccount = SteamRepository.getLinkedAccount(context)
    val linkedPlayStationAccount = PlayStationRepository.getLinkedAccount(context)
    var calendarRefreshKey by remember { mutableStateOf(0) }

    val recentGamesState by produceState<RemoteUiState<List<Juego>>>(
        initialValue = RemoteUiState.Loading,
        key1 = linkedAccount?.steamId,
        key2 = linkedPlayStationAccount?.accountId
    ) {
        if (linkedAccount == null && linkedPlayStationAccount == null) {
            value = RemoteUiState.Empty("Vincula Steam o PlayStation desde el perfil para ver tus juegos recientes.")
            return@produceState
        }

        val result = LibraryRepository.getRecentGamesForHome(context)
        value = result.fold(
            onSuccess = { games ->
                when {
                    games.isEmpty() -> RemoteUiState.Empty(
                        "Tu biblioteca aun no esta disponible. Sincroniza Steam o PlayStation desde el perfil."
                    )
                    else -> RemoteUiState.Success(games)
                }
            },
            onFailure = { RemoteUiState.Error(it.message ?: "No se pudo cargar tu biblioteca.") }
        )
    }

    val releaseCalendarState by produceState<RemoteUiState<List<ReleaseCalendarItem>>>(
        initialValue = RemoteUiState.Loading,
        key1 = calendarRefreshKey
    ) {
        val result = ReleaseCalendarRepository.getUpcomingReleases(
            context = context,
            forceRefresh = calendarRefreshKey > 0
        )
        value = result.fold(
            onSuccess = { releases ->
                if (releases.isEmpty()) {
                    RemoteUiState.Empty("No hay estrenos proximos para este filtro.")
                } else {
                    RemoteUiState.Success(releases)
                }
            },
            onFailure = { RemoteUiState.Error(it.message ?: "No se pudo cargar el calendario de estrenos.") }
        )
    }

    PrincipalContent(
        navController = navController,
        recentGamesState = recentGamesState,
        releaseCalendarState = releaseCalendarState,
        onRetryCalendar = { calendarRefreshKey += 1 }
    )
}

@Composable
private fun PrincipalContent(
    navController: NavHostController,
    recentGamesState: RemoteUiState<List<Juego>>,
    releaseCalendarState: RemoteUiState<List<ReleaseCalendarItem>>,
    onRetryCalendar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        item {
            RecentGamesSection(
                state = recentGamesState,
                onProfileClick = { navController.navigate(Screen.Perfil.route) },
                onGameClick = { game ->
                    navController.navigate(
                        Screen.DetalleJuego.createRoute(game.platform, game.platformGameId)
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate("explorar") },
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.explorar_boton2),
                        contentDescription = "Explorar juegos",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 100f
                                )
                            )
                    )
                    Text(
                        text = "Explora nuevos juegos\npara tu proxima aventura.",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }

        item {
            ReleaseCalendarSection(
                state = releaseCalendarState,
                onRetry = onRetryCalendar
            )
        }
    }
}

@Composable
private fun RecentGamesSection(
    state: RemoteUiState<List<Juego>>,
    onProfileClick: () -> Unit,
    onGameClick: (Juego) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                text = "Jugado recientemente",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 15.dp)
            )

            when (state) {
                RemoteUiState.Loading -> CompactLoadingRow("Cargando biblioteca...")
                is RemoteUiState.Error -> RecentGamesMessage(
                    message = state.message,
                    actionLabel = "Ir al perfil",
                    onAction = onProfileClick
                )
                is RemoteUiState.Empty -> RecentGamesMessage(
                    message = state.message,
                    actionLabel = "Vincular plataformas",
                    onAction = onProfileClick
                )
                is RemoteUiState.Success -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data) { game ->
                        RecentGameCard(
                            name = game.title,
                            platform = game.platform.displayName,
                            imageUrl = game.capsuleImageUrl,
                            fallbackImageUrls = listOf(game.headerImageUrl, game.heroImageUrl),
                            onClick = { onGameClick(game) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactLoadingRow(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp
        )
        Text(text = message, color = Color.LightGray, fontSize = 13.sp)
    }
}

@Composable
private fun RecentGamesMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = message, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun ReleaseCalendarSection(
    state: RemoteUiState<List<ReleaseCalendarItem>>,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 42.dp)
        ) {
            Text(
                text = "Calendario de estrenos",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            when (state) {
                RemoteUiState.Loading -> CompactLoadingRow("Buscando proximos lanzamientos...")
                is RemoteUiState.Error -> ReleaseCalendarMessage(
                    message = state.message,
                    actionLabel = "Reintentar",
                    onAction = onRetry
                )
                is RemoteUiState.Empty -> ReleaseCalendarMessage(
                    message = state.message,
                    actionLabel = "Reintentar",
                    onAction = onRetry
                )
                is RemoteUiState.Success -> ReleaseCalendarContent(releases = state.data)
            }
        }
    }
}

@Composable
private fun ReleaseCalendarContent(releases: List<ReleaseCalendarItem>) {
    var selectedPlatform by remember { mutableStateOf<ReleasePlatformFamily?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val filteredReleases = remember(releases, selectedPlatform) {
        ReleaseCalendarMapper.filterByPlatform(releases, selectedPlatform)
    }
    val availableDates = remember(filteredReleases) {
        filteredReleases.map { it.releaseDate }.distinct().sorted()
    }

    LaunchedEffect(availableDates) {
        if (selectedDate !in availableDates) {
            selectedDate = availableDates.firstOrNull()
        }
    }

    PlatformFilterRow(
        selectedPlatform = selectedPlatform,
        onPlatformSelected = { selectedPlatform = it }
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (filteredReleases.isEmpty()) {
        ReleaseCalendarMessage(
            message = "No hay estrenos proximos para este filtro.",
            actionLabel = null,
            onAction = null
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(availableDates) { date ->
            ReleaseDateChip(
                date = date,
                selected = date == selectedDate,
                releaseCount = filteredReleases.count { it.releaseDate == date },
                onClick = { selectedDate = date }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val dayReleases = filteredReleases.filter { it.releaseDate == selectedDate }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dayReleases) { release ->
            ReleaseCalendarCard(release = release)
        }
    }
}

@Composable
private fun PlatformFilterRow(
    selectedPlatform: ReleasePlatformFamily?,
    onPlatformSelected: (ReleasePlatformFamily?) -> Unit
) {
    val filters = listOf<Pair<String, ReleasePlatformFamily?>>(
        "Todos" to null,
        "PC" to ReleasePlatformFamily.PC,
        "PlayStation" to ReleasePlatformFamily.PLAYSTATION,
        "Xbox" to ReleasePlatformFamily.XBOX
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (label, family) ->
            FilterChip(
                selected = selectedPlatform == family,
                onClick = { onPlatformSelected(family) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ReleaseDateChip(
    date: LocalDate,
    selected: Boolean,
    releaseCount: Int,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) TrophyAccent else TrophySurfaceElevated
    val textColor = if (selected) Color.Black else Color.White
    val weekDay = date.format(dayOfWeekFormatter).replace(".", "").uppercase(spanishLocale)
    val month = date.format(monthFormatter).replace(".", "").uppercase(spanishLocale)

    Surface(
        modifier = Modifier
            .width(76.dp)
            .height(86.dp)
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = weekDay, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = date.dayOfMonth.toString(), color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = month, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "$releaseCount", color = textColor.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReleaseCalendarCard(release: ReleaseCalendarItem) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .height(168.dp),
        color = TrophySurfaceElevated,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .fillMaxSize()
                    .background(TrophySurface),
                contentAlignment = Alignment.Center
            ) {
                if (release.coverUrl != null) {
                    AsyncImage(
                        model = release.coverUrl,
                        contentDescription = release.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = release.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = release.humanDate,
                        color = TrophyAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = release.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!release.summary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = release.summary,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                PlatformLabelRow(families = release.platformFamilies)
            }
        }
    }
}

@Composable
private fun PlatformLabelRow(families: Set<ReleasePlatformFamily>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        families.sortedBy { it.ordinal }.take(3).forEach { family ->
            Surface(
                color = TrophySurfaceMuted,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = family.label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ReleaseCalendarMessage(
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = message, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

fun buildPlaytimeLabel(playtimeMinutes: Int): String {
    if (playtimeMinutes <= 0) return "No jugado"
    val hours = playtimeMinutes / 60
    return if (hours > 0) "$hours h jugadas" else "$playtimeMinutes min jugados"
}

@Composable
fun RecentGameCard(
    name: String,
    platform: String,
    imageUrl: String,
    fallbackImageUrls: List<String?> = emptyList(),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FallbackAsyncImage(
                imageUrls = listOf(imageUrl) + fallbackImageUrls,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(text = platform, color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}
