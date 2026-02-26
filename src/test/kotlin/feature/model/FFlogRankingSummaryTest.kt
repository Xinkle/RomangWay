package feature.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FFlogRankingSummaryTest {
    @Test
    fun `4보스 티어도 IndexOutOfBounds 없이 요약된다`() {
        val ranking = createRanking(
            spec = "Astrologian",
            floorPercents = listOf(98.6, 100.0, 86.7, 90.1)
        )

        val summary = FFlogRankingSummary.fromRanking(ranking, "Scaniorts", "초코보")

        assertEquals("98.6", summary.firstFloor)
        assertEquals("100.0", summary.secondFloor)
        assertEquals("86.7", summary.thirdFloor)
        assertEquals("90.1", summary.fourthFloor)
        assertEquals("N/A", summary.fifthFloor)
    }

    @Test
    fun `null 퍼센트는 NA로 표기된다`() {
        val ranking = createRanking(
            spec = "Sage",
            floorPercents = listOf(100.0, 100.0, null, null, null)
        )

        val summary = FFlogRankingSummary.fromRanking(ranking, "피어베인", "톤베리")

        assertEquals("100.0", summary.firstFloor)
        assertEquals("100.0", summary.secondFloor)
        assertEquals("N/A", summary.thirdFloor)
        assertEquals("N/A", summary.fourthFloor)
        assertEquals("N/A", summary.fifthFloor)
    }

    private fun createRanking(spec: String, floorPercents: List<Double?>): FFlogRanking {
        val rankings = floorPercents.mapIndexed { index, percent ->
            FFlogRanking.Data.CharacterData.Character.ZoneRankings.Ranking(
                allStars = null,
                bestAmount = null,
                bestSpec = spec,
                encounter = FFlogRanking.Data.CharacterData.Character.ZoneRankings.Ranking.Encounter(
                    id = index + 1,
                    name = "Boss ${index + 1}"
                ),
                fastestKill = null,
                lockedIn = true,
                medianPercent = percent,
                rankPercent = percent,
                spec = if (percent == null) null else spec,
                totalKills = if (percent == null) 0 else 1
            )
        }

        val allStar = FFlogRanking.Data.CharacterData.Character.ZoneRankings.AllStar(
            partition = 5,
            points = 500.0,
            possiblePoints = 600,
            rank = 1,
            rankPercent = 100.0,
            regionRank = 1,
            serverRank = 1,
            spec = spec,
            total = 100
        )

        val zoneRankings = FFlogRanking.Data.CharacterData.Character.ZoneRankings(
            allStars = listOf(allStar),
            bestPerformanceAverage = null,
            difficulty = 101,
            medianPerformanceAverage = null,
            metric = "dps",
            partition = -1,
            rankings = rankings,
            zone = 73
        )

        return FFlogRanking(
            data = FFlogRanking.Data(
                characterData = FFlogRanking.Data.CharacterData(
                    character = FFlogRanking.Data.CharacterData.Character(
                        zoneRankings = zoneRankings
                    )
                )
            )
        )
    }
}
