# 04 - MainActivity, NavGraph y Screen

## Resumen Para Tontos

Estos tres archivos son el esqueleto de TrophyChest. No son pantallas normales: explican como se abre la app, que estructura visual se monta y como se decide a que pantalla va el usuario.

La idea facil es esta:

- `MainActivity.kt` es la puerta de entrada de Android.
- `MiApp()` es la estructura general: cabecera, buscador, menu inferior y contenido.
- `NavGraph.kt` es el mapa que dice que pantalla corresponde a cada ruta.
- `Screen.kt` guarda los nombres de las rutas para no escribir textos sueltos por todo el codigo.

En una defensa, este bloque sirve para explicar la arquitectura general de la app antes de entrar en Firebase, Steam, PlayStation o IGDB.

## Archivo 1: MainActivity.kt

### Que Hace

`MainActivity` es la primera clase importante que se ejecuta cuando Android abre la aplicacion. Dentro de `onCreate()` se prepara la pantalla, se activa el modo edge-to-edge, se aplica el tema visual y se llama a `MiApp()`.

La parte clave no es que tenga mucha logica, sino que es donde empieza todo:

1. Android lanza `MainActivity`.
2. Se instala la splash screen.
3. Se llama a `setContent`.
4. Se aplica `TrophyChestTheme`.
5. Se monta `MiApp()`, que ya contiene la navegacion real.

### Estado

En `MainActivity` casi no hay estado. La clase solo prepara Compose.

El estado importante aparece dentro de `MiApp()`:

- `navController`: controla la navegacion entre pantallas.
- `navBackStackEntry`: observa cual es la pantalla actual.
- `currentRoute`: guarda la ruta actual como texto.
- `searchQuery`: texto que escribe el usuario en el buscador superior.
- `trimmedSearchQuery`: el mismo texto, pero sin espacios al principio o al final.
- `searchResultsState`: estado de la busqueda global de juegos.
- `mostrarMenuInferior`: decide si se ve o no el menu inferior.

Lo importante para defenderlo es que este proyecto no usa ViewModels. Aqui el estado global pequeño vive directamente en Compose con `rememberSaveable` y `produceState`.

### Flujo De MainActivity y MiApp

Cuando se abre la app, `MiApp()` crea un `NavController` con `rememberNavController()`. Ese objeto es como el GPS interno de la app: sabe en que pantalla estas y permite navegar a otra.

Despues, `currentBackStackEntryAsState()` permite que Compose se entere de los cambios de ruta. Si el usuario pasa de `principal` a `perfil`, esta variable cambia y la interfaz se redibuja.

Con `currentRoute`, el codigo calcula en que tipo de pantalla estamos:

- `esVideo`: pantalla inicial de video/splash.
- `esLogin`: pantalla de login.
- `esExplorar`: pantalla de explorar juegos.
- `esChat`: pantalla de chat.
- `esCategoria`: pantalla de categoria.
- `esTrofeos`: detalle de trofeos.
- `esDetalleJuego`: detalle de un juego.
- `esHelpCenter`: pantalla de ayuda.

Esto sirve para decidir si se muestran la cabecera y el menu inferior. Por ejemplo, en login no tiene sentido mostrar el menu inferior porque todavia no estas dentro de la app.

### Scaffold

`Scaffold` es la estructura base de Material Design. En este proyecto se usa como plantilla:

- `topBar`: muestra `TopHeader`, que contiene el buscador superior y el boton de ayuda.
- `bottomBar`: muestra `BottomNavigationBar`, que permite moverse por las secciones principales.
- contenido central: muestra `SetupNavGraph`, que decide que pantalla concreta se renderiza.

La frase sencilla seria: `Scaffold` pone el marco y `NavGraph` pone la pantalla dentro del marco.

### Buscador Global

El buscador superior vive en `MainActivity.kt`, no dentro de una pantalla concreta. Eso tiene sentido porque la cabecera aparece en varias zonas de la app.

El flujo es:

1. El usuario escribe en `TopHeader`.
2. `onSearchQueryChange` actualiza `searchQuery`.
3. `trimmedSearchQuery` elimina espacios innecesarios.
4. `produceState` detecta el cambio.
5. Si el texto esta vacio, no llama a la API.
6. Si hay texto, pone el estado en `Loading`.
7. Espera `delay(350)` para no llamar a IGDB con cada tecla demasiado rapido.
8. Llama a `IGDBRepository.searchGames(trimmedSearchQuery)`.
9. Convierte el resultado en `RemoteUiState.Success`, `Error` o `Empty`.
10. `TopHeader` recibe ese estado y muestra resultados, error o vacio.

