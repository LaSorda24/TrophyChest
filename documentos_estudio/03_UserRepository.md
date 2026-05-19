# 03 - UserRepository.kt

## Resumen Para Tontos

Este archivo gestiona el perfil del usuario en Firestore. Firebase Auth solo da la cuenta; este repositorio guarda el perfil real de TrophyChest: username, codigo de amigo, avatar, banner, juegos guardados y datos publicos.

## Estado

- No tiene `remember`, porque no es UI.
- Usa Firestore como fuente de verdad del perfil.
- `observeCurrentUser()` devuelve un `Flow<User?>`, que permite escuchar cambios en tiempo real.

## Flujo

1. `observeCurrentUser` escucha el documento `users/{uid}`.
2. `getUser` lee un usuario concreto.
3. `ensureUserDocument` comprueba si el usuario ya tiene perfil; si falta algo, lo completa.
4. `createUserDocument` crea perfil nuevo y reserva username/friendCode.
5. `updateUsername` cambia el nombre usando transaccion.
6. `updateProfileImages` guarda ids de avatar y banner.
7. `findUserByFriendCode` busca otro usuario por codigo de amigo.

## Firebase/API

Colecciones importantes:

- `users`: perfil publico del usuario.
- `usernames`: reserva de nombres unicos.
- `friendCodes`: reserva de codigos de amigo unicos.

Usa transacciones para evitar duplicados. Esto es importante porque dos usuarios podrian intentar usar el mismo nombre a la vez.

## Kotlin Basico Que Aparece Aqui

- `Flow`: flujo de datos que puede emitir varias veces.
- `callbackFlow`: convierte listener de Firestore en Flow.
- `awaitClose`: limpia el listener cuando ya no se usa.
- `repeat(MAX_ATTEMPTS)`: intenta varias veces generar codigo unico.
- `data class User`: estructura de datos simple.
- `copy(...)`: crea una copia cambiando algunos campos.

## Pregunta Para Defender

**Si me preguntan: Como se actualiza el perfil sin recargar manualmente?**

Responderia: "Uso `observeCurrentUser`, que mantiene un listener de Firestore. Cuando cambia el documento del usuario, el Flow emite un nuevo User y la pantalla que lo recoge actualiza su estado."

**Si me preguntan: Por que usas transacciones?**

Responderia: "Porque username y friendCode deben ser unicos. La transaccion permite comprobar y escribir como una operacion segura."

## Frase Para Decir En La Defensa

"UserRepository es la capa que convierte la cuenta de Firebase en un perfil util para mi aplicacion."

