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
        private val jobEmojiMapping = mapOf(
            "Astrologian" to "<:Astrologian:938816154229690368>",
            "Sage" to "<:Sage:1030750763758125066>",
            "Scholar" to "<:Scholar:938816154540048465>",
            "WhiteMage" to "<:WhiteMage:938816154292592691>",
            "DarkKnight" to "<:DarkKnight:938816154695262258>",
            "Paladin" to "<:Paladin:938816581490839572>",
            "Gunbreaker" to "<:Gunbreaker:938816154338750474>",
            "Warrior" to "<:Warrior:938816154351333407>",
            "Bard" to "<:Bard:938816153864798249>",
            "BlackMage" to "<:BlackMage:938816154510696488>",
            "Dancer" to "<:Dancer:938816154233896980>",
            "Dragoon" to "<:Dragoon:938816154317758474>",
            "Machinist" to "<:Machinist:938816154640719872>",
            "Monk" to "<:Monk:938816154456178730>",
            "Ninja" to "<:Ninja:938816154728800256>",
            "Reaper" to "<:Reaper:1030729479598907422>",
            "RedMage" to "<:RedMage:946139417033187351>",
            "Samurai" to "<:Samurai:938816154431000666>",
            "Summoner" to "<:Summoner:938816154561032232>"
        )

        private fun Any?.toDisplayOrNA(): String = this?.toString() ?: "N/A"

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
                firstFloor = firstFloor?.rankPercent.toDisplayOrNA(),
                secondFloor = secondFloor?.rankPercent.toDisplayOrNA(),
                thirdFloor = thirdFloor?.rankPercent.toDisplayOrNA(),
                fourthFloor = fourthFloor?.rankPercent.toDisplayOrNA(),
                fifthFloor = fifthFloor?.rankPercent.toDisplayOrNA(),
            )
        }
    }

    override fun toString(): String {
        return """
            이름: $name
            서버: $server
            직업: ${jobEmojiMapping[job]}
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
