package feature.model

import kotlin.math.floor

data class FFlogRankingSummary(
    val name: String,
    val server: String,
    val job: String?,
    val allStarPoint: String?,
    val allStarPercent: String?,
    val allStarRankings: String?,
    val firstFloor: String?,
    val secondFloor: String?,
    val thirdFloor: String?,
    val fourthFloor: String?,
    val fifthFloor: String?
) {
    companion object {
        fun fromRanking(fflogRanking: FFlogRanking, name: String, server: String): FFlogRankingSummary {
            val ranking = fflogRanking.data!!.characterData!!.character!!.zoneRankings!!
            val allstar = ranking.allStars?.first()
            val firstFloor = ranking.rankings?.get(0)
            val secondFloor = ranking.rankings?.get(1)
            val thirdFloor = ranking.rankings?.get(2)
            val fourthFloor = ranking.rankings?.get(3)
            val fifthFloor = ranking.rankings?.get(4)

            return FFlogRankingSummary(
                name = name,
                server = server,
                job = allstar?.spec,
                allStarPoint = "${allstar?.points} / ${allstar?.possiblePoints}",
                allStarPercent = "${allstar?.rankPercent?.times(10.0)?.let { floor(it) }?.div(10.0)}",
                allStarRankings = "${allstar?.rank} / ${allstar?.total}",
                firstFloor = "${firstFloor?.rankPercent}",
                secondFloor = "${secondFloor?.rankPercent}",
                thirdFloor = "${thirdFloor?.rankPercent}",
                fourthFloor = "${fourthFloor?.rankPercent}",
                fifthFloor = "${fifthFloor?.rankPercent}",
            )
        }
    }

    override fun toString(): String {
        return """
            이름: $name
            서버: $server
            직업: $job
            올스타 포인트: $allStarPoint
            올스타 백분위: $allStarPercent
            올스타 등수: $allStarRankings
            1층: $firstFloor
            2층: $secondFloor
            3층: $thirdFloor
            4층전반: $fourthFloor
            4층후반: $fifthFloor
        """.trimIndent()
    }


}