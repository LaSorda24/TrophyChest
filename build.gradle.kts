//DECLARAMOS LOS PLUGGINS QUE VAN A ESTAR DISPONIBLE

// Top-level build file
plugins {

    alias(libs.plugins.android.application) apply false
    //PLUGGIN DE GOOGLE PARA CREAR APPS ANDROID

    alias(libs.plugins.kotlin.android) apply false
    //PLUGGIN LENGUAJE KOTLIN

    alias(libs.plugins.kotlin.compose) apply false
    //PLUGGIN JETPACK COMPONE

    alias(libs.plugins.google.services) apply false
    // PUENTE PARA PODER LEERSE EL GOOGLE-SERVICES.JSON Y QUE EL LOGIN Y FIRESTORE FUNCIONEN
}
