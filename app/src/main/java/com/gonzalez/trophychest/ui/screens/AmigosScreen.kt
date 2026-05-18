package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gonzalez.trophychest.data.ChatRepository
import com.gonzalez.trophychest.data.ChatValidation
import com.gonzalez.trophychest.data.CONNECTION_STATUS_ACCEPTED
import com.gonzalez.trophychest.data.CONNECTION_STATUS_PENDING
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.data.FriendConnection
import com.gonzalez.trophychest.data.UserDefaults
import com.gonzalez.trophychest.data.UserProfile
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary
import com.gonzalez.trophychest.ui.theme.SubtleTrack
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChatBackground = Color(0xFF0F0F0F)
private val ChatSurface = Color(0xFF1E1E1E)
private val AccentGold = Color(0xFFF6C453)

// ESTA PANTALLA GESTIONA BUSCAR AMIGOS, SOLICITUDES Y ACCESO AL CHAT.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmigosScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseManager.currentUser?.uid
    val profiles = remember { mutableStateMapOf<String, UserProfile>() }

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var connections by remember { mutableStateOf<List<FriendConnection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { ChatRepository.ensureUserProfile() }
            .onSuccess { profile = it }
            .onFailure { errorMessage = friendErrorMessage(it) }
        isLoading = false
    }

    LaunchedEffect(currentUid) {
        if (currentUid == null) return@LaunchedEffect
        ChatRepository.observeConnections().collect { latest ->
            connections = latest
        }
    }

    LaunchedEffect(connections, currentUid) {
        if (currentUid == null) return@LaunchedEffect
        connections
            .mapNotNull { it.peerId(currentUid) }
            .filterNot { profiles.containsKey(it) }
            .distinct()
            .forEach { peerUid ->
                ChatRepository.getUserProfile(peerUid)?.let { profiles[peerUid] = it }
            }
    }

    val acceptedConnections = connections.filter { it.status == CONNECTION_STATUS_ACCEPTED }
    val pendingReceived = connections.filter {
        it.status == CONNECTION_STATUS_PENDING && it.requestedTo == currentUid
    }
    val pendingSent = connections.filter {
        it.status == CONNECTION_STATUS_PENDING && it.requestedBy == currentUid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Amigos",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${acceptedConnections.size})",
                            color = ReadableSecondary,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddFriendDialog = true },
                        enabled = currentUid != null && !profile?.friendCode.isNullOrBlank()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Anadir amigo", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatBackground)
            )
        },
        containerColor = ChatBackground
    ) { padding ->
        when {
            currentUid == null -> LoginRequiredState(modifier = Modifier.padding(padding))
            isLoading -> LoadingState(modifier = Modifier.padding(padding))
            else -> FriendContent(
                modifier = Modifier.padding(padding),
                currentUid = currentUid,
                profile = profile,
                acceptedConnections = acceptedConnections,
                pendingReceived = pendingReceived,
                pendingSent = pendingSent,
                profiles = profiles,
                errorMessage = errorMessage,
                onOpenChat = { connectionId -> navController.navigate("chat/$connectionId") },
                onAccept = { connectionId ->
                    scope.launch {
                        runCatching { ChatRepository.acceptConnection(connectionId) }
                            .onFailure { errorMessage = friendErrorMessage(it) }
                    }
                },
                onReject = { connectionId ->
                    scope.launch {
                        runCatching { ChatRepository.rejectConnection(connectionId) }
                            .onFailure { errorMessage = friendErrorMessage(it) }
                    }
                },
                onDismissError = { errorMessage = null }
            )
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onSendRequest = { friendCode ->
                scope.launch {
                    runCatching {
                        val target = ChatRepository.findUserByFriendCode(friendCode)
                            ?: throw IllegalStateException("No existe ningun usuario con ese codigo de amigo.")
                        ChatRepository.sendFriendRequest(target.uid)
                    }.onSuccess {
                        showAddFriendDialog = false
                    }.onFailure {
                        errorMessage = friendErrorMessage(it)
                    }
                }
            }
        )
    }
}

private fun friendErrorMessage(throwable: Throwable): String {
    return if (
        throwable is FirebaseFirestoreException &&
        throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    ) {
        "No se pudo completar la accion por permisos. Actualiza las reglas de Firebase e intentalo de nuevo."
    } else {
        throwable.message ?: "No se pudo completar la accion."
    }
}

