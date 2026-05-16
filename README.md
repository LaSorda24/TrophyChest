# TrophyChest

TrophyChest es una aplicación móvil Android desarrollada como Trabajo de Fin de Grado. Su objetivo es centralizar la actividad de un jugador en distintas plataformas, permitiendo consultar biblioteca de juegos, progreso de trofeos/logros, perfil de usuario, amigos, chat y descubrimiento de nuevos títulos.

La aplicación integra servicios externos como Steam, PlayStation Network e IGDB mediante una arquitectura cliente-servidor, usando una app Android nativa y un proxy desplegable en Cloudflare Workers.

## Funcionalidades principales

- Registro e inicio de sesión con Firebase Authentication.
- Inicio de sesión con correo/contraseña y Google.
- Gestión de perfil de usuario con avatar, banner y nombre público.
- Sistema de código de amigo.
- Solicitudes de amistad y chat entre usuarios.
- Vinculación de cuenta de Steam.
- Sincronización de biblioteca y logros de Steam.
- Vinculación de PlayStation mediante NPSSO.
- Consulta de juegos y trofeos de PlayStation.
- Exploración de videojuegos por categorías y géneros.
- Buscador global de juegos.
- Ficha detallada de cada juego con descripción, imágenes, género, PEGI y plataformas.
- Calendario de próximos lanzamientos.
- Pantalla de trofeos/logros con filtros por plataforma y progreso.
- Centro de ayuda integrado.
- Splash screen con vídeo de presentación.

## Tecnologías utilizadas

### Aplicación Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coroutines
- Retrofit
- OkHttp
- Coil
- Firebase Authentication
- Cloud Firestore
- Media3 / ExoPlayer
- JUnit

### Backend / Proxy

- Cloudflare Workers
- JavaScript ES Modules
- Wrangler
- IGDB API
- Twitch OAuth
- Steam Web API
- Steam Store API
- psn-api

## Arquitectura del proyecto

El repositorio está dividido en dos partes principales:

TrophyChest/
├── app/                 # Aplicación Android
├── igdb-proxy/          # Proxy backend en Cloudflare Workers
├── gradle/              # Configuración de Gradle
├── firestore.rules      # Reglas de seguridad de Firestore
└── documentos_estudio/  # Documentación técnica del desarrollo

### Módulo Android

La app sigue una separación por responsabilidades:

app/src/main/java/com/gonzalez/trophychest/
├── data/           # Modelos, repositorios, servicios API y lógica de datos
├── navigation/     # Rutas y grafo de navegación
├── ui/
│   ├── components/ # Componentes reutilizables
│   ├── screens/    # Pantallas principales
│   └── theme/      # Colores, tipografías y tema visual
└── MainActivity.kt # Entrada principal de la aplicación

### Proxy IGDB / PlayStation

El proxy evita exponer credenciales sensibles dentro de la aplicación Android. Se encarga de consultar IGDB, gestionar tokens de Twitch y normalizar algunas respuestas externas.

Endpoints principales:

GET  /release-calendar
GET  /search-games?q=
GET  /explore-games
GET  /category-games?genre=
GET  /games/{id}

GET  /psn/status
POST /psn/auth/npsso
POST /psn/title-history
POST /psn/trophy-titles
POST /psn/achievements/{npCommunicationId}

## Requisitos previos

Para ejecutar el proyecto es necesario tener instalado:

- Android Studio
- JDK 11 o superior
- Gradle
- Node.js
- npm
- Cuenta de Firebase
- Cuenta de Cloudflare
- Claves de Steam Web API
- Credenciales de Twitch Developer para acceder a IGDB

## Configuración de Firebase

1. Crear un proyecto en Firebase.
2. Activar Firebase Authentication.
3. Activar Cloud Firestore.
4. Añadir una aplicación Android con el package name:

com.gonzalez.trophychest

5. Descargar el archivo google-services.json.
6. Colocarlo en:

app/google-services.json

7. Publicar o adaptar las reglas incluidas en:

firestore.rules

## Configuración de variables locales

En el archivo local.properties, añadir las claves necesarias:

STEAM_API_KEY=TU_CLAVE_DE_STEAM
RELEASE_CALENDAR_BASE_URL=https://tu-worker.workers.dev/

La variable RELEASE_CALENDAR_BASE_URL se utiliza como URL base del proxy para IGDB, calendario de lanzamientos y PlayStation.

## Configuración del proxy

Entrar en la carpeta del proxy:

cd igdb-proxy

Instalar dependencias:

npm install

Configurar los secretos de Cloudflare Workers:

npx wrangler secret put TWITCH_CLIENT_ID
npx wrangler secret put TWITCH_CLIENT_SECRET

Ejecutar el proxy en local:

npm run dev

Desplegar el proxy:

npm run deploy

## Ejecución de la app Android

Desde la raíz del proyecto:

./gradlew assembleDebug

O en Windows:

gradlew.bat assembleDebug

También puede abrirse directamente desde Android Studio y ejecutarse en un emulador o dispositivo físico.

## Pruebas

El proyecto incluye pruebas unitarias para repositorios, mapeadores, validaciones, filtros, navegación y lógica de negocio.

Ejecutar pruebas Android:

./gradlew testDebugUnitTest

En Windows:

gradlew.bat testDebugUnitTest

Ejecutar pruebas del proxy:

cd igdb-proxy
npm test

## Estado del proyecto

El proyecto se encuentra en una versión funcional orientada a la presentación del TFG. Incluye autenticación, persistencia en Firebase, integración con APIs externas, navegación completa, perfil de usuario, sistema social, exploración de juegos y consulta de logros/trofeos.

## Posibles mejoras futuras

- Integración completa con Xbox/Game Pass cuando exista acceso oficial estable.
- Añadir notificaciones.
- Implementaciones de personalizacion en el apartado de perfil.
- Incorporar recomendaciones personalizadas más profundas.
- Añadir modo offline con base de datos local.

## Autor

Proyecto desarrollado por Sergio Gonzalez como Trabajo de Fin de Grado del Gradp Superior de DAM.
