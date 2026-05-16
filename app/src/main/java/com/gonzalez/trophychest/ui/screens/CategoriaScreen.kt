package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.gonzalez.trophychest.data.IGDBCategoryContent
import com.gonzalez.trophychest.data.IGDBRepository
import com.gonzalez.trophychest.data.Juego
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.FallbackAsyncImage
import com.gonzalez.trophychest.ui.components.RemoteUiState
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary

// CATEGORIA USA EL SLUG DE NAVEGACION PARA PEDIR A IGDB JUEGOS DE UN GENERO.
@Composable
fun CategoriaScreen(navController: NavHostController, genero: String) {
    val context = LocalContext.current
    val categoryTitle = remember(genero) {
        IGDBRepository.categoryForSlug(genero)?.displayName ?: genero.uppercase()
    }
    var refreshKey by remember { mutableIntStateOf(0) }

    val categoryState by produceState<RemoteUiState<IGDBCategoryContent>>(
        initialValue = RemoteUiState.Loading,
        key1 = genero,
        key2 = refreshKey
    ) {
        val result = IGDBRepository.getCategoryGames(context, genero)
        value = result.fold(
            onSuccess = { content ->
                if (content.games.isEmpty()) {
                    RemoteUiState.Empty("IGDB/Twitch no ha devuelto juegos para esta categoria.")
                } else {
                    RemoteUiState.Success(content)
                }
            },
            onFailure = { throwable ->
                RemoteUiState.Error(throwable.message ?: "No se pudo cargar esta categoria desde IGDB/Twitch.")
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF0F0F0F)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text(
                        text = categoryTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = categoryState) {
                RemoteUiState.Loading -> CategoryLoading()
                is RemoteUiState.Error -> CategoryMessage(
                    message = state.message,
                    actionLabel = "Reintentar",
                    onAction = { refreshKey += 1 }
                )
                is RemoteUiState.Empty -> CategoryMessage(
                    message = state.message,
                    actionLabel = "Reintentar",
                    onAction = { refreshKey += 1 }
                )
                is RemoteUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.data.games) { juego ->
                            CategoryGameItem(juego) {
                                navController.navigate(
                                    Screen.DetalleJuego.createRoute(PlataformaJuego.IGDB, juego.platformGameId)
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
private fun CategoryGameItem(juego: Juego, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FallbackAsyncImage(
                imageUrls = listOf(
                    juego.capsuleImageUrl,
                    juego.headerImageUrl,
                    juego.heroImageUrl,
                    juego.iconImageUrl
                ),
                contentDescription = juego.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 250f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = juego.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = platformLabel(juego.supportedPlatforms),
                    color = ReadableTertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.White)
        Text(
            text = "Cargando...",
            color = ReadableSecondary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun CategoryMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = ReadableSecondary,
            lineHeight = 20.sp
        )
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        ) {
            Text(actionLabel)
        }
    }
}

private fun platformLabel(platforms: List<String>): String {
    val labels = platforms
        .map(::shortPlatformName)
        .distinct()
        .take(3)
    return labels.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "IGDB"
}

private fun shortPlatformName(platform: String): String {
    return when {
        platform.contains("PC", ignoreCase = true) -> "PC"
        platform.contains("PlayStation 5", ignoreCase = true) -> "PS5"
        platform.contains("PlayStation 4", ignoreCase = true) -> "PS4"
        platform.contains("Xbox Series", ignoreCase = true) -> "Xbox Series"
        platform.contains("Xbox One", ignoreCase = true) -> "Xbox One"
        else -> platform
    }
}
