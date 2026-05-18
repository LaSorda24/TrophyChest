package com.gonzalez.trophychest.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class GoogleAuthResult(
    val user: FirebaseUser,
    val isNewUser: Boolean
)

// ESTE OBJECT ES UN SINGLETON TODA LA APP USA LA MISMA PUERTA DE ENTRADA A FIREBASE AUTH.
object FirebaseManager {
    // CURRENTUSER ES LA SESION ACTUAL SI ES NULL, NADIE HA INICIADO SESION.
    val currentUser: FirebaseUser?
        get() = runCatching { auth().currentUser }.getOrNull()

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        // SUSPEND SIGNIFICA QUE SE LLAMA DESDE CORRUTINA PORQUE FIREBASE TRABAJA EN RED.
        return runCatching {
            val user = auth().signInWithEmailAndPassword(email.trim(), password).await().user
                ?: error("No se pudo recuperar el usuario autenticado.")
            // DESPUES DEL LOGIN ME ASEGURO DE QUE EXISTE EL DOCUMENTO DEL USUARIO EN FIRESTORE.
            UserRepository.ensureUserDocument(user)
            user
        }.mapError()
    }

    suspend fun register(email: String, password: String, username: String): Result<FirebaseUser> {
        return runCatching {
            // PRIMERO VALIDO EL USERNAME Y LUEGO CREO AUTH + DOCUMENTO DE FIRESTORE.
            ChatValidation.requireValidUsername(username)
            val user = auth().createUserWithEmailAndPassword(email.trim(), password).await().user
                ?: error("No se pudo crear la cuenta.")
            try {
                UserRepository.createUserDocument(user, username)
            } catch (throwable: Throwable) {
                // SI FALLA FIRESTORE, BORRO EL USUARIO DE AUTH PARA NO DEJAR UNA CUENTA A MEDIAS.
                runCatching { user.delete().await() }
                throw throwable
            }
            user
        }.mapError()
    }

    suspend fun loginWithGoogle(idToken: String, username: String? = null): Result<GoogleAuthResult> {
        return runCatching {
            // GOOGLE DEVUELVE UN TOKEN; FIREBASE LO CONVIERTE EN UNA SESION REAL.
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth().signInWithCredential(credential).await()
            val user = authResult.user ?: error("No se pudo iniciar sesion con Google.")
            val isNewUser = authResult.additionalUserInfo?.isNewUser == true

            if (isNewUser) {
                // SI ES USUARIO NUEVO, OBLIGO A TENER USERNAME PARA PODER CREAR PERFIL PUBLICO.
                if (username.isNullOrBlank()) {
                    runCatching { user.delete().await() }
                    runCatching { auth().signOut() }
                    error("Usa Crear una cuenta nueva y elige un nombre de usuario antes de continuar con Google.")
                }
                try {
                    UserRepository.createUserDocument(user, username)
                } catch (throwable: Throwable) {
                    runCatching { user.delete().await() }
                    runCatching { auth().signOut() }
                    throw throwable
                }
            } else {
                UserRepository.ensureUserDocument(user)
            }

            GoogleAuthResult(user = user, isNewUser = isNewUser)
        }.mapError()
    }

    fun logout() {
        runCatching { auth().signOut() }
    }


    private fun auth(): FirebaseAuth {
        // ESTA FUNCION CENTRALIZA FIREBASEAUTH PARA CAPTURAR ERRORES DE CONFIGURACION.
        return runCatching { FirebaseAuth.getInstance() }
            .getOrElse { throwable ->
                throw IllegalStateException(
                    "Firebase no esta configurado. Anade google-services.json y habilita Authentication en Firebase Console.",
                    throwable
                )
            }
    }

    private fun <T> Result<T>.mapError(): Result<T> {
        // MAPERROR TRADUCE ERRORES TECNICOS DE FIREBASE A MENSAJES ENTENDIBLES PARA LA APP.
        return exceptionOrNull()?.let { Result.failure(mapFirebaseException(it)) } ?: this
    }

    private fun mapFirebaseException(throwable: Throwable): Throwable {
        val message = when (throwable) {
            is FirebaseAuthUserCollisionException -> "Ese correo ya esta registrado."
            is FirebaseAuthInvalidCredentialsException -> "Las credenciales no son validas."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
            is FirebaseAuthException -> when (throwable.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "La contrasena debe tener al menos 6 caracteres."
                "ERROR_NETWORK_REQUEST_FAILED" -> "No se pudo conectar con Firebase. Revisa tu internet."
                else -> throwable.localizedMessage ?: "Ha ocurrido un error de autenticacion."
            }
            is UserRepository.FriendCodeCollisionException -> "No se pudo generar un codigo de amigo unico. Intentalo de nuevo."
            else -> throwable.localizedMessage ?: "Ha ocurrido un error inesperado."
        }

        return IllegalStateException(message, throwable)
    }
}

