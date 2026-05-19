# 07 - SteamRepository.kt

## Resumen Para Tontos

Este es uno de los archivos mas importantes. Se encarga de todo lo relacionado con Steam: vincular cuenta, leer biblioteca, guardar cache, pedir detalles, pedir logros y transformar respuestas de Steam al modelo `Juego`.

## Estado

- No usa estado Compose.
- Usa `SharedPreferences` para cache local.
- La cache va separada por UID de Firebase para que un usuario no vea datos de otro.
- Usa `BuildConfig.STEAM_API_KEY` para saber si Steam esta configurado.

## Flujo

1. `importLibrary` recibe SteamID o URL.
2. `resolveProfileInput` convierte ese texto en SteamID64.
3. Llama a `RetrofitInstance.steamApi.getOwnedGames`.
4. Convierte juegos Steam a `Juego`.
5. Guarda cuenta y biblioteca en cache.
6. `getGameDetails` intenta cache y luego Steam Store.
7. `getAchievementBundle` junta logros del usuario, esquema del juego y porcentajes globales.
8. `getGamesWithAchievementSummaries` prepara datos para Trofeos.

## Firebase/API

- Firebase: solo usa `FirebaseManager.currentUser?.uid` para separar cache.
- Steam Web API: biblioteca, perfil, logros.
- Steam Store: detalles, categorias, reviews, imagenes.
- SharedPreferences: cache de cuenta, juegos, detalles y logros.

## Kotlin Basico Que Aparece Aqui

- `object`: singleton.
- `withContext(Dispatchers.IO)`: trabajo pesado fuera del hilo principal.
- `coroutineScope`, `async`, `awaitAll`: varias llamadas en paralelo.
- `Semaphore`: limita cuantas llamadas se hacen a la vez.
- `Gson`: convierte objetos a JSON para guardarlos.
- `TypeToken`: permite leer listas genericas desde JSON.
- Extension functions como `SteamGame.toJuego()`.

## Pregunta Para Defender

**Si me preguntan: Por que hay tanta cache?**

Responderia: "Steam devuelve muchos datos y algunas llamadas pueden tardar. Guardo cuenta, juegos, detalles y logros para que la app sea mas rapida y no dependa siempre de internet."

**Si me preguntan: Como evitas mezclar datos entre usuarios?**

Responderia: "Las claves de SharedPreferences se construyen con el UID de Firebase. Por eso la cache de Steam queda separada por usuario."

## Frase Para Decir En La Defensa

"SteamRepository es la capa que convierte las APIs de Steam en datos simples que mis pantallas pueden pintar."

