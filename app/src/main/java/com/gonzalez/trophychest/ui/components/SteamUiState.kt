package com.gonzalez.trophychest.ui.components


//DEFINE LOS ESTADOS DE LA APP EN GENERAL , NO SOLO STEAM
sealed interface RemoteUiState<out T> {
    data object Loading : RemoteUiState<Nothing>//NO NECESITA NI TRAE DATOS
    data class Error(val message: String) : RemoteUiState<Nothing>//NECESITA UN MENSAJE Y NO TRAE NADA
    data class Empty(val message: String) : RemoteUiState<Nothing>//NECESITA UN MENSAJE Y NO TRAE NADA

    data class Success<T>(val data: T, val message: String? = null) : RemoteUiState<T>
    //CUANDO RECIBE RESULTADO LO GUARDA EN UN TIPO DATA  + MENSAJE SECUNDARIO
}
