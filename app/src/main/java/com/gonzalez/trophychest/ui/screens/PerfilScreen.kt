package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.ChatRepository
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.data.FriendConnection
import com.gonzalez.trophychest.data.GameWishlistRepository
import com.gonzalez.trophychest.data.Juego
import com.gonzalez.trophychest.data.PlataformaJuego
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.ProfileStatsCalculator
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.data.User
import com.gonzalez.trophychest.data.UserRepository
import com.gonzalez.trophychest.navigation.Screen
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.SubtleTrack
import com.gonzalez.trophychest.ui.theme.VisibleOutline
import kotlinx.coroutines.launch

// APUNTE: PERFIL ENSENA DATOS DEL USUARIO, AMIGOS, JUEGOS GUARDADOS Y RESUMEN DE PROGRESO.
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PerfilScreen(navController: NavHostController) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUser = FirebaseManager.currentUser
    var profile by remember { mutableStateOf<User?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var friendConnections by remember(currentUser?.uid) { mutableStateOf(emptyList<FriendConnection>()) }

    val cachedGames = SteamRepository.getCachedGames(context)
    val cachedPlayStationGames = PlayStationRepository.getCachedGames(context)
    val cachedPlayStationTrophyGames = PlayStationRepository.getCachedTrophyGames(context)
    var savedGames by remember(currentUser?.uid) {
        mutableStateOf(GameWishlistRepository.getSavedGames(context))
    }
    val profileStats = ProfileStatsCalculator.calculate(
        steamGames = cachedGames,
        playStationRecentGames = cachedPlayStationGames,
        playStationTrophyGames = cachedPlayStationTrophyGames,
        connections = friendConnections
    )
    val displayName = profile?.username?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Jugador"
    val friendCode = profile?.friendCode?.takeIf { it.isNotBlank() } ?: "pendiente"

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) return@LaunchedEffect
        runCatching {
            UserRepository.observeCurrentUser().collect { latest ->
                profile = latest
            }
        }.onFailure {
            syncMessage = it.message ?: "No se pudo cargar el perfil de Firebase."
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            friendConnections = emptyList()
            return@LaunchedEffect
        }
        runCatching {
            ChatRepository.observeConnections().collect { latest ->
                friendConnections = latest
            }
        }.onFailure {
            syncMessage = it.message ?: "No se pudieron cargar los amigos."
        }
    }

    DisposableEffect(lifecycleOwner, currentUser?.uid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedGames = GameWishlistRepository.getSavedGames(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Image(
                painter = painterResource(id = profileBannerRes(profile?.bannerImageId ?: 1)),
                contentDescription = "Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = { navController.navigate(Screen.ProfileSettings.route) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomCenter)
                    .border(3.dp, Color(0xFF0F0F0F), CircleShape),
                shape = CircleShape,
                color = Color(0xFF3A3A3A)
            ) {
                Image(
                    painter = painterResource(id = profileAvatarRes(profile?.profileImageId ?: 1)),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = friendCode,
                color = Color(0xFFE6E6E6),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (friendCode != "pendiente") {
                                clipboardManager.setText(AnnotatedString(friendCode))
                                syncMessage = "Codigo de amigo copiado."
                            }
                        }
                    )
            )
            syncMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFF8DE2B7),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    FirebaseManager.logout(context)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Principal.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, VisibleOutline)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesion")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Juegos", profileStats.totalGames.toString())
            StatItem("Trofeos", profileStats.totalTrophies.toString())
            StatItem("Amigos", profileStats.friendCount.toString())
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Color.White
                )
            },
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                icon = { Icon(Icons.Default.Gamepad, contentDescription = null) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                SeccionPlataformas()
            } else {
                SeccionCuadriculaJuegosSeparada(
                    games = savedGames,
                    onGameClick = { platform, gameId ->
                        navController.navigate(
                            Screen.DetalleJuego.createRoute(platform, gameId)
                        )
                    }
                )
            }
        }
    }

}

@Composable
fun SeccionPlataformas() {
    val context = LocalContext.current
    val linkedAccount = SteamRepository.getLinkedAccount(context)
    val cachedGames = SteamRepository.getCachedGames(context)
    val linkedPlayStationAccount = PlayStationRepository.getLinkedAccount(context)
    val cachedPlayStationGames = PlayStationRepository.getCachedGames(context)
    val cachedPlayStationTrophyGames = PlayStationRepository.getCachedTrophyGames(context)
    val playStationStatus = PlayStationRepository.profileStatusText(
        account = linkedPlayStationAccount,
        recentGames = cachedPlayStationGames,
        trophyGames = cachedPlayStationTrophyGames
    )
    val playStationTrophies = cachedPlayStationTrophyGames
        .mapNotNull { it.achievementSummary?.desbloqueados }
        .sum()
    val steamStatus = when {
        linkedAccount == null -> "Conecta Steam desde ajustes de perfil"
        !linkedAccount.isPublicProfile -> "Perfil privado o sin biblioteca publica"
        cachedGames.isEmpty() -> "Vinculada sin juegos visibles"
        else -> "Vinculada con ${cachedGames.size} juegos"
    }
    val steamAccountLabel = linkedAccount?.displayName
        ?: linkedAccount?.steamId
        ?: "Sin vincular"
    val playStationAccountLabel = linkedPlayStationAccount?.onlineId
        ?: linkedPlayStationAccount?.displayName
        ?: "Sin vincular"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PlataformaCardElegante(
                nombre = "Steam",
                subtitulo = steamStatus,
                trofeos = cachedGames.mapNotNull { it.achievementSummary?.desbloqueados }.sum().toString(),
                imageRes = R.drawable.banner_steam,
                accountLabel = steamAccountLabel
            )
        }
        item {
            PlataformaCardElegante(
                nombre = "PlayStation",
                subtitulo = playStationStatus,
                trofeos = playStationTrophies.toString(),
                imageRes = R.drawable.banner_psn,
                accountLabel = playStationAccountLabel
            )
        }
        item {
            // DEJO GAME PASS COMO TARJETA VISUAL PARA EXPLICAR EN EL TFG QUE FALTA EL ACCESO DE MICROSOFT
            PlataformaCardElegante(
                nombre = "Game Pass",
                subtitulo = "Esperando implementacion futura",
                trofeos = "0",
                imageRes = R.drawable.banner_xbox,
                accountLabel = "No disponible"
            )
        }
    }
}

@Composable
fun PlataformaCardElegante(
    nombre: String,
    subtitulo: String,
    trofeos: String,
    imageRes: Int,
    accountLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                            startX = 0f,
                            endX = 500f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = nombre,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subtitulo,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            null,
                            tint = Color(0xFFFFD700).copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$trofeos trofeos",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.52f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = accountLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeccionCuadriculaJuegosSeparada(
    games: List<Juego>,
    onGameClick: (PlataformaJuego, String) -> Unit
) {
    if (games.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Guarda juegos desde el boton + del detalle para ver aqui tu lista de deseos.",
                color = Color.LightGray
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            AsyncImage(
                model = game.capsuleImageUrl,
                contentDescription = game.title,
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SubtleTrack)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    .clickable { onGameClick(game.platform, game.platformGameId) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = ReadableSecondary, fontSize = 12.sp)
    }
}

