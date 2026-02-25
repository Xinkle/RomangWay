package feature

import feature.model.FFlogRanking
import feature.model.FFlogZones

data class SavageRaidZoneCandidate(
    val zoneId: Int,
    val zoneName: String,
    val difficultyId: Int
)

object FFlogRankingLogic {
    fun extractSpecNames(ranking: FFlogRanking): List<String> {
        return ranking.data?.characterData?.character?.zoneRankings?.allStars
            ?.mapNotNull { it?.spec }
            ?.distinct()
            ?: emptyList()
    }

    fun hasTierClearRanking(ranking: FFlogRanking): Boolean {
        val rankings = ranking.data?.characterData?.character?.zoneRankings?.rankings ?: return false
        val finalEncounterRanking = rankings.lastOrNull() ?: return false
        return (finalEncounterRanking.totalKills ?: 0) > 0
    }

    fun getZoneId(ranking: FFlogRanking): Int? =
        ranking.data?.characterData?.character?.zoneRankings?.zone

    fun toSavageRaidZoneCandidates(zones: FFlogZones): List<SavageRaidZoneCandidate> {
        return zones.data?.worldData?.zones
            ?.asSequence()
            ?.filterNotNull()
            ?.mapNotNull { zone ->
                val savageDifficulty = zone.difficulties
                    ?.filterNotNull()
                    ?.firstOrNull { difficulty ->
                        difficulty.name == "Savage" &&
                            (difficulty.sizes?.filterNotNull()?.contains(8) == true)
                    } ?: return@mapNotNull null

                val encounterCount = zone.encounters?.count { it?.id != null } ?: 0
                if (encounterCount < 4) return@mapNotNull null

                val zoneId = zone.id ?: return@mapNotNull null
                val difficultyId = savageDifficulty.id ?: return@mapNotNull null

                SavageRaidZoneCandidate(
                    zoneId = zoneId,
                    zoneName = zone.name ?: "Unknown Zone",
                    difficultyId = difficultyId
                )
            }
            ?.sortedByDescending { it.zoneId }
            ?.toList()
            ?: emptyList()
    }
}
