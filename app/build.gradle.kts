import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // AQUI ACTIVO FIREBASE PARA LOGIN Y BASE DE DATOS
}

// CREAMOS UN OBJETO VACIO PARA GUARDAR LOS CODIGOS
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")//BUSCA EL ARCHIVO LOCAL.PROPERTIES
    if (localPropertiesFile.exists()) {//SI EXISTE
        localPropertiesFile.inputStream().use(::load)//LO CARGA EN EL OBJETO VACIO
    }
}

//FUNCION PARA BUSCAR NOMBRES
//LE DAS UN NOMBRE Y BUSCA EN LOCAL.PROPERTIES
fun localProperty(name: String): String {
        val localValue = localProperties.getProperty(name)
        if (localValue != null) {
            return localValue //DEVUELVE VALOE ENCONTRADO
        }
//SI NO LO ENCUENTRA DEVUELVE UN STRING VACIO
    return ""
}

//FUNCION PARA QUE COMPRENDA LA CADENA DE TEXTO
//Y NO GENERE CONFUSIONES , POR EJEMPLO \U O COMILLAS
fun String.asBuildConfigString(): String {
    return replace("\\", "\\\\").replace("\"", "\\\"")
}

//VARIABLE PARA LA CLAVE DE STEAM
val steamApiKey = localProperty("STEAM_API_KEY")
//GUARDA LA DIRECCION WEB DE IGBD PARA EL CALENDARIO
val releaseCalendarBaseUrl = localProperty("RELEASE_CALENDAR_BASE_URL")
//GUARDA LA DIRECCION PERO PARA EL RESTO DE COSAS CON OGDB
val igdbProxyBaseUrl = localProperty("RELEASE_CALENDAR_BASE_URL")

//PLAY STATION USA LA DE IGDB PARA QUE PUEDA FUNCIONAR
//DENTRO DEL PROXY DE IGBD TENEMOS DIRECCIONES DE PSN
val playStationProxyBaseUrl = releaseCalendarBaseUrl


//CONFUIGURACION ANDROID
android {
    namespace = "com.gonzalez.trophychest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gonzalez.trophychest"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"


        //CREAMOS LOS BUIILDCONFIG
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
    implementation(platform(libs.firebase.bom))//BOM PARA SINCRONIZAR
    implementation(libs.firebase.auth)//AUTENTICACION
    implementation(libs.firebase.firestore)//FIRESTORE PARA BDD
    implementation(libs.play.services.auth)//AUTENTICACION DE GOOGLE
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
