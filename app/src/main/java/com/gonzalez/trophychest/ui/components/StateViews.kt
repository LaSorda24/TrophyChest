package com.gonzalez.trophychest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


//GUARDA COMPONENTRES REUTILIZABLES PARA PANTALLAS

@Composable
fun LoadingStateView(message: String = "Cargando...") {//SI NO RECIBE NINGUN OTRO MENSAJE MUESTRA CARGANDO
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.White)//CIRCULO DE CARGA EN EL CENTRO
        Text( //MENSAJE DE CARGA
            text = message,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun MessageStateView(
    title: String,
    message: String,
    //TEXTO BOTON
    actionLabel: String? = null,//PUEDE SER NULO
    onAction: (() -> Unit)? = null//ACCION OPCIONAL AL PULSAR
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            //TITULO
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            //MENSAUJE
            text = message,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 10.dp),
            lineHeight = 20.sp
        )
        //SI HAY BOTON....
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
