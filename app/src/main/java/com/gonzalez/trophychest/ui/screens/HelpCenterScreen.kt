package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.navigation.Screen

private val HelpBackground = Color(0xFF0F0F0F)
private val HelpSurface = Color(0xFF1A1A1A)
private val HelpAccent = Color(0xFFF6C453)

// ESTAS LISTAS SON CONTENIDO LOCAL DE AYUDA, NO VIENEN DE FIREBASE NI DE INTERNET.
private data class HelpBlock(
    val title: String,
    val bullets: List<String>
)

private data class HelpSection(
    val title: String,
    val intro: String,
    val blocks: List<HelpBlock>
)

private val helpSections = listOf(
    HelpSection(
        title = "Perfil",
        intro = "Aqui se concentra tu identidad dentro de TrophyChest y los accesos principales de tu cuenta.",
        blocks = listOf(
            HelpBlock(
                title = "Datos del perfil",
                bullets = listOf(
                    "En Perfil puedes ver tu nombre publico, correo, codigo de amigo, plataformas vinculadas y juegos guardados.",
                    "El engranaje abre Ajustes de perfil, donde puedes cambiar el nombre publico si han pasado 5 dias desde el ultimo cambio.",
                    "Desde Ajustes puedes elegir avatar y banner entre las imagenes disponibles y guardarlos con Guardar imagenes.",
                    "El codigo de amigo tambien aparece en Amigos para que puedas compartirlo con otros jugadores.",
                    "Cerrar sesion sale de tu cuenta y vuelve a la pantalla de inicio de sesion."
                )
            )
        )
    ),
    HelpSection(
        title = "Plataformas",
        intro = "Para conectar bibliotecas, entra en Perfil, toca el engranaje y baja hasta la seccion Plataformas.",
        blocks = listOf(
            HelpBlock(
                title = "Antes de vincular",
                bullets = listOf(
                    "Desde esa seccion puedes vincular, sincronizar o desvincular cada cuenta compatible.",
                    "Si una biblioteca no aparece, revisa que la cuenta este vinculada, que el perfil sea visible y que esa cuenta tenga juegos compatibles o visibles.",
                    "La sincronizacion depende de la informacion publica o autorizada que devuelve cada plataforma."
                )
            ),
            HelpBlock(
                title = "Steam",
                bullets = listOf(
                    "Puedes vincular Steam pegando tu SteamID64 o la URL publica de tu perfil.",
                    "Para encontrar el SteamID en la app de Steam, abre los detalles o ajustes de la cuenta; debajo de los datos de cuenta aparece el ID de Steam.",
                    "Tambien puedes copiar la URL de tu perfil publico de Steam si la tienes visible.",
                    "No se pide la contrasena de Steam: solo se usa el identificador para consultar la biblioteca visible."
                )
            ),
            HelpBlock(
                title = "PlayStation",
                bullets = listOf(
                    "Al intentar vincular PlayStation se abre un dialogo con dos botones.",
                    "El primer boton te lleva al navegador para iniciar sesion en PlayStation Network.",
                    "El segundo boton abre la pagina que muestra el NPSSO, el token necesario para autorizar la conexion.",
                    "Copia el valor del NPSSO, pegalo en TrophyChest y pulsa Vincular para terminar el proceso.",
                    "Si el NPSSO caduca o falla, vuelve a generarlo iniciando sesion de nuevo desde el navegador."
                )
            ),
            HelpBlock(
                title = "Game Pass",
                bullets = listOf(
                    "Game Pass aparece como plataforma futura dentro del perfil.",
                    "La conexion real queda pendiente porque Microsoft todavia tiene que dar acceso a la API necesaria.",
                    "Por eso no se piden credenciales ni se intenta sincronizar juegos de Microsoft desde la app actual."
                )
            )
        )
    ),
    HelpSection(
        title = "Amigos y chat",
        intro = "La zona social usa codigos de amigo, solicitudes y chats privados entre usuarios.",
        blocks = listOf(
            HelpBlock(
                title = "Como funciona",
                bullets = listOf(
                    "Tu codigo de amigo tiene 6 caracteres y aparece en la pantalla Amigos.",
                    "Usa el boton superior de anadir amigo para introducir el codigo de otro jugador y enviarle una solicitud.",
                    "Las solicitudes recibidas se pueden aceptar o rechazar. Las enviadas quedan marcadas como pendientes.",
                    "Cuando una solicitud se acepta, aparece en Chats y puedes abrir la conversacion tocando el amigo.",
                    "Los chats muestran el ultimo mensaje, la hora y un indicador cuando hay mensajes sin leer."
                )
            )
        )
    ),
    HelpSection(
        title = "Pantallas",
        intro = "Cada pantalla cubre una parte distinta de la experiencia de TrophyChest.",
        blocks = listOf(
            HelpBlock(
                title = "Navegacion principal",
                bullets = listOf(
                    "Inicio resume tus juegos recientes y el calendario de proximos estrenos.",
                    "Buscar permite localizar juegos en IGDB desde la barra superior y abrir su detalle.",
                    "Trofeos agrupa logros y trofeos disponibles de las plataformas vinculadas.",
                    "Perfil muestra tus datos, plataformas, juegos vinculados y lista de guardados.",
                    "Explorar y las pantallas de detalle amplian la informacion de juegos, trofeos y recomendaciones."
                )
            )
        )
    )
)

// CENTRO DE AYUDA EXPLICA AL USUARIO COMO USAR LA APP DESDE DENTRO.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Centro de ayuda",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Principal.route)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HelpBackground)
            )
        },
        containerColor = HelpBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(HelpBackground),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 28.dp
            )
        ) {
            item {
                Text(
                    text = "Consulta rapida sobre perfil, plataformas, amigos y pantallas.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            items(helpSections, key = { it.title }) { section ->
                HelpSectionCard(section = section)
            }
        }
    }
}

@Composable
private fun HelpSectionCard(section: HelpSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HelpSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = section.intro,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            section.blocks.forEach { block ->
                HelpBlockContent(block = block)
            }
        }
    }
}

@Composable
private fun HelpBlockContent(block: HelpBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = block.title,
            color = HelpAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        block.bullets.forEach { bullet ->
            HelpBullet(text = bullet)
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun HelpBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "-",
            color = HelpAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
