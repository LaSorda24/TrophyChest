package com.gonzalez.trophychest.data

import com.google.firebase.Timestamp

const val CONNECTION_STATUS_PENDING = "pending"
const val CONNECTION_STATUS_ACCEPTED = "accepted"
const val CONNECTION_STATUS_REJECTED = "rejected"

data class UserProfile(
    val uid: String = "",
    val username: String? = null,
    val usernameLower: String? = null,
    val friendCode: String? = null,
    val profileImageId: Int = UserDefaults.MIN_IMAGE_ID,
    val bannerImageId: Int = UserDefaults.MIN_IMAGE_ID,
    val lastNameChange: Timestamp? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null
) {
    val visibleName: String
        get() = username?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: "Jugador"
}

data class FriendConnection(
    val id: String = "",
    val members: List<String> = emptyList(),
    val requestedBy: String = "",
    val requestedTo: String = "",
    val status: String = CONNECTION_STATUS_PENDING,
    val lastMessageText: String? = null,
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String? = null,
    val readAtByUid: Map<String, Timestamp> = emptyMap()
) {
    fun peerId(currentUid: String): String? = members.firstOrNull { it != currentUid }

    fun hasUnread(currentUid: String): Boolean {
        return ChatValidation.hasUnread(
            lastMessageAtMillis = lastMessageAt?.toDate()?.time,
            lastSenderId = lastSenderId,
            currentUid = currentUid,
            readAtMillis = readAtByUid[currentUid]?.toDate()?.time
        )
    }
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)
