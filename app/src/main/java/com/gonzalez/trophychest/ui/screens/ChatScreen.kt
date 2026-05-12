package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.data.ChatMessage
import com.gonzalez.trophychest.data.ChatRepository
import com.gonzalez.trophychest.data.CONNECTION_STATUS_ACCEPTED
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.data.FriendConnection
import com.gonzalez.trophychest.data.UserProfile
import com.gonzalez.trophychest.ui.theme.ReadableDisabled
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary
import com.gonzalez.trophychest.ui.theme.SubtleTrack
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val ScreenBackground = Color(0xFF0F0F0F)
private val HeaderBackground = Color(0xFF1E1E1E)
private val OwnBubble = Color(0xFF2B5278)
private val OtherBubble = Color(0xFF1E1E1E)
private val SendBlue = Color(0xFF54A7E5)

// APUNTE: CHAT ESCUCHA MENSAJES EN TIEMPO REAL Y SOLO DEJA ESCRIBIR SI LA AMISTAD ESTA ACEPTADA.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, connectionId: String) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUid = FirebaseManager.currentUser?.uid

    var connection by remember { mutableStateOf<FriendConnection?>(null) }
    var peerProfile by remember { mutableStateOf<UserProfile?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(connectionId, currentUid) {
        if (currentUid == null) return@LaunchedEffect
        ChatRepository.observeConnection(connectionId).collect { latest ->
            connection = latest
            isLoading = false
        }
    }

    LaunchedEffect(connection?.peerId(currentUid.orEmpty())) {
        val peerUid = currentUid?.let { connection?.peerId(it) }
        if (peerUid != null) {
            peerProfile = ChatRepository.getUserProfile(peerUid)
        }
    }

    LaunchedEffect(connectionId, currentUid) {
        if (currentUid == null) return@LaunchedEffect
        ChatRepository.observeMessages(connectionId).collect { latest ->
            messages = latest
        }
    }

    LaunchedEffect(messages.size, connectionId, currentUid) {
        if (currentUid != null && messages.isNotEmpty()) {
            runCatching { ChatRepository.markChatRead(connectionId) }
        }
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            Surface(color = HeaderBackground, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }

                    ChatAvatar(profile = peerProfile)
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = peerProfile?.visibleName ?: "Chat",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color(0xFF2B2B2B))
                        ) {
                            DropdownMenuItem(text = { Text("Silenciar", color = Color.White) }, onClick = { menuExpanded = false })
                            DropdownMenuItem(text = { Text("Bloquear", color = Color.White) }, onClick = { menuExpanded = false })
                            DropdownMenuItem(text = { Text("Denunciar", color = Color.Red) }, onClick = { menuExpanded = false })
                            DropdownMenuItem(text = { Text("Eliminar amigo", color = Color.Red) }, onClick = { menuExpanded = false })
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                enabled = connection?.status == CONNECTION_STATUS_ACCEPTED,
                onSend = { text ->
                    scope.launch {
                        runCatching { ChatRepository.sendMessage(connectionId, text) }
                            .onFailure { errorMessage = it.message }
                    }
                }
            )
        },
        containerColor = ScreenBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ScreenBackground)
        ) {
            when {
                currentUid == null -> Text(
                    text = "Inicia sesion para usar el chat.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                isLoading -> CircularProgressIndicator(color = SendBlue, modifier = Modifier.align(Alignment.Center))
                connection == null -> Text(
                    text = "No se pudo abrir este chat.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                messages.isEmpty() -> EmptyChatState(modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            isFromMe = message.senderId == currentUid
                        )
                    }
                }
            }

            errorMessage?.let { message ->
                Surface(
                    color = Color(0xFF3A1F1F),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(message, color = Color.White, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(profile: UserProfile?) {
    Image(
        painter = painterResource(id = profileAvatarRes(profile?.profileImageId ?: 1)),
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SubtleTrack),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ChatBubble(message: ChatMessage, isFromMe: Boolean) {
    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromMe) OwnBubble else OtherBubble
    val shape = if (isFromMe) {
        RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = message.text, color = Color.White, fontSize = 15.sp)
                Text(
                    text = formatMessageTime(message.createdAt),
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }

    Surface(
        color = ScreenBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = HeaderBackground,
                shape = CircleShape
            ) {
                TextField(
                    value = textState,
                    onValueChange = { if (it.length <= 1000) textState = it },
                    enabled = enabled,
                    placeholder = { Text("Escribe un mensaje...", color = ReadableTertiary, fontSize = 14.sp) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = ReadableDisabled,
                        cursorColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    val message = textState
                    if (message.isNotBlank()) {
                        textState = ""
                        onSend(message)
                    }
                },
                modifier = Modifier.size(44.dp),
                containerColor = SendBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sin mensajes todavia", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Escribe el primero para empezar.", color = ReadableSecondary, fontSize = 13.sp)
    }
}

private fun formatMessageTime(timestamp: Timestamp?): String {
    timestamp ?: return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
}
