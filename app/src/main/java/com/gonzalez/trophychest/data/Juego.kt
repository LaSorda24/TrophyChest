package com.gonzalez.trophychest.data

enum class PlataformaJuego(val displayName: String) {
    STEAM("Steam"),
    PSN("PlayStation"),
    GAME_PASS("Game Pass"),
    IGDB("IGDB")
}

// DEJO GAME PASS COMO OPCION FUTURA PORQUE AUN FALTA EL ACCESO OFICIAL DE MICROSOFT
const val GAME_PASS_UNAVAILABLE_MESSAGE = "Game Pass queda pendiente hasta tener acceso a la API oficial de Microsoft."

data class ResumenTrofeos(
    val total: Int = 0,
    val desbloqueados: Int = 0,
    val porcentajeCompletado: Float = 0f
)

data class PegiAgeRating(
    val label: String,
    val minimumAge: Int,
    val imageUrl: String? = null
)

data class Juego(
    val platform: PlataformaJuego = PlataformaJuego.STEAM,
    val platformGameId: String,
    val title: String,
    val headerImageUrl: String,
    val capsuleImageUrl: String,
    val heroImageUrl: String,
    val iconImageUrl: String?,
    val playtimeMinutes: Int = 0,
    val lastPlayedEpochSeconds: Long? = null,
    val achievementGameId: String? = null,
    val achievementSummary: ResumenTrofeos? = null,
    val shortDescription: String? = null,
    val developers: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val releaseDate: String? = null,
    val screenshots: List<String> = emptyList(),
    val supportedPlatforms: List<String> = emptyList(),
    val ageRating: PegiAgeRating? = null,
    val steamAppId: String? = null
)

