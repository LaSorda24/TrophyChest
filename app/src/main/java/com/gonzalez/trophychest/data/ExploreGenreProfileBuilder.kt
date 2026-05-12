package com.gonzalez.trophychest.data

object ExploreGenreProfileBuilder {
    private const val DEFAULT_MAX_GENRES = 5

    fun buildFromCachedGames(
        steamGames: List<Juego>,
        playStationGames: List<Juego>,
        maxGenres: Int = DEFAULT_MAX_GENRES
    ): List<String> {
        if (maxGenres <= 0) return emptyList()

        val games = (steamGames + playStationGames)
            .sortedWith(
                compareByDescending<Juego> { it.playtimeMinutes }
                    .thenByDescending { it.lastPlayedEpochSeconds ?: 0L }
                    .thenBy { it.title }
            )

        return extractUniqueGenres(games, maxGenres)
    }

    fun extractUniqueGenres(games: List<Juego>, maxGenres: Int = DEFAULT_MAX_GENRES): List<String> {
        if (maxGenres <= 0) return emptyList()

        val genresByKey = linkedMapOf<String, String>()
        games.forEach { game ->
            game.genres.forEach { rawGenre ->
                val normalized = normalizeGenre(rawGenre) ?: return@forEach
                genresByKey.putIfAbsent(normalized.key, normalized.displayName)
                if (genresByKey.size >= maxGenres) {
                    return genresByKey.values.toList()
                }
            }
        }

        return genresByKey.values.toList()
    }

    fun mergeGenreLists(
        primary: List<String>,
        secondary: List<String>,
        maxGenres: Int = DEFAULT_MAX_GENRES
    ): List<String> {
        if (maxGenres <= 0) return emptyList()

        val genresByKey = linkedMapOf<String, String>()
        (primary + secondary).forEach { rawGenre ->
            val normalized = normalizeGenre(rawGenre) ?: return@forEach
            genresByKey.putIfAbsent(normalized.key, normalized.displayName)
            if (genresByKey.size >= maxGenres) {
                return genresByKey.values.toList()
            }
        }

        return genresByKey.values.toList()
    }

    private fun normalizeGenre(value: String): NormalizedGenre? {
        val trimmed = value.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return null

        val displayName = when {
            trimmed.equals("RPG", ignoreCase = true) -> "Role-playing (RPG)"
            trimmed.equals("Role Playing", ignoreCase = true) -> "Role-playing (RPG)"
            trimmed.equals("Role-playing", ignoreCase = true) -> "Role-playing (RPG)"
            trimmed.equals("Action-Adventure", ignoreCase = true) -> "Adventure"
            else -> trimmed
        }

        return NormalizedGenre(
            key = displayName.lowercase(),
            displayName = displayName
        )
    }

    private data class NormalizedGenre(
        val key: String,
        val displayName: String
    )
}
