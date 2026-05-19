# 09 - TrofeosScreen.kt

## Resumen Para Tontos

Esta pantalla muestra los juegos que tienen logros o trofeos. Puede mezclar Steam y PlayStation, y permite filtrar por plataforma o porcentaje minimo.

## Estado

- `achievementState`: carga, exito, vacio o error.
- `mostrarMenuFiltros`: abre/cierra filtros.
- `sliderValue`: valor temporal del filtro.
- `porcentajeAplicado`: filtro realmente aplicado.
- `plataformasSeleccionadas`: plataformas activas.
- `juegosFiltrados`: lista final tras aplicar filtros.

## Flujo

1. Comprueba cuentas vinculadas de Steam y PlayStation.
2. Si no hay cuentas, muestra mensaje.
3. Si hay cuentas, `produceState` llama a `AchievementRepository.getGamesWithAchievementSummaries`.
4. Si carga bien, pinta lista de juegos.
5. Si el usuario abre filtros, puede cambiar plataformas y porcentaje.
6. Al pulsar un juego, navega al detalle de trofeos.

## Firebase/API

- No llama directamente a Firebase.
- No llama directamente a Steam/PSN.
- Llama a `AchievementRepository`, que decide si usar SteamRepository y PlayStationRepository.
- Usa datos de cuentas/cache ya vinculadas.

## Kotlin Basico Que Aparece Aqui

- `produceState`: carga datos asincronos y los convierte en estado.
- `RemoteUiState`: sealed class para Loading, Error, Empty y Success.
- `remember`: memoriza filtros.
- `mutableFloatStateOf`: estado optimizado para Float.
- `filterGames`: logica separada para no meter filtros a mano en la UI.

## Pregunta Para Defender

**Si me preguntan: Como se actualiza la lista al cambiar filtros?**

Responderia: "Los filtros son estado Compose. Cuando cambia `porcentajeAplicado` o `plataformasSeleccionadas`, se recalcula `juegosFiltrados` con `remember`, y Compose vuelve a pintar la lista."

**Si me preguntan: Por que no llamas a Steam directamente desde la pantalla?**

Responderia: "Porque la pantalla solo debe pintar y gestionar interaccion. La logica de obtener logros esta en repositorios."

## Frase Para Decir En La Defensa

"TrofeosScreen mezcla datos ya preparados por repositorios y se centra en mostrar y filtrar."

