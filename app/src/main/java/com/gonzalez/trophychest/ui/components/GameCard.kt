package com.gonzalez.trophychest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonzalez.trophychest.ui.theme.ReadableSecondary
import com.gonzalez.trophychest.ui.theme.SubtleTrack

@Composable
fun GameCard(name: String, progress: Float, platform: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AQUI DEJO EL HUECO DONDE VA LA IMAGEN DEL JUEGO
            Surface(
                modifier = Modifier.size(60.dp),
                color = Color(0xFF3A3A3A),
                shape = RoundedCornerShape(8.dp)
            ) {}

            Spacer(modifier = Modifier.width(16.dp))

            // AQUI MUESTRO EL NOMBRE DEL JUEGO Y SU PLATAFORMA
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = platform, color = ReadableSecondary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                // ESTA BARRA ME SIRVE PARA VER EL AVANCE DE TROFEOS DE UN VISTAZO
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFFFFD700),
                    trackColor = SubtleTrack,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // AQUI ENSENO EL PORCENTAJE PARA QUE NO HAYA QUE INTERPRETAR SOLO LA BARRA
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
