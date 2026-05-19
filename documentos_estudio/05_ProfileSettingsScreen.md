# 05 - ProfileSettingsScreen.kt

## Resumen Para Tontos

Esta pantalla permite cambiar datos del perfil y vincular plataformas. Es una pantalla critica porque conecta UI con `UserRepository`, `SteamRepository` y `PlayStationRepository`.

## Estado

- `profile`: usuario actual.
- `username`: texto editable del nombre.
- `selectedAvatar` y `selectedBanner`: imagenes elegidas.
- `isSaving`: bloquea acciones mientras se guarda.
- `message`: aviso o error que se muestra.
- `showSteamDialog`: abre dialogo para Steam.
- `showPlayStationDialog`: abre dialogo para PlayStation.
- `linkedAccount`: cuenta Steam guardada.
- `linkedPlayStationAccount`: cuenta PlayStation guardada.

## Flujo

1. Entra la pantalla y `LaunchedEffect` escucha `UserRepository.observeCurrentUser`.
2. Si cambia el perfil en Firestore, se actualiza `profile`, `username`, avatar y banner.
3. Guardar nombre llama a `UserRepository.updateUsername`.
4. Guardar imagenes llama a `UserRepository.updateProfileImages`.
5. Vincular Steam abre dialogo y llama a `SteamRepository.importLibrary`.
6. Vincular PlayStation abre dialogo y llama a `PlayStationRepository.linkWithNpsso`.
7. Desvincular borra datos locales de la plataforma correspondiente.

## Firebase/API

- Firestore: actualiza username, avatar y banner.
- Steam API: al vincular, descarga biblioteca de Steam.
- Proxy PlayStation: al vincular, envia NPSSO y descarga juegos/trofeos.
- No sube imagenes; solo guarda ids numericos.

## Kotlin Basico Que Aparece Aqui

- `remember { mutableStateOf(...) }`: estado de pantalla.
- `mutableIntStateOf`: estado optimizado para enteros.
- `LaunchedEffect`: arranca escucha de perfil.
- `scope.launch`: corrutina para guardar sin bloquear.
- `runCatching`: captura fallo y permite mostrar mensaje.
- `when (val result = ...)`: decide que mensaje mostrar segun el resultado.

## Pregunta Para Defender

**Si me preguntan: Como se actualiza la interfaz cuando se guarda el perfil?**

Responderia: "Cuando `UserRepository.updateUsername` o `updateProfileImages` devuelve el User actualizado, asigno ese valor a `profile`. Como `profile` es estado Compose, la pantalla se redibuja. Ademas `observeCurrentUser` escucha Firestore por si el cambio viene de la nube."

**Si me preguntan: Por que PlayStation necesita proxy?**

Responderia: "Porque el flujo de PlayStation es mas sensible y no quiero depender directamente de PSN desde Android. La app manda el NPSSO al proxy y recibe datos ya normalizados."

## Frase Para Decir En La Defensa

"ProfileSettingsScreen es la pantalla donde el usuario conecta su identidad de Firebase con sus plataformas externas."