Esto es importante porque demuestra que la UI se actualiza por estado: cuando cambia `searchResultsState`, Compose redibuja la cabecera.

### Firebase/API

`MainActivity.kt` no llama a Firebase directamente.

La llamada externa importante aqui es:

```kotlin
IGDBRepository.searchGames(trimmedSearchQuery)
```

Esa llamada busca juegos en IGDB a traves del repositorio. La pantalla no sabe los detalles del proxy ni de Retrofit; solo pide juegos y recibe un `Result`.

## Archivo 2: NavGraph.kt

### Que Hace

`NavGraph.kt` contiene `SetupNavGraph()`. Esta funcion es el mapa real de navegacion. Cada bloque `composable(...)` significa:

"Cuando la ruta sea esta, muestra esta pantalla".

Ejemplo mental:

- ruta `"login"` -> `LoginScreen`
- ruta `"principal"` -> `PrincipalScreen`
- ruta `"perfil"` -> `PerfilScreen`
- ruta `"detalle_juego/{platform}/{gameId}"` -> `DetalleJuegosScreen`

### Start Destination

El `startDestination` es:

```kotlin
startDestination = "video_splash"
```

Eso significa que la primera pantalla no es el login directamente, sino `VideoSplashScreen`.

El flujo normal seria:

1. Se abre la app.
2. Entra en `video_splash`.
3. `VideoSplashScreen` decide despues si manda al login o a la parte principal, segun la sesion.

### Rutas Simples

Hay rutas que no necesitan parametros:

- `Screen.Login.route`
- `Screen.Principal.route`
- `Screen.Mensajes.route`
- `Screen.Busqueda.route`
- `Screen.Trofeos.route`
- `Screen.Perfil.route`
- `Screen.ProfileSettings.route`
- `Screen.HelpCenter.route`
- `"explorar"`

Estas rutas solo abren una pantalla. No necesitan datos extra en la URL de navegacion.

Ejemplo:

```kotlin
composable(route = Screen.Perfil.route) {
    PerfilScreen(navController = navController)
}
```

Esto significa que cuando el `NavController` navega a `"perfil"`, se muestra `PerfilScreen`.

### Rutas Con Argumentos

Algunas pantallas necesitan recibir datos. Por ejemplo, para abrir un detalle de juego no basta con decir "abre detalle"; tambien hay que saber de que plataforma es y que juego es.

Por eso existen rutas con parametros:

- `detalle_juego/{platform}/{gameId}`
- `detalle_trofeos/{platform}/{gameId}`
- `chat/{connectionId}`
- `categoria/{genero}`

Los parametros van entre llaves. Compose Navigation los lee con `navArgument`.

Ejemplo:

```kotlin
navArgument("platform") { type = NavType.StringType }
navArgument("gameId") { type = NavType.StringType }
```

Esto dice que la ruta espera dos datos de tipo texto: `platform` y `gameId`.

### Detalle De Juego

La ruta de detalle de juego recibe:

- `platform`: indica si el juego viene de Steam, PlayStation o IGDB.
- `gameId`: identificador del juego.

Dentro del bloque se leen asi:

```kotlin
val rawPlatform = backStackEntry.arguments?.getString("platform")
val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
```

Despues se llama a:

```kotlin
DetalleJuegosScreen(
    navController = navController,
    platform = rawPlatform.toPlatformOrDefault(),
    gameId = gameId,
    onBack = { navController.popBackStack() }
)
```

La idea sencilla: la ruta trae texto, el codigo lo convierte a una plataforma real y abre la pantalla de detalle.

### Detalle De Trofeos

`DetalleTrofeosScreen` funciona parecido al detalle de juego:

- recibe `platform`;
- recibe `gameId`;
- abre la pantalla de trofeos/logros concreta.

Esto permite reutilizar la misma pantalla de trofeos para distintas plataformas.

### Chat

La ruta de chat es:

```kotlin
chat/{connectionId}
```

`connectionId` identifica la conversacion concreta con otro usuario. Cuando se abre:

```kotlin
ChatScreen(navController, connectionId)
```

La pantalla de chat usa ese identificador para saber que mensajes cargar.

### Categoria

La ruta de categoria es:

```kotlin
categoria/{genero}
```

El parametro `genero` indica que categoria de juegos se quiere mostrar, por ejemplo accion, aventura u otro genero.

### toPlatformOrDefault

