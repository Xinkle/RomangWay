package feature

import feature.model.FFlogRanking
import feature.model.FFlogZones
import fflog.FFlogJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FFlogRankingLogicTest {
    @Test
    fun `Savage 8인 레이드 티어 후보만 추출하고 최신 zoneId 순으로 정렬한다`() {
        val zones = parseResource<FFlogZones>("fflogs/world_zones_mixed_sample.json")

        val candidates = FFlogRankingLogic.toSavageRaidZoneCandidates(zones)

        assertEquals(listOf(73, 68), candidates.map { it.zoneId })
        assertEquals(listOf(101, 101), candidates.map { it.difficultyId })
    }

    @Test
    fun `최종 보스 totalKills 기준으로 티어 클리어 여부를 판정한다`() {
        val cleared = createRankingWithFinalKill(1)
        val notCleared = createRankingWithFinalKill(0)

        assertTrue(FFlogRankingLogic.hasTierClearRanking(cleared))
        assertFalse(FFlogRankingLogic.hasTierClearRanking(notCleared))
    }

    @Test
    fun `최종 보스 미클리어여도 층 진행 기록이 있으면 현재 티어 진행도로 판정한다`() {
        val partialProgress = createRankingWithKills(listOf(3, 1, 0, 0, 0))
        val noProgress = createRankingWithKills(listOf(0, 0, 0, 0, 0))

        assertTrue(FFlogRankingLogic.hasAnyTierProgress(partialProgress))
        assertFalse(FFlogRankingLogic.hasAnyTierProgress(noProgress))
        assertFalse(FFlogRankingLogic.hasTierClearRanking(partialProgress))
    }

    @Test
    fun `allStars에서 직업 목록을 중복 없이 추출한다`() {
        val ranking = parseResource<FFlogRanking>("fflogs/zone_rankings_response_with_unknown_and_missing_fields.json")

        assertEquals(listOf("Sage", "WhiteMage"), FFlogRankingLogic.extractSpecNames(ranking))
    }

    private fun createRankingWithFinalKill(finalKill: Int): FFlogRanking {
        return createRankingWithKills(listOf(1, 1, 1, 1, finalKill))
    }

    private fun createRankingWithKills(killsPerEncounter: List<Int>): FFlogRanking {
        val rankings = killsPerEncounter.mapIndexed { idx, kills ->
            FFlogRanking.Data.CharacterData.Character.ZoneRankings.Ranking(
                allStars = null,
                bestAmount = 1.0,
                bestSpec = "Sage",
                encounter = FFlogRanking.Data.CharacterData.Character.ZoneRankings.Ranking.Encounter(
                    id = idx + 1,
                    name = "Boss ${idx + 1}"
                ),
                fastestKill = 1,
                lockedIn = true,
                medianPercent = 50.0,
                rankPercent = 50.0,
                spec = "Sage",
                totalKills = kills
            )
        }

        val zoneRankings = FFlogRanking.Data.CharacterData.Character.ZoneRankings(
            allStars = listOf(
                FFlogRanking.Data.CharacterData.Character.ZoneRankings.AllStar(
                    partition = 5,
                    points = 1.0,
                    possiblePoints = 600,
                    rank = 1,
                    rankPercent = 1.0,
                    regionRank = 1,
                    serverRank = 1,
                    spec = "Sage",
                    total = 1
                )
            ),
            bestPerformanceAverage = 1.0,
            difficulty = 101,
            medianPerformanceAverage = 1.0,
            metric = "dps",
            partition = -1,
            rankings = rankings,
            zone = 73
        )

        return FFlogRanking(
            data = FFlogRanking.Data(
                characterData = FFlogRanking.Data.CharacterData(
                    character = FFlogRanking.Data.CharacterData.Character(zoneRankings = zoneRankings)
                )
            )
        )
    }

    private inline fun <reified T> parseResource(path: String): T {
        val text = javaClass.classLoader.getResource(path)!!.readText()
        return FFlogJson.parser.decodeFromString(text)
    }
}
