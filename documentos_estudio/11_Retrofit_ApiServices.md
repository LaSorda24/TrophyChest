# 11 - RetrofitInstance, SteamApiService e IGDBProxyApiService

## Resumen Para Tontos

Estos archivos definen como la app llama a internet. `RetrofitInstance` crea los clientes HTTP. `SteamApiService` declara endpoints de Steam. `IGDBProxyApiService` declara endpoints del proxy.

## Estado

- No hay estado Compose.
- `retrofitByBaseUrl`: cache de clientes Retrofit por URL.
- `httpClient`: cliente OkHttp con timeouts y DNS fallback.
- Los ApiService son interfaces: no guardan datos, solo definen llamadas.

## Flujo

1. Un repositorio necesita datos.
2. Pide un servicio a `RetrofitInstance`.
3. Retrofit usa la interfaz correspondiente.
4. La anotacion `@GET` indica la ruta.
5. `@Query` mete parametros en URL.
6. `@Path` mete parametros dentro de la ruta.
7. Retrofit convierte JSON a modelos Kotlin con Gson.

## Firebase/API

- Steam:
  - `GetOwnedGames`
  - `ResolveVanityURL`
  - `GetPlayerSummaries`
  - `GetPlayerAchievements`
  - `GetSchemaForGame`
- IGDB Proxy:
  - `explore-games`
  - `category-games`
  - `search-games`
  - `games/{gameId}`

## Kotlin Basico Que Aparece Aqui

- `interface`: contrato de funciones, Retrofit lo implementa por detras.
- `@GET`: anotacion de Retrofit para ruta HTTP GET.
- `@Query`: parametro de URL.
- `@Path`: parte dinamica de la ruta.
- `by lazy`: se crea solo al primer uso.
- `ConcurrentHashMap`: mapa seguro para cachear Retrofit.

## Pregunta Para Defender

**Si me preguntan: Donde estan las llamadas a la API?**

Responderia: "Las rutas HTTP estan declaradas en los ApiService, y RetrofitInstance crea los servicios. Los repositorios son los que los usan."

**Si me preguntan: Por que separas SteamApiService e IGDBProxyApiService?**

Responderia: "Porque cada servicio tiene base URL, endpoints y modelos distintos. Separarlos hace mas claro de donde viene cada dato."

## Frase Para Decir En La Defensa

"RetrofitInstance fabrica los clientes y los ApiService son el contrato de llamadas HTTP."

