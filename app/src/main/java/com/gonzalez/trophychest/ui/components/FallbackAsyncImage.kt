package com.gonzalez.trophychest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


@Composable
fun FallbackAsyncImage(
    imageUrls: List<String?>,//DIRECCIONES DE IMAGENES DEL JUEGO , RECIBIMOS VARIAS POR SI LA PRIMERA NO ESTA DISPONIBLE COMO EN EL PEAK
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,//CROP PARA RELLENAR EL CONTENEDOR
    placeholderLabel: String = "Steam"//SI NO HAY IMAGEN VALIDA MUESTRA STEAM
) {
    val validUrls = remember(imageUrls) {//RECUERDA LA LISTA DE URLS
        imageUrls
            .filterNotNull()//FILTRA PARA LAS QUE NO SE VEAN BIEN (NULAS) Y LAS ELIMINA
            .map { it.trim() }//ELIMINA ESPACIOS
            .filter { it.isNotBlank() }//ELIMINA TEXTOS VACIOS
            .distinct()//ELIMINA DUPLICADOS
    }
    var selectedIndex by remember(validUrls) { mutableIntStateOf(0) }
    //UNA VEZ APLICADOS LOS FILTROS SE REVISAN PARA VER LA PRIMERA DISPONIBLE

    //CREAMOS EL CONTENEDOR
    Box(
        modifier = modifier.background(Color(0xFF202833)),
        contentAlignment = Alignment.Center
    ) {
        if (validUrls.isNotEmpty()) { //SI NO ESTA VACIO PONEMOS LA IMAGEN
            AsyncImage(
                model = validUrls[selectedIndex],
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = { //ERROR AL CARGAR LA IMAGEN
                    if (selectedIndex < validUrls.lastIndex) {
                        selectedIndex += 1 //BUSCA LA SIGUIENTE
                    }
                }
            )
        } else {//MUESTRA TEXTO POR DEFECTO
            Text(
                text = placeholderLabel,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