@Composable
private fun FriendContent(
    modifier: Modifier,
    currentUid: String,
    profile: UserProfile?,
    acceptedConnections: List<FriendConnection>,
    pendingReceived: List<FriendConnection>,
    pendingSent: List<FriendConnection>,
    profiles: Map<String, UserProfile>,
    errorMessage: String?,
    onOpenChat: (String) -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onDismissError: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        errorMessage?.let { message ->
            item {
                ErrorBanner(message = message, onDismiss = onDismissError)
            }
        }

        if (pendingReceived.isNotEmpty()) {
            item { SectionTitle("Solicitudes recibidas") }
            items(pendingReceived, key = { it.id }) { connection ->
                val peer = connection.peerId(currentUid)?.let { profiles[it] }
                FriendRequestItem(
                    connection = connection,
                    peer = peer,
                    onAccept = { onAccept(connection.id) },
                    onReject = { onReject(connection.id) }
                )
            }
        }

        if (acceptedConnections.isNotEmpty()) {
            item { SectionTitle("Chats") }
            items(acceptedConnections, key = { it.id }) { connection ->
                val peer = connection.peerId(currentUid)?.let { profiles[it] }
                FriendItem(
                    connection = connection,
                    currentUid = currentUid,
                    peer = peer,
                    onClick = { onOpenChat(connection.id) }
                )
            }
        } else {
            item {
                EmptyFriendsState(
                    hasPending = pendingReceived.isNotEmpty() || pendingSent.isNotEmpty()
                )
            }
        }

        if (pendingSent.isNotEmpty()) {
            item { SectionTitle("Solicitudes enviadas") }
            items(pendingSent, key = { it.id }) { connection ->
                val peer = connection.peerId(currentUid)?.let { profiles[it] }
                PendingSentItem(peer = peer)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = ReadableSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun FriendItem(
    connection: FriendConnection,
    currentUid: String,
    peer: UserProfile?,
    onClick: () -> Unit
) {
    val hasUnread = connection.hasUnread(currentUid)
    val prefix = if (connection.lastSenderId == currentUid) "Tu: " else ""
    val preview = connection.lastMessageText ?: "Ya sois amigos. Empieza la conversacion."
    val messageColor = if (hasUnread) Color.White else ReadableSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = peer, size = 55)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer?.visibleName ?: "Jugador",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$prefix$preview",
                color = messageColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTimestamp(connection.lastMessageAt),
                color = if (hasUnread) AccentGold else ReadableTertiary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AccentGold)
                )
            } else {
                Spacer(modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun FriendRequestItem(
    connection: FriendConnection,
    peer: UserProfile?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = peer, size = 50)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer?.visibleName ?: "Jugador",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Quiere anadirte como amigo",
                color = ReadableSecondary,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )
        }
        IconButton(onClick = onAccept) {
            Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = AccentGold)
        }
        IconButton(onClick = onReject) {
            Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = ReadableSecondary)
        }
    }
}

@Composable
private fun PendingSentItem(peer: UserProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = peer, size = 44)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(peer?.visibleName ?: "Jugador", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Solicitud pendiente", color = ReadableSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile?, size: Int) {
    Image(
        painter = painterResource(id = profileAvatarRes(profile?.profileImageId ?: 1)),
        contentDescription = null,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(SubtleTrack),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onSendRequest: (String) -> Unit
) {
    var friendCode by remember { mutableStateOf("") }
    val validationError = UserDefaults.friendCodeError(friendCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anadir amigo") },
        text = {
            Column {
                OutlinedTextField(
                    value = friendCode,
                    onValueChange = { friendCode = UserDefaults.normalizeFriendCode(it) },
                    singleLine = true,
                    label = { Text("Codigo de amigo") },
                    isError = friendCode.isNotBlank() && validationError != null
                )
                if (friendCode.isNotBlank() && validationError != null) {
                    Text(validationError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendRequest(friendCode) },
                enabled = validationError == null
            ) {
                Text("Enviar solicitud")
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
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = Color(0xFF3A1F1F),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun EmptyFriendsState(hasPending: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AccentGold, modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasPending) "Aun no tienes chats activos" else "Todavia no tienes amigos",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Usa el boton superior para enviar una solicitud por codigo.",
            color = ReadableSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AccentGold)
    }
}

@Composable
private fun LoginRequiredState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Inicia sesion para usar amigos y chat.", color = Color.White)
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    timestamp ?: return ""
    val date = timestamp.toDate()
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val messageDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
    val pattern = if (today == messageDay) "HH:mm" else "dd/MM"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