Al final de `NavGraph.kt` hay una funcion privada:

```kotlin
private fun String?.toPlatformOrDefault(): PlataformaJuego
```

Sirve para convertir el texto que viene de la ruta en un valor de `PlataformaJuego`.

Si la ruta viene mal o el texto no coincide, devuelve `PlataformaJuego.STEAM` por defecto. Esto evita que la app crashee por un parametro incorrecto.

Para defenderlo puedes decir que es una pequeña proteccion ante rutas incompletas o mal formadas.

## Archivo 3: Screen.kt

### Que Hace

`Screen.kt` centraliza las rutas de navegacion. En vez de escribir `"login"`, `"perfil"` o `"detalle_juego/{platform}/{gameId}"` en muchos archivos, se guardan dentro de una `sealed class`.

Esto ayuda a:

- tener las rutas ordenadas;
- evitar errores por escribir mal un texto;
- encontrar rapido todas las pantallas principales;
- crear rutas con parametros de forma controlada.

### Sealed Class

La clase principal es:

```kotlin
sealed class Screen(val route: String)
```

Una `sealed class` es una clase cerrada: sus opciones estan definidas en el propio archivo. Aqui cada `object` representa una pantalla.

Ejemplos:

```kotlin
object Login : Screen("login")
object Principal : Screen("principal")
object Perfil : Screen("perfil")
```

Cada objeto tiene una ruta asociada.

### Objetos De Ruta

Los objetos simples son:

- `Login`
- `Principal`
- `Mensajes`
- `Busqueda`
- `Trofeos`
- `Perfil`
- `ProfileSettings`
- `HelpCenter`

Estas rutas no necesitan construir nada especial. Solo guardan un texto.

### Rutas Dinamicas

`DetalleJuego` y `DetalleTrofeos` son especiales porque necesitan parametros:

```kotlin
object DetalleJuego : Screen("detalle_juego/{platform}/{gameId}")
```

La ruta plantilla tiene `{platform}` y `{gameId}`, pero para navegar hay que construir una ruta real.

Por eso existe:

```kotlin
fun createRoute(platform: PlataformaJuego, gameId: String): String
```

Ejemplo real:

```kotlin
detalle_juego/STEAM/12345
```

o:

```kotlin
detalle_trofeos/PLAYSTATION/NPWR00000_00
```

### Uri.encode

En `createRoute` se usa:

```kotlin
Uri.encode(gameId)
```

Esto protege la ruta si el identificador tiene caracteres raros, espacios o simbolos que podrian romper la navegacion.

Explicacion sencilla: antes de meter un dato dentro de una ruta, lo convierto a un formato seguro para URL/ruta.

## Estado

Este bloque no tiene un ViewModel ni un estado complejo.

El estado principal esta en `MiApp()`:

- `searchQuery`: texto del buscador.
- `searchResultsState`: estado remoto de la busqueda.
- `currentRoute`: ruta actual.
- `mostrarMenuInferior`: decision visual basada en la ruta.

`NavGraph.kt` no guarda estado propio. Solo recibe el `navController` y conecta rutas con pantallas.

`Screen.kt` tampoco guarda estado. Solo define constantes y funciones para crear rutas.

## Flujo Completo

El flujo completo de estos tres archivos seria:

1. Android abre `MainActivity`.
2. `MainActivity` llama a `setContent`.
3. Se aplica `TrophyChestTheme`.
4. Se ejecuta `MiApp()`.
5. `MiApp()` crea el `NavController`.
6. Se mira la ruta actual con `currentBackStackEntryAsState`.
7. Se decide si mostrar cabecera y menu inferior.
8. `Scaffold` monta la estructura visual.
9. `SetupNavGraph` crea el `NavHost`.
10. `NavHost` empieza en `video_splash`.
11. Cada vez que se llama a `navController.navigate(...)`, cambia la ruta.
12. Compose redibuja la pantalla correspondiente.

## Firebase/API

Este bloque toca poco Firebase directamente.

Lo importante es:

- `MainActivity.kt` llama a `IGDBRepository.searchGames` para el buscador global.
- `NavGraph.kt` no llama a APIs; solo decide pantallas.
- `Screen.kt` no llama a APIs; solo define rutas.
- Las pantallas abiertas por `NavGraph` son las que luego llaman a Firebase, Steam, PlayStation o IGDB.

La separacion es buena para defenderla:

"La navegacion no mezcla la logica de datos. Solo manda al usuario a la pantalla correcta. Luego cada pantalla usa su repositorio."

