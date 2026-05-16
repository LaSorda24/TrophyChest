package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.ChatValidation
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.data.PlayStationLinkedAccount
import com.gonzalez.trophychest.data.PlayStationRepository
import com.gonzalez.trophychest.data.PlayStationSyncResult
import com.gonzalez.trophychest.data.SteamLinkedAccount
import com.gonzalez.trophychest.data.SteamRepository
import com.gonzalez.trophychest.data.SteamSyncResult
import com.gonzalez.trophychest.data.User
import com.gonzalez.trophychest.data.UserDefaults
import com.gonzalez.trophychest.data.UserRepository
import com.gonzalez.trophychest.ui.theme.ReadableDisabled
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.VisibleOutline
import kotlinx.coroutines.launch

private val SettingsBackground = Color(0xFF0F0F0F)
private val SettingsSurface = Color(0xFF1A1A1A)
private val SettingsAccent = Color(0xFFF6C453)
private const val PLAYSTATION_SIGN_IN_URL = "https://www.playstation.com/"
private const val PLAYSTATION_NPSSO_URL = "https://ca.account.sony.com/api/v1/ssocookie"

private data class PlayStationDialogState(
    val message: String,
    val isLoading: Boolean = false
)

// AJUSTES PERMITE CAMBIAR PERFIL Y VINCULAR/DESVINCULAR STEAM O PLAYSTATION.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkedEmail = FirebaseManager.currentUser?.email
        ?.takeIf { it.isNotBlank() }
        ?: "Sin correo vinculado"

    // ESTAS VARIABLES SON EL ESTADO DE LA PANTALLA. AL CAMBIAR UNA, COMPOSE REDIBUJA LA UI.
    var profile by remember { mutableStateOf<User?>(null) }
    var username by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableIntStateOf(UserDefaults.MIN_IMAGE_ID) }
    var selectedBanner by remember { mutableIntStateOf(UserDefaults.MIN_IMAGE_ID) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSteamDialog by remember { mutableStateOf(false) }
    var showPlayStationDialog by remember { mutableStateOf(false) }
    var playStationDialogState by remember { mutableStateOf<PlayStationDialogState?>(null) }
    var linkedAccount by remember { mutableStateOf<SteamLinkedAccount?>(SteamRepository.getLinkedAccount(context)) }
    var linkedPlayStationAccount by remember {
        mutableStateOf<PlayStationLinkedAccount?>(PlayStationRepository.getLinkedAccount(context))
    }

    LaunchedEffect(Unit) {
        // LAUNCHEDEFFECT ARRANCA LA ESCUCHA DEL PERFIL SOLO CUANDO ENTRA LA PANTALLA.
        UserRepository.observeCurrentUser().collect { latest ->
            profile = latest
            latest?.let {
                username = it.username
                selectedAvatar = it.profileImageId
                selectedBanner = it.bannerImageId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SettingsBackground)
            )
        },
        containerColor = SettingsBackground
    ) { padding ->
        if (profile == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SettingsAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "Cuenta") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Correo vinculado",
                        color = ReadableSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = linkedEmail,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            SettingsSection(title = "Nombre publico") {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                    label = { Text("Nombre de usuario") },
                    isError = username.isNotBlank() && ChatValidation.usernameError(username) != null,
                    supportingText = {
                        val error = ChatValidation.usernameError(username)
                        when {
                            error != null && username.isNotBlank() -> Text(error)
                            else -> Text("Puedes cambiarlo tantas veces como necesites.")
                        }
                    }
                )
                Button(
                    onClick = {
                        val validationError = ChatValidation.usernameError(username)
                        if (validationError != null) {
                            message = validationError
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            // UPDATEUSERNAME VA A FIRESTORE; POR ESO LO LANZO EN CORRUTINA.
                            runCatching { UserRepository.updateUsername(username) }
                                .onSuccess {
                                    profile = it
                                    message = "Nombre actualizado."
                                }
                                .onFailure { message = it.message ?: "No se pudo cambiar el nombre." }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving && username != profile?.username,
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent)
                ) {
                    Text("Guardar nombre", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            SettingsSection(title = "Avatar") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items((1..5).toList()) { imageId ->
                        SelectableAvatar(
                            imageId = imageId,
                            selected = selectedAvatar == imageId,
                            onClick = { selectedAvatar = imageId }
                        )
                    }
                }
            }

            SettingsSection(title = "Banner") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items((1..5).toList()) { imageId ->
                        SelectableBanner(
                            imageId = imageId,
                            selected = selectedBanner == imageId,
                            onClick = { selectedBanner = imageId }
                        )
                    }
                }
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            // GUARDO SOLO IDS DE AVATAR/BANNER, NO ARCHIVOS DE IMAGEN.
                            runCatching {
                                UserRepository.updateProfileImages(selectedAvatar, selectedBanner)
                            }.onSuccess {
                                profile = it
                                message = "Imagenes actualizadas."
                            }.onFailure {
                                message = it.message ?: "No se pudieron guardar las imagenes."
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving &&
                        (selectedAvatar != profile?.profileImageId || selectedBanner != profile?.bannerImageId),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent)
                ) {
                    Text("Guardar imagenes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            SettingsSection(title = "Plataformas") {
                PlatformActionRow(
                    title = "Steam",
                    subtitle = linkedAccount?.displayName ?: linkedAccount?.steamId ?: "No vinculada",
                    actionLabel = if (linkedAccount == null) "Vincular" else "Desvincular",
                    icon = Icons.Default.Link,
                    onClick = {
                        // VINCULAR STEAM ABRE DIALOGO; DESVINCULAR BORRA CACHE LOCAL DE ESA PLATAFORMA.
                        if (linkedAccount == null) {
                            showSteamDialog = true
                        } else {
                            SteamRepository.unlinkSteam(context)
                            linkedAccount = null
                            message = "La cuenta de Steam se ha desvinculado del dispositivo."
                        }
                    }
                )
                PlatformActionRow(
                    title = "PlayStation",
                    subtitle = PlayStationRepository.accountDisplayName(linkedPlayStationAccount),
                    actionLabel = if (linkedPlayStationAccount == null) "Vincular" else "Desvincular",
                    icon = Icons.Default.Link,
                    enabled = !isSaving,
                    onClick = {
                        message = null
                        // PLAYSTATION NECESITA PROXY; SI NO HAY URL CONFIGURADA, NO INTENTO VINCULAR.
                        if (!PlayStationRepository.hasProxyEndpoint()) {
                            playStationDialogState = PlayStationDialogState(
                                message = PlayStationRepository.setupMessage()
                            )
                            return@PlatformActionRow
                        }

                        if (linkedPlayStationAccount == null) {
                            showPlayStationDialog = true
                        } else {
                            PlayStationRepository.unlinkPlayStation(context)
                            linkedPlayStationAccount = null
                            message = "La cuenta de PlayStation se ha desvinculado del dispositivo."
                        }
                    }
                )
                // DEJO GAME PASS SOLO COMO PARTE VISUAL PORQUE TODAVIA FALTA EL ACCESO OFICIAL DE MICROSOFT
                PlatformActionRow(
                    title = "Game Pass",
                    subtitle = "Futura implementacion",
                    actionLabel = "No disponible",
                    icon = null,
                    enabled = false,
                    onClick = {}
                )
            }

            message?.let {
                Surface(
                    color = Color(0xFF242424),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SettingsAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showSteamDialog) {
        SteamSettingsDialog(
            initialValue = linkedAccount?.profileInput.orEmpty(),
            onDismiss = { showSteamDialog = false },
            onConfirm = { input ->
                showSteamDialog = false
                isSaving = true
                scope.launch {
                    // IMPORTLIBRARY CONTACTA CON STEAM Y GUARDA LOS JUEGOS EN CACHE LOCAL.
                    message = when (val result = SteamRepository.importLibrary(context, input)) {
                        SteamSyncResult.MissingApiKey -> SteamRepository.steamSetupMessage()
                        is SteamSyncResult.Failure -> result.message
                        is SteamSyncResult.Success -> {
                            linkedAccount = result.linkedAccount
                            result.warningMessage
                                ?: "Steam vinculada como ${result.linkedAccount.displayName ?: result.linkedAccount.steamId}."
                        }
                    }
                    isSaving = false
                }
            }
        )
    }

    if (showPlayStationDialog) {
        PlayStationSettingsDialog(
            onDismiss = { showPlayStationDialog = false },
            onConfirm = { npsso ->
                showPlayStationDialog = false
                isSaving = true
                playStationDialogState = PlayStationDialogState(
                    message = "Vinculando PlayStation...",
                    isLoading = true
                )
                scope.launch {
                    // EL NPSSO SE ENVIA AL PROXY, Y EL REPOSITORIO DEVUELVE CUENTA/JUEGOS/TROFEOS.
                    playStationDialogState = when (val result = PlayStationRepository.linkWithNpsso(context, npsso)) {
                        PlayStationSyncResult.MissingProxyEndpoint -> PlayStationDialogState(
                            message = PlayStationRepository.setupMessage()
                        )
                        PlayStationSyncResult.MissingSession -> PlayStationDialogState(
                            message = "Vuelve a vincular PlayStation con un NPSSO nuevo."
                        )
                        is PlayStationSyncResult.Failure -> PlayStationDialogState(
                            message = result.message
                        )
                        is PlayStationSyncResult.LinkedOnly -> {
                            linkedPlayStationAccount = result.linkedAccount
                            PlayStationDialogState(message = result.message)
                        }
                        is PlayStationSyncResult.Success -> {
                            linkedPlayStationAccount = result.linkedAccount
                            result.warningMessage?.let { PlayStationDialogState(message = it) }
                        }
                    }
                    isSaving = false
                }
            }
        )
    }

    playStationDialogState?.let { state ->
        AlertDialog(
            onDismissRequest = {
                if (!state.isLoading) playStationDialogState = null
            },
            title = { Text("Vincular PlayStation") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SettingsAccent)
                    }
                    Text(state.message, fontSize = 14.sp)
                }
            },
            confirmButton = {
                if (!state.isLoading) {
                    TextButton(onClick = { playStationDialogState = null }) {
                        Text("Cerrar")
                    }
                }
            },
            dismissButton = {
                if (!state.isLoading && linkedPlayStationAccount != null) {
                    TextButton(
                        onClick = {
                            PlayStationRepository.unlinkPlayStation(context)
                            linkedPlayStationAccount = null
                            playStationDialogState = PlayStationDialogState(
                                "La cuenta de PlayStation se ha desvinculado del dispositivo."
                            )
                        }
                    ) {
                        Text("Desvincular")
                    }
                }
            }
        )
    }

}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SelectableAvatar(imageId: Int, selected: Boolean, onClick: () -> Unit) {
    Image(
        painter = painterResource(id = profileAvatarRes(imageId)),
        contentDescription = "Avatar $imageId",
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) SettingsAccent else Color.White.copy(alpha = 0.35f),
                shape = CircleShape
            ),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun SelectableBanner(imageId: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) SettingsAccent else Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Image(
            painter = painterResource(id = profileBannerRes(imageId)),
            contentDescription = "Banner $imageId",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PlatformActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = Icons.Default.Link,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ReadableSecondary, fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                disabledContentColor = ReadableDisabled
            ),
            border = BorderStroke(1.dp, if (enabled) VisibleOutline else Color.White.copy(alpha = 0.35f))
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(actionLabel)
        }
    }
}

@Composable
private fun SteamSettingsDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular Steam") },
        text = {
            Column {
                Text(
                    text = "Pega tu SteamID64 o la URL publica de tu perfil. No te pediremos usuario ni contrasena.",
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    label = { Text("SteamID o URL") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(input) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PlayStationSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular PlayStation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Primero inicia sesion en PlayStation.com. Despues abre la pagina del NPSSO, copia el valor npsso y pegalo aqui. No guardaremos ese NPSSO.",
                    fontSize = 14.sp
                )
                OutlinedButton(
                    onClick = { uriHandler.openUri(PLAYSTATION_SIGN_IN_URL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar sesion en PlayStation")
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri(PLAYSTATION_NPSSO_URL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Obtener NPSSO")
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("NPSSO") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.length == 64
            ) {
                Text("Vincular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
