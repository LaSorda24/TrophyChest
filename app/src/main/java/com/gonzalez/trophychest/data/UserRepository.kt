package com.gonzalez.trophychest.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

// ESTE REPOSITORIO CREA Y MANTIENE EL PERFIL PUBLICO DEL USUARIO EN FIRESTORE.
// USERREPOSITORY ES EL PUENTE ENTRE LA APP Y FIRESTORE PARA EL PERFIL DEL USUARIO.
object UserRepository {
    private const val MAX_FRIEND_CODE_ATTEMPTS = 8
    private const val MAX_USERNAME_ATTEMPTS = 8

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        // CALLBACKFLOW CONVIERTE EL LISTENER EN TIEMPO REAL DE FIRESTORE EN UN FLOW DE KOTLIN.
        val uid = FirebaseManager.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot
                    ?.toObject(User::class.java)
                    ?.copy(uid = snapshot.id)
                trySend(user)
            }

        awaitClose { registration.remove() }
    }

    suspend fun getUser(uid: String): User? {
        if (uid.isBlank()) return null
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return getUser(uid)?.toUserProfile()
    }

    //FUNCION JACOB completeExistingUserDocument.
    suspend fun ensureUserDocument(user: FirebaseUser): User {
        // ESTA FUNCION EVITA QUE UN LOGIN ENTRE SIN PERFIL PUBLICO CREADO EN FIRESTORE.
        getUser(user.uid)?.let { existing ->
            if (existing.usernameLower.isNotBlank() && existing.friendCode.isNotBlank()) {
                return existing
            }
            val username = existing.usernameLower
                .takeIf { it.isNotBlank() }
                ?: existing.username.takeIf { it.isNotBlank() }
                ?: fallbackUsernameBase(user)
            return completeExistingUserDocument(user, username)
        }

        val base = fallbackUsernameBase(user)
        repeat(MAX_USERNAME_ATTEMPTS) { attempt ->
            val username = if (attempt == 0) {
                base
            } else {
                "${base}_${user.uid.takeLast(4).lowercase(Locale.ROOT)}$attempt"
                    .take(ChatValidation.MAX_USERNAME_LENGTH)
                    .trimEnd('_')
            }
            val result = runCatching { createUserDocument(user, username) }
            if (result.isSuccess) return result.getOrThrow()
            val message = result.exceptionOrNull()?.message.orEmpty()
            if (!message.contains("usuario", ignoreCase = true) &&
                !message.contains("username", ignoreCase = true) &&
                !message.contains("nombre", ignoreCase = true)
            ) {
                throw result.exceptionOrNull() ?: IllegalStateException("No se pudo crear el perfil.")
            }
        }

        return createUserDocument(user, "${base}_${user.uid.takeLast(6).lowercase(Locale.ROOT)}")
    }

    //FUNCION JACOB
    private suspend fun completeExistingUserDocument(user: FirebaseUser, usernameInput: String): User {
        // COMPLETA PERFILES ANTIGUOS QUE NO TENIAN USERNAME O CODIGO DE AMIGO.
        val username = ChatValidation.requireValidUsername(usernameInput)
        repeat(MAX_FRIEND_CODE_ATTEMPTS) { attempt ->
            val friendCode = UserDefaults.generateFriendCode()
            try {
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(user.uid)
                    val usernameRef = db.collection("usernames").document(username)
                    val friendCodeRef = db.collection("friendCodes").document(friendCode)

                    val userSnapshot = transaction.get(userRef)
                    if (!userSnapshot.exists()) {
                        throw IllegalStateException("No se encontro tu perfil.")
                    }

                    val existingFriendCode = userSnapshot.getString("friendCode").orEmpty()
                    val resolvedFriendCode = existingFriendCode.ifBlank { friendCode }
                    val resolvedFriendCodeRef = db.collection("friendCodes").document(resolvedFriendCode)
                    val friendCodeSnapshot = transaction.get(resolvedFriendCodeRef)
                    val friendCodeOwner = friendCodeSnapshot.getString("uid")
                    if (friendCodeSnapshot.exists() && friendCodeOwner != user.uid) {
                        throw FriendCodeCollisionException()
                    }

                    val usernameSnapshot = transaction.get(usernameRef)
                    val usernameOwner = usernameSnapshot.getString("uid")
                    if (usernameSnapshot.exists() && usernameOwner != user.uid) {
                        throw IllegalStateException("Ese nombre de usuario ya esta en uso.")
                    }

                    if (!usernameSnapshot.exists()) {
                        transaction.set(
                            usernameRef,
                            mapOf(
                                "uid" to user.uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }

                    if (!friendCodeSnapshot.exists()) {
                        transaction.set(
                            resolvedFriendCodeRef,
                            mapOf(
                                "uid" to user.uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }

                    transaction.set(
                        userRef,
                        mapOf(
                            "uid" to user.uid,
                            "username" to username,
                            "usernameLower" to username,
                            "friendCode" to resolvedFriendCode,
                            "profileImageId" to (userSnapshot.getLong("profileImageId")?.toInt() ?: UserDefaults.randomImageId()),
                            "bannerImageId" to (userSnapshot.getLong("bannerImageId")?.toInt() ?: UserDefaults.randomImageId()),
                            "lastNameChange" to (userSnapshot.getTimestamp("lastNameChange") ?: FieldValue.serverTimestamp()),
                            "savedGames" to (userSnapshot.get("savedGames") ?: emptyList<String>()),
                            "displayName" to (userSnapshot.getString("displayName") ?: user.displayName ?: username),
                            "photoUrl" to (userSnapshot.getString("photoUrl") ?: user.photoUrl?.toString()),
                            "createdAt" to (userSnapshot.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
                        ),
                        SetOptions.merge()
                    )
                    null
                }.await()

                return getUser(user.uid) ?: error("No se pudo cargar el perfil actualizado.")
            } catch (throwable: FriendCodeCollisionException) {
                if (attempt == MAX_FRIEND_CODE_ATTEMPTS - 1) throw throwable
            }
        }

        error("No se pudo generar un codigo de amigo unico.")
    }

    suspend fun createUserDocument(user: FirebaseUser, usernameInput: String): User {
        // CREAR USUARIO NO ES SOLO GUARDAR DATOS; TAMBIEN RESERVO USERNAME Y FRIENDCODE UNICOS.
        val username = ChatValidation.requireValidUsername(usernameInput)
        val displayName = user.displayName
            ?: user.email?.substringBefore("@")
            ?: username

        repeat(MAX_FRIEND_CODE_ATTEMPTS) { attempt ->
            val friendCode = UserDefaults.generateFriendCode()
            val profileImageId = UserDefaults.randomImageId()
            val bannerImageId = UserDefaults.randomImageId()

            try {
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(user.uid)
                    val usernameRef = db.collection("usernames").document(username)
                    val friendCodeRef = db.collection("friendCodes").document(friendCode)

                    val userSnapshot = transaction.get(userRef)
                    if (userSnapshot.exists()) {
                        throw IllegalStateException("Tu perfil ya esta creado.")
                    }

                    val usernameSnapshot = transaction.get(usernameRef)
                    val usernameOwner = usernameSnapshot.getString("uid")
                    if (usernameSnapshot.exists() && usernameOwner != user.uid) {
                        throw IllegalStateException("Ese nombre de usuario ya esta en uso.")
                    }

                    val friendCodeSnapshot = transaction.get(friendCodeRef)
                    if (friendCodeSnapshot.exists()) {
                        throw FriendCodeCollisionException()
                    }

                    transaction.set(
                        userRef,
                        mapOf(
                            "uid" to user.uid,
                            "username" to username,
                            "usernameLower" to username,
                            "friendCode" to friendCode,
                            "profileImageId" to profileImageId,
                            "bannerImageId" to bannerImageId,
                            "lastNameChange" to FieldValue.serverTimestamp(),
                            "savedGames" to emptyList<String>(),
                            "displayName" to displayName,
                            "photoUrl" to user.photoUrl?.toString(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (!usernameSnapshot.exists()) {
                        transaction.set(
                            usernameRef,
                            mapOf(
                                "uid" to user.uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    transaction.set(
                        friendCodeRef,
                        mapOf(
                            "uid" to user.uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    null
                }.await()

                return User(
                    uid = user.uid,
                    username = username,
                    usernameLower = username,
                    friendCode = friendCode,
                    profileImageId = profileImageId,
                    bannerImageId = bannerImageId,
                    savedGames = emptyList(),
                    displayName = displayName,
                    photoUrl = user.photoUrl?.toString()
                )
            } catch (throwable: FriendCodeCollisionException) {
                if (attempt == MAX_FRIEND_CODE_ATTEMPTS - 1) throw throwable
            }
        }

        error("No se pudo generar un codigo de amigo unico.")
    }

    suspend fun updateUsername(usernameInput: String): User {
        // EL CAMBIO DE NOMBRE USA TRANSACCION PARA NO DUPLICAR USERNAMES ENTRE USUARIOS.
        val currentUser = FirebaseManager.currentUser ?: error("Inicia sesion para editar tu perfil.")
        val username = ChatValidation.requireValidUsername(usernameInput)
        val userRef = db.collection("users").document(currentUser.uid)
        val usernameRef = db.collection("usernames").document(username)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            if (!userSnapshot.exists()) throw IllegalStateException("No se encontro tu perfil.")

            val currentUsername = userSnapshot.getString("usernameLower")
            if (currentUsername == username) return@runTransaction null

            val usernameSnapshot = transaction.get(usernameRef)
            val ownerUid = usernameSnapshot.getString("uid")
            if (usernameSnapshot.exists() && ownerUid != currentUser.uid) {
                throw IllegalStateException("Ese nombre de usuario ya esta en uso.")
            }

            if (!usernameSnapshot.exists()) {
                transaction.set(
                    usernameRef,
                    mapOf(
                        "uid" to currentUser.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            transaction.update(
                userRef,
                mapOf(
                    "username" to username,
                    "usernameLower" to username,
                    "lastNameChange" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()

        return getUser(currentUser.uid) ?: error("No se pudo cargar el perfil actualizado.")
    }

    suspend fun updateProfileImages(profileImageId: Int, bannerImageId: Int): User {
        // AVATAR Y BANNER SON IDS NUMERICOS, NO IMAGENES SUBIDAS; ESO SIMPLIFICA FIRESTORE.
        val uid = FirebaseManager.currentUser?.uid ?: error("Inicia sesion para editar tu perfil.")
        require(profileImageId in UserDefaults.MIN_IMAGE_ID..UserDefaults.MAX_IMAGE_ID) {
            "Avatar no valido."
        }
        require(bannerImageId in UserDefaults.MIN_IMAGE_ID..UserDefaults.MAX_IMAGE_ID) {
            "Banner no valido."
        }

        db.collection("users").document(uid)
            .update(
                mapOf(
                    "profileImageId" to profileImageId,
                    "bannerImageId" to bannerImageId
                )
            )
            .await()

        return getUser(uid) ?: error("No se pudo cargar el perfil actualizado.")
    }

    suspend fun findUserByFriendCode(friendCodeInput: String): UserProfile? {
        // BUSCAMOS PRIMERO EN FRIENDCODES PARA ENCONTRAR RAPIDO EL UID DEL OTRO USUARIO.
        UserDefaults.friendCodeError(friendCodeInput)?.let { throw IllegalArgumentException(it) }
        val friendCode = UserDefaults.normalizeFriendCode(friendCodeInput)
        val reservation = db.collection("friendCodes").document(friendCode).get().await()
        val targetUid = reservation.getString("uid") ?: return null
        return getUserProfile(targetUid)
    }

    private fun fallbackUsernameBase(user: FirebaseUser): String {
        val raw = user.displayName
            ?: user.email?.substringBefore("@")
            ?: "jugador"
        val sanitized = raw
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return sanitized
            .ifBlank { "jugador" }
            .take(ChatValidation.MAX_USERNAME_LENGTH - 3)
            .let {
                if (it.length >= ChatValidation.MIN_USERNAME_LENGTH) it else "${it}123".take(ChatValidation.MIN_USERNAME_LENGTH)
            }
    }

    private fun User.toUserProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            username = username,
            usernameLower = usernameLower,
            friendCode = friendCode,
            profileImageId = profileImageId,
            bannerImageId = bannerImageId,
            lastNameChange = lastNameChange,
            displayName = displayName,
            photoUrl = photoUrl,
            createdAt = createdAt
        )
    }

    class FriendCodeCollisionException : IllegalStateException("No se pudo generar un codigo de amigo unico.")
}
