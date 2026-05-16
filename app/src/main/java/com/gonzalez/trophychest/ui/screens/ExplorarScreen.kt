package com.gonzalez.trophychest.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.data.IGDBExploreContent
import com.gonzalez.trophychest.data.IGDBExploreGame
import com.gonzalez.trophychest.data.IGDBRepository
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.FallbackAsyncImage
import com.gonzalez.trophychest.ui.components.RemoteUiState

private val ExploreBackground = Color(0xFF0F0F0F)
private val ExploreSectionSurface = Color(0xFF181818)
private val ExploreItemSurface = Color(0xFF222222)

// EXPLORAR PIDE RECOMENDACIONES A IGDB A TRAVES DEL PROXY.
@Composable
fun ExplorarScreen(navController: NavHostController) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val exploreState by produceState<RemoteUiState<IGDBExploreContent>>(
        initialValue = RemoteUiState.Loading,
        key1 = refreshKey
    ) {
        val result = IGDBRepository.getExploreContent(
            context = context,
            forceRefresh = refreshKey > 0
        )
        value = result.fold(
            onSuccess = { content ->
                if (content.topSellers.isEmpty() &&
                    content.recommended.isEmpty() &&
                    content.newReleases.isEmpty() &&
                    content.qualityTime.isEmpty()
                ) {
                    RemoteUiState.Empty("IGDB no ha devuelto juegos para explorar ahora mismo.")
                } else {
                    RemoteUiState.Success(content)
                }
            },
            onFailure = { RemoteUiState.Error(it.message ?: "No se pudo cargar Explorar desde IGDB.") }
        )
    }

    fun openGame(game: IGDBExploreGame) {
        navController.navigate(
            Screen.DetalleJuego.createRoute(PlataformaJuego.IGDB, game.game.platformGameId)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ExploreBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(text = "Explorar", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            when (val state = exploreState) {
                RemoteUiState.Loading -> item {
                    ExploreLoadingRow("Conectando...")
                }
                is RemoteUiState.Error -> item {
                    ExploreMessage(
                        message = state.message,
                        actionLabel = "Reintentar",
                        onAction = { refreshKey += 1 }
                    )
                }
                is RemoteUiState.Empty -> item {
                    ExploreMessage(
                        message = state.message,
                        actionLabel = "Reintentar",
                        onAction = { refreshKey += 1 }
                    )
                }
                is RemoteUiState.Success -> {
                    val content = state.data

                    item {
                        SectionHeader("LISTA DE EXITOS", topPadding = 0.dp)
                        ExplorePosterRow(
                            games = content.topSellers,
                            onGameClick = ::openGame
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(modifier = Modifier.fillMaxWidth(), color = ExploreSectionSurface) {
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                SectionHeader("RECOMENDADOS PARA TI", topPadding = 0.dp)
                                content.recommendationMessage?.let { message ->
                                    Text(
                                        text = message,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                    )
                                }
                                ExploreRecommendedRow(
                                    games = content.recommended,
                                    onGameClick = ::openGame
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader("RECIEN ANADIDOS")
                        ExploreRecentRow(
                            games = content.newReleases,
                            onGameClick = ::openGame
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(modifier = Modifier.fillMaxWidth(), color = ExploreSectionSurface) {
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                SectionHeader("INDIES DESTACADOS", topPadding = 0.dp)
                                ExploreQualityList(
                                    games = content.qualityTime,
                                    onGameClick = ::openGame
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = topPadding, bottom = 12.dp)
    )
}

@Composable
private fun ExplorePosterRow(games: List<IGDBExploreGame>, onGameClick: (IGDBExploreGame) -> Unit) {
    if (games.isEmpty()) {
        SectionInlineMessage("IGDB no ha devuelto exitos en este momento.")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .clickable { onGameClick(game) }
            ) {
                FallbackAsyncImage(
                    imageUrls = game.imageCandidates(preferPoster = true),
                    contentDescription = game.game.title,
                    modifier = Modifier
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = game.game.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ExploreRecommendedRow(games: List<IGDBExploreGame>, onGameClick: (IGDBExploreGame) -> Unit) {
    if (games.isEmpty()) {
        SectionInlineMessage("No hay recomendaciones disponibles ahora mismo.")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games) { game ->
            RecommendedBanner(game, onClick = { onGameClick(game) })
        }
    }
}

@Composable
fun RecommendedBanner(juego: IGDBExploreGame, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        FallbackAsyncImage(
            imageUrls = juego.imageCandidates(preferPoster = false),
            contentDescription = juego.game.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(text = juego.game.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = juego.subtitle ?: "Seleccion para explorar",
                color = Color.LightGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreRecentRow(games: List<IGDBExploreGame>, onGameClick: (IGDBExploreGame) -> Unit) {
    if (games.isEmpty()) {
        SectionInlineMessage("No hay novedades disponibles ahora mismo.")
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            RecentItem(game, onClick = { onGameClick(game) })
        }
    }
}

@Composable
fun RecentItem(juego: IGDBExploreGame, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        FallbackAsyncImage(
            imageUrls = juego.imageCandidates(preferPoster = false),
            contentDescription = juego.game.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
        )
        Text(
            text = juego.game.title,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreQualityList(games: List<IGDBExploreGame>, onGameClick: (IGDBExploreGame) -> Unit) {
    if (games.isEmpty()) {
        SectionInlineMessage("No hay indies destacados disponibles ahora mismo.")
        return
    }

    games.forEach { game ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onGameClick(game) },
            color = ExploreItemSurface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FallbackAsyncImage(
                    imageUrls = game.imageCandidates(preferPoster = false),
                    contentDescription = game.game.title,
                    modifier = Modifier
                        .size(70.dp, 40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = game.game.title,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreLoadingRow(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
private fun ExploreMessage(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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

@Composable
private fun SectionInlineMessage(message: String) {
    Text(
        text = message,
        color = Color.LightGray,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private fun IGDBExploreGame.imageCandidates(preferPoster: Boolean): List<String?> {
    return if (preferPoster) {
        listOf(
            game.capsuleImageUrl,
            game.headerImageUrl,
            game.heroImageUrl,
            game.iconImageUrl
        )
    } else {
        listOf(
            game.heroImageUrl,
            game.headerImageUrl,
            game.capsuleImageUrl,
            game.iconImageUrl
        )
    }
}
