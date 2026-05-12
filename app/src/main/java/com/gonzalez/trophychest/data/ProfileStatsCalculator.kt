package com.gonzalez.trophychest.data

data class ProfileStats(
    val totalGames: Int = 0,
    val totalTrophies: Int = 0,
    val friendCount: Int = 0
)

object ProfileStatsCalculator {
    fun calculate(
        steamGames: List<Juego>,
        playStationRecentGames: List<Juego>,
        playStationTrophyGames: List<Juego>,
        connections: List<FriendConnection>
    ): ProfileStats {
        val platformGames = steamGames + playStationRecentGames + playStationTrophyGames
        val uniqueGameKeys = platformGames.mapNotNull { it.profileGameKey() }.toSet()
        val uniqueTrophyGames = (steamGames + playStationTrophyGames)
            .distinctBy { it.profileGameKey() }

        return ProfileStats(
            totalGames = uniqueGameKeys.size,
            totalTrophies = uniqueTrophyGames.sumOf { it.achievementSummary?.desbloqueados ?: 0 },
            friendCount = connections.count { it.status == CONNECTION_STATUS_ACCEPTED }
        )
    }

    private fun Juego.profileGameKey(): String? {
        val gameId = achievementGameId?.takeIf { it.isNotBlank() }
            ?: platformGameId.takeIf { it.isNotBlank() }
            ?: return null
        return "${platform.name}:$gameId"
    }
}
