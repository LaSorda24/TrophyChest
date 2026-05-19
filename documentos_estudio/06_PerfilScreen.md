# 06 - PerfilScreen.kt

## Resumen Para Tontos

Esta pantalla muestra el perfil ya creado: nombre, avatar, banner, codigo de amigo, estadisticas, amigos, juegos guardados y plataformas vinculadas.

## Estado

- `profile`: datos actuales del usuario desde Firestore.
- `syncMessage`: error o aviso si algo no carga.
- `friendConnections`: lista de amistades desde Firestore.
- `savedGames`: juegos guardados localmente.
- `profileStats`: calculo de estadisticas con juegos y amigos.
- `pagerState`: controla las pestanas/paginas internas del perfil.

## Flujo

1. Carga usuario actual desde `FirebaseManager.currentUser`.
2. Lee caches locales de Steam y PlayStation.
3. `LaunchedEffect` escucha perfil en `UserRepository`.
4. Otro `LaunchedEffect` escucha amigos en `ChatRepository`.
5. `DisposableEffect` recarga guardados al volver a la pantalla.
6. Boton ajustes navega a `ProfileSettings`.
7. Boton logout llama a `FirebaseManager.logout`.

## Firebase/API

- Firestore: perfil y conexiones de amigos en tiempo real.
- SharedPreferences/cache local: juegos de Steam, PlayStation y guardados.
- No llama directamente a Steam/PSN; usa datos ya sincronizados.

## Kotlin Basico Que Aparece Aqui

- `rememberPagerState`: estado del paginador.
- `LaunchedEffect(currentUser?.uid)`: se reinicia si cambia usuario.
- `collect`: recibe emisiones de un Flow.
- `DisposableEffect`: registra y limpia un observer del ciclo de vida.
- `takeIf`: usa un valor solo si cumple condicion.
- `?:`: operador Elvis, valor por defecto si es null.

## Pregunta Para Defender

**Si me preguntan: Como se actualiza el perfil al cambiar datos en ajustes?**

Responderia: "PerfilScreen escucha el documento del usuario con `UserRepository.observeCurrentUser`. Cuando Firestore cambia, el Flow emite el nuevo perfil, se actualiza `profile` y Compose redibuja la pantalla."

**Si me preguntan: Por que usa cache local para juegos?**

Responderia: "Porque no necesito llamar a Steam o PlayStation cada vez que entro en Perfil. La sincronizacion se hace al vincular o actualizar, y Perfil pinta lo guardado."

## Frase Para Decir En La Defensa

"PerfilScreen es principalmente una pantalla de lectura: muestra datos de Firebase y caches locales ya sincronizadas."

