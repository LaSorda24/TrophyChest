package com.gonzalez.trophychest.ui.components

sealed interface RemoteUiState<out T> {
    data object Loading : RemoteUiState<Nothing>
    data class Error(val message: String) : RemoteUiState<Nothing>
    data class Empty(val message: String) : RemoteUiState<Nothing>
    data class Success<T>(val data: T, val message: String? = null) : RemoteUiState<T>
}
