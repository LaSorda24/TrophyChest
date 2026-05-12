package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.gonzalez.trophychest.data.AchievementRepository
import com.gonzalez.trophychest.data.PlatformAchievement
import com.gonzalez.trophychest.data.PlatformAchievementBundle
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.components.LoadingStateView
import com.gonzalez.trophychest.ui.components.MessageStateView
import com.gonzalez.trophychest.ui.components.RemoteUiState
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary
import com.gonzalez.trophychest.ui.theme.SubtleTrack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ColorBronce = Color(0xFFC49B7F)
val ColorPlata = Color(0xFFC0C0C0)
val ColorOro = Color(0xFFFFD700)
val ColorPlatino = Color(0xFF87CEEB)
val ColorFondoObtenido = Color(0xFFFFEB3B)
val ColorCentroOscuro = Color(0xFF1A1A1A)

@Composable
fun DetalleTrofeosScreen(
    navController: NavHostController,
    platform: PlataformaJuego,
    gameId: String
) {
    val context = LocalContext.current
    val bundleState by produceState<RemoteUiState<PlatformAchievementBundle>>(
        initialValue = RemoteUiState.Loading,
        key1 = platform,
        key2 = gameId
    ) {
        val result = AchievementRepository.getAchievementBundle(context, platform, gameId)
        value = result.fold(
            onSuccess = {
                if (!it.hasAchievements || it.achievements.isEmpty()) {
                    RemoteUiState.Empty("No hemos encontrado trofeos publicos para este juego en ${platform.displayName}.")
                } else {
                    RemoteUiState.Success(it)
                }
            },
            onFailure = { RemoteUiState.Error(it.message ?: "No se pudieron cargar los trofeos del juego.") }
        )
    }

    when (val state = bundleState) {
        RemoteUiState.Loading -> LoadingStateView("Cargando trofeos de ${platform.displayName}...")
        is RemoteUiState.Error -> MessageStateView(
            title = "Sin trofeos disponibles",
            message = state.message,
            actionLabel = "Volver"
        ) { navController.popBackStack() }
        is RemoteUiState.Empty -> MessageStateView(
            title = "Este juego no expone logros",
            message = state.message,
            actionLabel = "Volver"
        ) { navController.popBackStack() }
        is RemoteUiState.Success -> DetalleTrofeosContent(navController, state.data)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DetalleTrofeosContent(navController: NavHostController, bundle: PlatformAchievementBundle) {
    val game = when (bundle.platform) {
        PlataformaJuego.STEAM -> SteamRepository.getCachedGame(LocalContext.current, bundle.gameId)
        PlataformaJuego.PSN -> PlayStationRepository.getCachedGame(LocalContext.current, bundle.gameId)
        PlataformaJuego.GAME_PASS -> null
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = game?.headerImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp, 50.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.size(16.dp))

                        Column(
                            modifier = Modifier
                                .clickable {
                                    navController.navigate(
                                        Screen.DetalleJuego.createRoute(bundle.platform, bundle.gameId)
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = bundle.gameTitle,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bundle.platform.displayName,
                                    color = ReadableSecondary,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.size(24.dp))

                                val total = bundle.summary?.total ?: bundle.achievements.size
                                val trophyText = if (bundle.isPersonal) {
                                    "${bundle.summary?.desbloqueados ?: 0}/$total trofeos"
                                } else {
                                    "$total trofeos publicos"
                                }
                                Text(text = trophyText, color = ReadableSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            items(bundle.achievements) { achievement ->
                ItemTrofeoDetalle(achievement)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.White.copy(alpha = 0.05f)
                )
            }
        }
    }
}

@Composable
fun ItemTrofeoDetalle(trofeo: PlatformAchievement) {
    val esObtenido = trofeo.unlocked
    val colorTipo = when {
        trofeo.globalPercentage == null -> ColorBronce
        trofeo.globalPercentage < 5.0 -> ColorPlatino
        trofeo.globalPercentage < 15.0 -> ColorOro
        trofeo.globalPercentage < 35.0 -> ColorPlata
        else -> ColorBronce
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (esObtenido) ColorFondoObtenido else Color(0xFF262626))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!trofeo.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = if (esObtenido) trofeo.iconUrl else trofeo.lockedIconUrl ?: trofeo.iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (esObtenido) ColorCentroOscuro else colorTipo,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(trofeo.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                trofeo.description.ifBlank { "No hay descripcion disponible para este logro." },
                color = ReadableSecondary,
                fontSize = 13.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            if (esObtenido) {
                Text(formatUnlockDate(trofeo.unlockTime), color = ReadableTertiary, fontSize = 11.sp)
            } else {
                Icon(Icons.Default.Lock, null, tint = SubtleTrack, modifier = Modifier.size(14.dp))
            }
            Text(
                text = trofeo.globalPercentage?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "N/D",
                color = ReadableTertiary,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatUnlockDate(unlockTime: Long?): String {
    if (unlockTime == null || unlockTime <= 0) return "Sin fecha"
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(unlockTime * 1000))
}
