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
    imageUrls: List<String?>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderLabel: String = "Steam"
) {
    val validUrls = remember(imageUrls) {
        imageUrls
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    var selectedIndex by remember(validUrls) { mutableIntStateOf(0) }

    Box(
        modifier = modifier.background(Color(0xFF202833)),
        contentAlignment = Alignment.Center
    ) {
        if (validUrls.isNotEmpty()) {
            AsyncImage(
                model = validUrls[selectedIndex],
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    if (selectedIndex < validUrls.lastIndex) {
                        selectedIndex += 1
                    }
                }
            )
        } else {
            Text(
                text = placeholderLabel,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
