# 02 - FirebaseManager.kt

## Resumen Para Tontos

Este archivo es la puerta principal a Firebase Authentication. Se encarga de iniciar sesion, registrar usuarios, entrar con Google, cerrar sesion y traducir errores tecnicos a mensajes entendibles.

Idea clave: `LoginScreen` no habla directamente con Firebase; habla con este manager.

## Estado

- `currentUser`: usuario actual de Firebase. Si es `null`, no hay sesion.
- No usa `remember` porque no es una pantalla Compose.
- No guarda estado visual; solo consulta Firebase Auth.

## Flujo

1. `login(email, password)` usa Firebase Auth para iniciar sesion.
2. Despues llama a `UserRepository.ensureUserDocument(user)` para asegurar que existe perfil en Firestore.
3. `register(email, password, username)` crea usuario en Auth.
4. Luego crea documento de usuario con `UserRepository.createUserDocument`.
5. Si falla la creacion del perfil, borra el usuario de Auth para no dejar una cuenta incompleta.
6. `loginWithGoogle` convierte el token de Google en sesion Firebase.
7. Si Google crea un usuario nuevo, exige `username`.
8. `logout(context)` limpia cache de Steam y cierra sesion.

## Firebase/API

- Usa `FirebaseAuth.getInstance()`.
- Llama a:
  - `signInWithEmailAndPassword`
  - `createUserWithEmailAndPassword`
  - `signInWithCredential`
  - `signOut`
- Usa `GoogleAuthProvider.getCredential(idToken, null)` para transformar token de Google en credencial Firebase.

## Kotlin Basico Que Aparece Aqui

- `object`: singleton, una unica instancia para toda la app.
- `suspend fun`: funcion que se ejecuta desde corrutina.
- `.await()`: convierte una tarea de Firebase en algo esperable desde corrutina.
- `runCatching`: captura errores sin hacer `try/catch` gigante.
- `mapError`: extension privada para transformar errores.
- `Result<T>`: devuelve exito o fallo sin lanzar directamente a la UI.

## Pregunta Para Defender

**Si me preguntan: Por que despues del login llamas a UserRepository?**

Responderia: "Firebase Auth solo sabe quien eres y si has iniciado sesion. Mi app necesita un perfil publico con username, avatar, codigo de amigo y otros datos. Por eso, despues de autenticar, aseguro el documento en Firestore."

**Si me preguntan: Que pasa si falla Firestore al registrar?**

Responderia: "Borro el usuario recien creado de Firebase Auth. Asi evito tener una cuenta autenticada pero sin perfil dentro de la app."

## Frase Para Decir En La Defensa

"FirebaseManager separa la autenticacion de la interfaz y deja los perfiles a UserRepository."

