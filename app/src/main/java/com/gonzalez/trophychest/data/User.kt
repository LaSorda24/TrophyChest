package com.gonzalez.trophychest.data

import com.google.firebase.Timestamp
import kotlin.random.Random

data class User(
    val uid: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val friendCode: String = "",
    val profileImageId: Int = UserDefaults.MIN_IMAGE_ID,
    val bannerImageId: Int = UserDefaults.MIN_IMAGE_ID,
    val lastNameChange: Timestamp? = null,
    val savedGames: List<String> = emptyList(),
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null
)

object UserDefaults {
    const val FRIEND_CODE_LENGTH = 6
    const val MIN_IMAGE_ID = 1
    const val MAX_IMAGE_ID = 5

    private const val FRIEND_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun generateFriendCode(random: Random = Random.Default): String {
        return buildString {
            repeat(FRIEND_CODE_LENGTH) {
                append(FRIEND_CODE_ALPHABET[random.nextInt(FRIEND_CODE_ALPHABET.length)])
            }
        }
    }

    fun randomImageId(random: Random = Random.Default): Int {
        return random.nextInt(MIN_IMAGE_ID, MAX_IMAGE_ID + 1)
    }

    fun normalizeFriendCode(input: String): String {
        return input.trim().uppercase().filter { it.isLetterOrDigit() }
    }

    fun friendCodeError(input: String): String? {
        val friendCode = normalizeFriendCode(input)
        return when {
            friendCode.length != FRIEND_CODE_LENGTH -> "El codigo de amigo debe tener 6 caracteres."
            !friendCode.all { it in FRIEND_CODE_ALPHABET } -> "Usa solo letras y numeros."
            else -> null
        }
    }
}
