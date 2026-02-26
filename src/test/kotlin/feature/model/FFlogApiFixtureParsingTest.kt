package feature.model

import fflog.FFlogJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FFlogApiFixtureParsingTest {
    @Test
    fun `zoneRankings 응답 fixture는 추가 필드와 누락 필드가 있어도 파싱된다`() {
        val text = javaClass.classLoader
            .getResource("fflogs/zone_rankings_response_with_unknown_and_missing_fields.json")!!
            .readText()

        val parsed: FFlogRanking = FFlogJson.parser.decodeFromString(text)
        val zoneRankings = parsed.data?.characterData?.character?.zoneRankings
        val thirdRanking = zoneRankings?.rankings?.getOrNull(2)

        assertNotNull(zoneRankings)
        assertEquals(73, zoneRankings.zone)
        assertEquals("Sage", zoneRankings.allStars?.firstOrNull()?.spec)
        assertNull(thirdRanking?.bestSpec) // missing in fixture, parsed as null
        assertNull(thirdRanking?.rankPercent)
    }
}
