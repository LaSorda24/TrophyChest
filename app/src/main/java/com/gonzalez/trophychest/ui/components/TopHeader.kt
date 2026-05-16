package com.gonzalez.trophychest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.IGDBGame
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.ReadableTertiary

@Composable
fun TopHeader(
    searchQuery: String,//TEXTO BASE
    onSearchQueryChange: (String) -> Unit,//FUNCION DE BUSCA
    onClearSearch: () -> Unit,//FUNCION QUE LIMPIA
    searchResultsState: RemoteUiState<List<IGDBGame>>,//ESTADO DE BUSQUEDA
    onGameClick: (IGDBGame) -> Unit,//ABRIR JUEGO PULSADO
    onHelpClick: () -> Unit//BOTON AYUDA
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()//OCUPE EL MAXIMO
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),//MARGENES INTERNOS
                verticalAlignment = Alignment.CenterVertically
            ) {
                //ICONO DE APP
                Image(
                    painter = painterResource(id = R.drawable.logo_principal),
                    contentDescription = "Logo de TrophyChest",
                    modifier = Modifier
                        .height(42.dp)
                        .wrapContentWidth(),
                    contentScale = ContentScale.Fit//ENCAJA SIN RECORTARSE
                )
                //LLAMAMOS A BARRA DE BUSQUEDA
                SearchField(
                    query = searchQuery,//LE PASA EL TEXTO
                    onQueryChange = onSearchQueryChange,//AVISA CUANDO ESCRIBIRMOS
                    onClearSearch = onClearSearch,//LIMPIA AL CERRAR
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                //BOTON DE AYUDA
                IconButton(onClick = onHelpClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Abrir ayuda",
                        tint = Color.LightGray,
                        modifier = Modifier.size(28.dp)


                    )
                }
            }
        }

        if (searchQuery.trim().isNotBlank()) {
            SearchResultsDropdown(//FUNCION PARA DESPLESGAR DE ABAJO
                query = searchQuery.trim(),//PODRIA ELIMINARSE------------------------------------------------
                state = searchResultsState,
                onGameClick = onGameClick
            )
        }
    }
}

//BARRA DE BUSQUEDA
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,//------------QUE DEVUELVE-------------------
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    //FONDO DE BARRA
    Surface(
        modifier = modifier.height(56.dp),
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(28.dp)
    ) {
        //TEXTO
        TextField(
            value = query,
            onValueChange = onQueryChange,//AVISA CUANDO ESCRIBIMOS
            modifier = Modifier.fillMaxSize(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            placeholder = {//TEXTO DE BASE
                Text("Buscar...", color = ReadableTertiary, fontSize = 14.sp)
            },
            leadingIcon = {//ICONO DE LUPA
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ReadableTertiary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {//ICONO DE LIMPIAR
                if (query.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar busqueda",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}


//DESPLAGABLE CON ESTADO DE BUSQUEDA
@Composable
private fun SearchResultsDropdown(
    query: String,
    state: RemoteUiState<List<IGDBGame>>,//LISTA DE JUEGOS DE IGDB
    onGameClick: (IGDBGame) -> Unit
) {
    Surface(//SUPERFICIE DE BUSQUEDA
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shadowElevation = 8.dp
    ) {
        when (state) {
            RemoteUiState.Loading -> SearchDropdownMessage("Buscando...", isLoading = true)//SI ESTA CARGANDO
            is RemoteUiState.Error -> SearchDropdownMessage(state.message)//SI HAY ALGUN ERROR
            is RemoteUiState.Empty -> SearchDropdownMessage(state.message)//SI NO HAY NADA
            //SI FUNCIONA...
            is RemoteUiState.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.data, key = { it.id }) { game ->
                    SearchResultRow(//LLAMAMOS A LA FILA DE BUSQUEDA DE ABAJO
                        game = game,
                        onClick = { onGameClick(game) }
                    )
                }
            }
        }
    }
}

//MENSAJE DE LA BARRA DE BUSQUEDA
@Composable
private fun SearchDropdownMessage(
    message: String,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) { //SI ESTA PENSANDO
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(//MENSAJE DE BASE
            text = message.ifBlank { "Empieza a escribir para buscar juegos." },
            color = ReadableSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

//FILA DE RESULTADO
@Composable
private fun SearchResultRow(
    game: IGDBGame,
    onClick: () -> Unit
) {
    val platformsLabel = if (game.platforms.isEmpty()) { //TEXTO DE PLATAFORMAS
        "Sin plataformas disponibles"
    } else {
        game.platforms.joinToString(separator = " | ")//SI TIENE  VARIAS LAS UNE
    }
    //CREAMOS UNA FILA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface( //CONTENEDOR DE PORTADA
            modifier = Modifier.size(width = 52.dp, height = 68.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF202833)
        ) {
            FallbackAsyncImage( //PORTADA
                imageUrls = listOf(game.coverUrl),
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                placeholderLabel = "IGDB"//-------------------ESTO COMO SE VE ?
            )
        }
        //INFO DEL JUEGO
        Column(modifier = Modifier.weight(1f)) {
            //TITULO
            Text(
                text = game.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            //PLATAFORMAS
            Text(
                text = platformsLabel,
                color = ReadableSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
