package com.gonzalez.trophychest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.R

data class Categoria(val nombre: String, val imagenRes: Int, val slug: String)

@Composable
fun BusquedaScreen(navController: NavHostController) {
    val categorias = listOf(
        Categoria("ACCION", R.drawable.categoria_accion, "accion"),
        Categoria("AVENTURA", R.drawable.categoria_aventura, "aventura"),
        Categoria("RPG", R.drawable.categoria_rpg, "rpg"),
        Categoria("TERROR", R.drawable.categoria_terror, "terror"),
        Categoria("INDIE", R.drawable.categoria_indie, "indie"),
        Categoria("ESTRATEGIA", R.drawable.categoria_estrategia, "estrategia"),
        Categoria("DEPORTES", R.drawable.categoria_deportes, "deportes"),
        Categoria("SIMULACION", R.drawable.categoria_simulacion, "simulacion"),
        Categoria("CARRERAS", R.drawable.categoria_carreras, "carreras")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Text(
            text = "Categorias",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 124.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categorias) { cat ->
                ItemCategoria(
                    categoria = cat,
                    onClick = {
                        navController.navigate("categoria/${cat.slug}")
                    }
                )
            }
        }
    }
}

@Composable
fun ItemCategoria(categoria: Categoria, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1080f / 1428f),
            color = Color(0xFF151515),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = categoria.imagenRes),
                    contentDescription = categoria.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Text(
            text = categoria.nombre,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
