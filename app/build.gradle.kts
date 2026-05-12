import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // AQUI ACTIVO FIREBASE PARA LOGIN Y BASE DE DATOS
}

// AQUI LEO LAS CLAVES LOCALES SIN SUBIRLAS AL REPOSITORIO
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localOrGradleProperty(vararg names: String): String {
    for (name in names) {
        val gradleValue = providers.gradleProperty(name).orNull
        if (gradleValue != null) {
            return gradleValue
        }

        val localValue = localProperties.getProperty(name)
        if (localValue != null) {
            return localValue
        }
    }

    return ""
}

fun String.asBuildConfigString(): String {
    return replace("\\", "\\\\").replace("\"", "\\\"")
}

val steamApiKey = localOrGradleProperty(
    "STEAM_API_KEY",
    "steamApiKey",
    "steam.api.key"
)

val releaseCalendarBaseUrl = localOrGradleProperty(
    "RELEASE_CALENDAR_BASE_URL",
    "releaseCalendarBaseUrl",
    "release.calendar.base.url"
)

val igdbProxyBaseUrl = localOrGradleProperty(
    "IGDB_PROXY_BASE_URL",
    "igdbProxyBaseUrl",
    "igdb.proxy.base.url",
    "RELEASE_CALENDAR_BASE_URL",
    "releaseCalendarBaseUrl",
    "release.calendar.base.url"
)

val configuredPlayStationProxyBaseUrl = localOrGradleProperty(
    "PLAYSTATION_PROXY_BASE_URL",
    "playStationProxyBaseUrl",
    "playstation.proxy.base.url"
)

val playStationProxyBaseUrl = configuredPlayStationProxyBaseUrl.ifBlank {
    igdbProxyBaseUrl.ifBlank { releaseCalendarBaseUrl }
}

android {
    namespace = "com.gonzalez.trophychest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gonzalez.trophychest"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "STEAM_API_KEY", "\"${steamApiKey.asBuildConfigString()}\"")
        buildConfigField("String", "RELEASE_CALENDAR_BASE_URL", "\"${releaseCalendarBaseUrl.asBuildConfigString()}\"")
        buildConfigField("String", "IGDB_PROXY_BASE_URL", "\"${igdbProxyBaseUrl.asBuildConfigString()}\"")
        buildConfigField("String", "PLAYSTATION_PROXY_BASE_URL", "\"${playStationProxyBaseUrl.asBuildConfigString()}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // FIREBASE PARA CUENTAS, PERFIL Y DATOS DEL USUARIO
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // DISENO Y UTILIDADES DE LA INTERFAZ
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    // LIBRERIAS PRINCIPALES DE ANDROID Y COMPOSE
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // CON ESTO CONECTAMOS CON STEAM, IGDB Y LOS PROXIES DEL PROYECTO
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // PRUEBAS DEL PROYECTO
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
