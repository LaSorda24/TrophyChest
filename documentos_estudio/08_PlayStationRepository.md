# 08 - PlayStationRepository.kt

## Resumen Para Tontos

Este repositorio hace con PlayStation algo parecido a Steam, pero usando un proxy. Vincula cuenta con NPSSO, guarda sesion, descarga juegos, descarga titulos con trofeos y transforma todo a modelos comunes.

## Estado

- No usa estado Compose.
- Guarda cuenta, juegos, trophy games y logros en cache local.
- Usa `BuildConfig.PLAYSTATION_PROXY_BASE_URL`.
- Se separa por UID de Firebase.

## Flujo

1. `linkWithNpsso` recibe el NPSSO escrito por el usuario.
2. Llama al proxy para convertir NPSSO en sesion PlayStation.
3. Pide historial de juegos.
4. Pide titulos con trofeos.
5. Guarda cuenta, juegos y trophy games.
6. `getAchievementBundle` pide trofeos concretos de un juego.
7. `enrichWithIgdbDetails` intenta completar datos visuales con IGDB.

## Firebase/API

- Firebase: solo se usa UID para separar cache.
- Proxy PlayStation: autenticacion NPSSO, title history, trophy titles, achievements.
- IGDB: se usa como apoyo para completar detalles.
- SharedPreferences: cache local.

## Kotlin Basico Que Aparece Aqui

- `sealed class PlayStationSyncResult`: distintos resultados posibles.
- `withContext(Dispatchers.IO)`: red/cache fuera del hilo principal.
- `runCatching`: captura errores de proxy.
- `mapNotNull`: transforma y descarta elementos invalidos.
- `copy`: actualiza tokens en la cuenta vinculada.
- Funciones internas `internal fun`: visibles en modulo y tests.

## Pregunta Para Defender

**Si me preguntan: Por que PlayStation no se llama directamente desde Android?**

Responderia: "Porque el flujo de PlayStation es mas delicado. Uso un proxy para centralizar esa logica, no exponerla en Android y devolver a la app datos ya normalizados."

**Si me preguntan: Para que sirve el refresh token?**

Responderia: "Permite actualizar la sesion sin pedir al usuario que vincule otra vez, mientras siga siendo valido."

## Frase Para Decir En La Defensa

"PlayStationRepository adapta datos de PSN a los mismos modelos que usa el resto de la app."

