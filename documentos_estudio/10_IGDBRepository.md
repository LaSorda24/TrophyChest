# 10 - IGDBRepository.kt

## Resumen Para Tontos

Este repositorio gestiona IGDB, que es el catalogo de videojuegos. Sirve para buscar, explorar, ver categorias y cargar detalles de juegos. IGDB no guarda usuarios ni trofeos.

## Estado

- No usa estado Compose.
- Usa cache local para explorar.
- Usa `BuildConfig.IGDB_PROXY_BASE_URL` o `RELEASE_CALENDAR_BASE_URL`.
- Tiene lista fija de categorias visibles en la app.

## Flujo

1. `searchGames` recibe texto del buscador.
2. Llama al proxy con `/search-games`.
3. Mapea DTOs de IGDB a modelos de la app.
4. `getExploreContent` calcula generos recomendados segun cache de Steam/PS.
5. Si hay cache fresca, devuelve cache.
6. Si no, llama al proxy.
7. `getGameDetails` pide detalle de juego y completa datos con Steam si existe `steamAppId`.

## Firebase/API

- No usa Firebase directamente.
- Usa cache local.
- Llama al proxy IGDB, no a IGDB directo.
- El proxy habla con IGDB/Twitch.

## Kotlin Basico Que Aparece Aqui

- `Result<List<IGDBGame>>`: puede devolver lista o error.
- `withContext(Dispatchers.IO)`: llamadas de red/cache.
- `mapNotNull`: transforma solo elementos validos.
- `internal fun`: funciones que se pueden testear dentro del modulo.
- `takeIf`: usa un valor solo si cumple condicion.
- `copy`: crea version modificada de `Juego`.

## Pregunta Para Defender

**Si me preguntan: Por que IGDB pasa por proxy?**

Responderia: "Porque IGDB usa credenciales de Twitch y no quiero meter esa logica directamente en Android. La app llama a mi proxy y recibe datos ya adaptados."

**Si me preguntan: Como personalizas Explorar?**

Responderia: "Uso los juegos cacheados de Steam y PlayStation para sacar generos frecuentes y enviarlos como filtro al proxy de IGDB."

## Frase Para Decir En La Defensa

"IGDBRepository es el repositorio de catalogo: busqueda, categorias, explorar y detalles."

