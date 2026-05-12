package com.gonzalez.trophychest.data

import java.util.Locale

object ChatValidation {
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 20
    const val MAX_MESSAGE_LENGTH = 1000

    private val usernamePattern = Regex("^[a-z0-9_]+$")

    fun normalizeUsername(input: String): String {
        return input.trim().lowercase(Locale.ROOT)
    }

    fun usernameError(input: String): String? {
        val username = normalizeUsername(input)
        return when {
            username.length < MIN_USERNAME_LENGTH -> "El nombre de usuario debe tener al menos 3 caracteres."
            username.length > MAX_USERNAME_LENGTH -> "El nombre de usuario no puede superar 20 caracteres."
            !usernamePattern.matches(username) -> "Usa solo letras, numeros y guion bajo."
            else -> null
        }
    }

    fun requireValidUsername(input: String): String {
        usernameError(input)?.let { throw IllegalArgumentException(it) }
        return normalizeUsername(input)
    }

    fun normalizeMessage(input: String): String {
        val text = input.trim()
        require(text.isNotEmpty()) { "Escribe un mensaje antes de enviarlo." }
        require(text.length <= MAX_MESSAGE_LENGTH) { "El mensaje no puede superar 1000 caracteres." }
        return text
    }

    fun pairIdFor(uidA: String, uidB: String): String {
        require(uidA.isNotBlank() && uidB.isNotBlank()) { "Los usuarios no pueden estar vacios." }
        require(uidA != uidB) { "No puedes crear un chat contigo mismo." }
        return listOf(uidA, uidB).sorted().joinToString("_")
    }

    fun hasUnread(
        lastMessageAtMillis: Long?,
        lastSenderId: String?,
        currentUid: String,
        readAtMillis: Long?
    ): Boolean {
        if (lastMessageAtMillis == null || lastSenderId == null) return false
        if (lastSenderId == currentUid) return false
        return readAtMillis == null || lastMessageAtMillis > readAtMillis
    }
}
