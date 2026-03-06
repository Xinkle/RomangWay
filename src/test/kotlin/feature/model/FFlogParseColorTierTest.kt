package feature.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FFlogParseColorTierTest {
    @Test
    fun `FFLogs 퍼센트 경계값에 따라 색상 티어가 분류된다`() {
        assertEquals(FFlogParseColorTier.GRAY, FFlogParseColorTier.fromAllStarPercent(null))
        assertEquals(FFlogParseColorTier.GRAY, FFlogParseColorTier.fromAllStarPercent(24.9))
        assertEquals(FFlogParseColorTier.GREEN, FFlogParseColorTier.fromAllStarPercent(25.0))
        assertEquals(FFlogParseColorTier.BLUE, FFlogParseColorTier.fromAllStarPercent(50.0))
        assertEquals(FFlogParseColorTier.PURPLE, FFlogParseColorTier.fromAllStarPercent(75.0))
        assertEquals(FFlogParseColorTier.ORANGE, FFlogParseColorTier.fromAllStarPercent(95.0))
        assertEquals(FFlogParseColorTier.PINK, FFlogParseColorTier.fromAllStarPercent(99.0))
        assertEquals(FFlogParseColorTier.GOLD, FFlogParseColorTier.fromAllStarPercent(100.0))
    }
}
