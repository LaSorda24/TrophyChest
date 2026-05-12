package com.gonzalez.trophychest.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// APUNTE: EL CHAT USA FIRESTORE EN TIEMPO REAL CON SOLICITUDES DE AMISTAD Y MENSAJES.
object ChatRepository {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val uid: String
        get() = FirebaseManager.currentUser?.uid ?: error("Inicia sesion para usar el chat.")

    suspend fun ensureUserProfile(): UserProfile {
        val user = FirebaseManager.currentUser ?: error("Inicia sesion para usar el chat.")
        return UserRepository.ensureUserDocument(user).let {
            UserProfile(
                uid = it.uid,
                username = it.username,
                usernameLower = it.usernameLower,
                friendCode = it.friendCode,
                profileImageId = it.profileImageId,
                bannerImageId = it.bannerImageId,
                lastNameChange = it.lastNameChange,
                displayName = it.displayName,
                photoUrl = it.photoUrl,
                createdAt = it.createdAt
            )
        }
    }

    suspend fun reserveUsername(usernameInput: String): UserProfile {
        val currentUid = uid
        val username = ChatValidation.requireValidUsername(usernameInput)
        val userRef = db.collection("users").document(currentUid)
        val usernameRef = db.collection("usernames").document(username)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentUsername = userSnapshot.getString("usernameLower")
            if (!currentUsername.isNullOrBlank() && currentUsername != username) {
                throw IllegalStateException("Tu cuenta ya tiene un codigo de usuario.")
            }

            val usernameSnapshot = transaction.get(usernameRef)
            val ownerUid = usernameSnapshot.getString("uid")
            if (usernameSnapshot.exists() && ownerUid != currentUid) {
                throw IllegalStateException("Ese codigo ya esta en uso.")
            }

            val reservation = mapOf(
                "uid" to currentUid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val profilePatch = mapOf(
                "uid" to currentUid,
                "username" to username,
                "usernameLower" to username
            )

            transaction.set(usernameRef, reservation)
            transaction.set(userRef, profilePatch, com.google.firebase.firestore.SetOptions.merge())
            null
        }.await()

        return getUserProfile(currentUid) ?: UserProfile(uid = currentUid, username = username, usernameLower = username)
    }

    suspend fun findUserByUsername(usernameInput: String): UserProfile? {
        val username = ChatValidation.requireValidUsername(usernameInput)
        val reservation = db.collection("usernames").document(username).get().await()
        val targetUid = reservation.getString("uid") ?: return null
        return getUserProfile(targetUid)
    }

    suspend fun findUserByFriendCode(friendCodeInput: String): UserProfile? {
        return UserRepository.findUserByFriendCode(friendCodeInput)
    }

    suspend fun getUserProfile(targetUid: String): UserProfile? {
        return UserRepository.getUserProfile(targetUid)
    }

    suspend fun sendFriendRequest(targetUid: String): String {
        val currentUid = uid
        val pairId = ChatValidation.pairIdFor(currentUid, targetUid)
        val connectionRef = db.collection("connections").document(pairId)

        db.runTransaction { transaction ->
            val existing = transaction.get(connectionRef)
            if (existing.exists()) {
                when (existing.getString("status")) {
                    CONNECTION_STATUS_ACCEPTED -> throw IllegalStateException("Ya sois amigos.")
                    CONNECTION_STATUS_PENDING -> throw IllegalStateException("Ya existe una solicitud pendiente.")
                    else -> Unit
                }
            }

            val members = listOf(currentUid, targetUid).sorted()
            val data = mapOf(
                "members" to members,
                "requestedBy" to currentUid,
                "requestedTo" to targetUid,
                "status" to CONNECTION_STATUS_PENDING,
                "createdAt" to FieldValue.serverTimestamp(),
                "readAtByUid" to mapOf(currentUid to FieldValue.serverTimestamp())
            )
            transaction.set(connectionRef, data)
            null
        }.await()

        return pairId
    }

    suspend fun acceptConnection(pairId: String) {
        val currentUid = uid
        val connectionRef = db.collection("connections").document(pairId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(connectionRef)
            if (!snapshot.exists()) throw IllegalStateException("La solicitud ya no existe.")
            if (snapshot.getString("requestedTo") != currentUid) {
                throw IllegalStateException("Solo el receptor puede aceptar esta solicitud.")
            }
            if (snapshot.getString("status") != CONNECTION_STATUS_PENDING) {
                throw IllegalStateException("La solicitud ya no esta pendiente.")
            }

            transaction.update(
                connectionRef,
                mapOf(
                    "status" to CONNECTION_STATUS_ACCEPTED,
                    "acceptedAt" to FieldValue.serverTimestamp(),
                    "readAtByUid.$currentUid" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
    }

    suspend fun rejectConnection(pairId: String) {
        val currentUid = uid
        val connectionRef = db.collection("connections").document(pairId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(connectionRef)
            if (!snapshot.exists()) throw IllegalStateException("La solicitud ya no existe.")
            if (snapshot.getString("requestedTo") != currentUid) {
                throw IllegalStateException("Solo el receptor puede rechazar esta solicitud.")
            }

            transaction.update(
                connectionRef,
                mapOf(
                    "status" to CONNECTION_STATUS_REJECTED,
                    "rejectedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
    }

    fun observeConnections(): Flow<List<FriendConnection>> = callbackFlow {
        val currentUid = uid
        val registration = db.collection("connections")
            .whereArrayContains("members", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val connections = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        document.toObject(FriendConnection::class.java)?.copy(id = document.id)
                    }
                    .filter { it.status != CONNECTION_STATUS_REJECTED }
                    .sortedWith(
                        compareByDescending<FriendConnection> { it.lastMessageAt?.toDate()?.time ?: 0L }
                            .thenBy { it.status }
                    )
                trySend(connections)
            }

        awaitClose { registration.remove() }
    }

    fun observeConnection(pairId: String): Flow<FriendConnection?> = callbackFlow {
        val registration = db.collection("connections")
            .document(pairId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val connection = snapshot
                    ?.toObject(FriendConnection::class.java)
                    ?.copy(id = snapshot.id)
                trySend(connection)
            }

        awaitClose { registration.remove() }
    }

    fun observeMessages(pairId: String, limit: Long = 50): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection("connections")
            .document(pairId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        document.toObject(ChatMessage::class.java)?.copy(id = document.id)
                    }
                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(pairId: String, textInput: String) {
        val currentUid = uid
        val text = ChatValidation.normalizeMessage(textInput)
        val connectionRef = db.collection("connections").document(pairId)
        val messageRef = connectionRef.collection("messages").document()

        val batch = db.batch()
        batch.set(
            messageRef,
            mapOf(
                "senderId" to currentUid,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            connectionRef,
            mapOf(
                "lastMessageText" to text,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to currentUid,
                "readAtByUid.$currentUid" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }

    suspend fun markChatRead(pairId: String) {
        val currentUid = uid
        db.collection("connections")
            .document(pairId)
            .update("readAtByUid.$currentUid", FieldValue.serverTimestamp())
            .await()
    }
}
