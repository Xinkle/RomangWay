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
        private data class JobDisplayInfo(
            val emoji: String,
            val koreanName: String
        )

        private val jobDisplayMapping = mapOf(
            "Astrologian" to JobDisplayInfo("<:Astrologian:938816154229690368>", "점성술사"),
            "Sage" to JobDisplayInfo("<:Sage:1030750763758125066>", "현자"),
            "Scholar" to JobDisplayInfo("<:Scholar:938816154540048465>", "학자"),
            "WhiteMage" to JobDisplayInfo("<:WhiteMage:938816154292592691>", "백마도사"),
            "DarkKnight" to JobDisplayInfo("<:DarkKnight:938816154695262258>", "암흑기사"),
            "Paladin" to JobDisplayInfo("<:Paladin:938816581490839572>", "나이트"),
            "Gunbreaker" to JobDisplayInfo("<:Gunbreaker:938816154338750474>", "건브레이커"),
            "Warrior" to JobDisplayInfo("<:Warrior:938816154351333407>", "전사"),
            "Bard" to JobDisplayInfo("<:Bard:938816153864798249>", "음유시인"),
            "BlackMage" to JobDisplayInfo("<:BlackMage:938816154510696488>", "흑마도사"),
            "Dancer" to JobDisplayInfo("<:Dancer:938816154233896980>", "무도가"),
            "Dragoon" to JobDisplayInfo("<:Dragoon:938816154317758474>", "용기사"),
            "Machinist" to JobDisplayInfo("<:Machinist:938816154640719872>", "기공사"),
            "Monk" to JobDisplayInfo("<:Monk:938816154456178730>", "몽크"),
            "Ninja" to JobDisplayInfo("<:Ninja:938816154728800256>", "닌자"),
            "Reaper" to JobDisplayInfo("<:Reaper:1030729479598907422>", "리퍼"),
            "RedMage" to JobDisplayInfo("<:RedMage:946139417033187351>", "적마도사"),
            "Samurai" to JobDisplayInfo("<:Samurai:938816154431000666>", "사무라이"),
            "Summoner" to JobDisplayInfo("<:Summoner:938816154561032232>", "소환사")
        )

        private fun Any?.toDisplayOrNA(): String = this?.toString() ?: "N/A"

        fun fromRanking(fflogRanking: FFlogRanking, name: String, server: String): FFlogRankingSummary {
            val ranking = fflogRanking.data!!.characterData!!.character!!.zoneRankings!!
            val allstar = ranking.allStars?.first()
            val firstFloor = ranking.rankings?.getOrNull(0)
            val secondFloor = ranking.rankings?.getOrNull(1)
            val thirdFloor = ranking.rankings?.getOrNull(2)
            val fourthFloor = ranking.rankings?.getOrNull(3)
            val fifthFloor = ranking.rankings?.getOrNull(4)

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
            직업: ${toEmbedFieldName()}
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

    fun toEmbedFieldName(): String {
        val displayInfo = job?.let(jobDisplayMapping::get)
        return when {
            displayInfo != null -> "${displayInfo.emoji} ${displayInfo.koreanName}"
            job != null -> job
            else -> "직업: N/A"
        }
    }

    fun toJobKoreanName(): String = job?.let { jobDisplayMapping[it]?.koreanName ?: it } ?: "Unknown"

    fun toEmbedFieldValue(): String {
        return """
            올스타: $allStarPoint (${allStarPercent ?: "N/A"} / $allStarRankings)
            1층 $firstFloor | 2층 $secondFloor | 3층 $thirdFloor
            4층전 $fourthFloor | 4층후 $fifthFloor
        """.trimIndent()
    }

    fun toDetailDescriptionWithoutIdentity(): String {
        return """
            직업: ${toEmbedFieldName()}
            올스타 포인트: $allStarPoint
            올스타 백분위: ${allStarPercent ?: "N/A"}
            올스타 등수: ${allStarRankings ?: "N/A"}
            1층: $firstFloor
            2층: $secondFloor
            3층: $thirdFloor
            4층전반: $fourthFloor
            4층후반: $fifthFloor
        """.trimIndent()
    }

    fun allStarPointValueOrDefault(default: Double = -1.0): Double =
        allStarPoint?.substringBefore("/")?.trim()?.toDoubleOrNull() ?: default


}