## Kotlin Basico Que Aparece Aqui

### `@Composable`

Indica que una funcion pinta interfaz con Jetpack Compose. `MiApp()` y `SetupNavGraph()` son composables.

### `rememberNavController`

Crea y recuerda el controlador de navegacion. Si Compose redibuja, no crea un controlador nuevo cada vez.

### `by`

Permite leer estados de Compose de forma mas limpia.

En vez de escribir algo como `searchQuery.value`, puedes trabajar directamente con `searchQuery`.

### `rememberSaveable`

Guarda estado y permite recuperarlo en situaciones como cambios de configuracion. Aqui se usa para el texto del buscador.

### `mutableStateOf`

Crea una variable observable por Compose. Cuando cambia, Compose puede redibujar la UI.

### `LaunchedEffect`

Ejecuta una accion cuando cambian unas claves. Aqui se usa para limpiar la busqueda al entrar en pantallas donde no debe aparecer.

### `produceState`

Convierte una operacion asincrona en estado de Compose. Aqui se usa para llamar a IGDB y producir un estado de carga, exito, vacio o error.

### `Scaffold`

Estructura base de pantalla: parte superior, parte inferior y contenido.

### `NavHost`

Es el contenedor que muestra una pantalla u otra segun la ruta actual.

### `composable`

Registra una ruta dentro del `NavHost`.

### `navArgument`

Define parametros que una ruta necesita recibir.

### `sealed class`

Sirve para representar un conjunto cerrado de opciones. Aqui representa las pantallas/rutas principales.

### `object`

Crea una unica instancia. Para rutas como `Login` o `Perfil`, no hace falta crear objetos nuevos; con uno basta.

### `private fun`

Funcion visible solo dentro de ese archivo. `toPlatformOrDefault` no se usa fuera de `NavGraph.kt`.

## Pregunta Para Defender

**Si me preguntan: Como arranca la aplicacion?**

Responderia: "Android entra por `MainActivity.onCreate`. Ahi monto Compose con `setContent`, aplico el tema de TrophyChest y llamo a `MiApp`, que contiene la estructura general con cabecera, menu inferior y navegacion."

**Si me preguntan: Como sabe la app que pantalla abrir?**

Responderia: "Uso un `NavController` y un `NavHost`. El `NavHost` tiene registradas las rutas en `NavGraph`. Cuando llamo a `navController.navigate`, cambia la ruta actual y Compose muestra el composable asociado."

**Si me preguntan: Por que tienes un archivo Screen.kt?**

Responderia: "Para centralizar las rutas y no escribir textos repetidos por muchos archivos. Asi, si quiero ir a perfil uso `Screen.Perfil.route`, y para rutas con parametros uso `createRoute`."

**Si me preguntan: Que son las rutas con llaves como `{gameId}`?**

Responderia: "Son parametros de navegacion. La ruta indica que necesita recibir un dato, por ejemplo el id del juego. Luego en `NavGraph` lo leo desde `backStackEntry.arguments`."

**Si me preguntan: Por que usas `Uri.encode(gameId)`?**

Responderia: "Porque el id viaja dentro de una ruta. Si tuviera caracteres raros, podria romper la navegacion. Con `Uri.encode` lo paso a un formato seguro."

**Si me preguntan: Como se actualiza la cabecera cuando busco un juego?**

Responderia: "El texto del buscador esta en `searchQuery`. Cuando cambia, `produceState` vuelve a ejecutarse, llama al repositorio de IGDB y actualiza `searchResultsState`. Como ese valor es estado de Compose, la cabecera se redibuja con los resultados."

**Si me preguntan: Por que no muestras el menu inferior en todas las pantallas?**

Responderia: "Porque hay pantallas donde molestaria o no tendria sentido, como login, splash, chat o detalle de juego. Calculo la ruta actual y con eso decido si el menu inferior debe aparecer."

**Si me preguntan: Donde estan los ViewModels?**

Responderia: "En este proyecto no uso ViewModels. El estado de cada pantalla se gestiona con Compose mediante `remember`, `rememberSaveable`, `produceState` o `LaunchedEffect`, y la logica de datos esta delegada en repositorios."

## Frase Para Decir En La Defensa

"La app arranca en `MainActivity`, monta una estructura comun con `Scaffold`, y despues `NavGraph` funciona como el mapa de rutas. `Screen.kt` me permite tener esas rutas centralizadas y construir de forma segura las que necesitan parametros, como detalles de juegos o trofeos."
