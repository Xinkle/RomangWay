package feature.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FFlogParseColorTierTest {
    @Test
    fun `FFLogs 퍼센트 경계값에 따라 색상 티어가 분류된다`() {
        assertEquals(FFlogParseColorTier.GRAY, FFlogParseColorTier.fromAllStarPercent(null))
        assertEquals(FFlogParseColorTier.GRAY, FFlogParseColorTier.fromAllStarPercent(24.999))
        assertEquals(FFlogParseColorTier.GREEN, FFlogParseColorTier.fromAllStarPercent(25.0))
        assertEquals(FFlogParseColorTier.GREEN, FFlogParseColorTier.fromAllStarPercent(49.999))
        assertEquals(FFlogParseColorTier.BLUE, FFlogParseColorTier.fromAllStarPercent(50.0))
        assertEquals(FFlogParseColorTier.BLUE, FFlogParseColorTier.fromAllStarPercent(74.999))
        assertEquals(FFlogParseColorTier.PURPLE, FFlogParseColorTier.fromAllStarPercent(75.0))
        assertEquals(FFlogParseColorTier.PURPLE, FFlogParseColorTier.fromAllStarPercent(94.999))
        assertEquals(FFlogParseColorTier.ORANGE, FFlogParseColorTier.fromAllStarPercent(95.0))
        assertEquals(FFlogParseColorTier.ORANGE, FFlogParseColorTier.fromAllStarPercent(98.999))
        assertEquals(FFlogParseColorTier.PINK, FFlogParseColorTier.fromAllStarPercent(99.0))
        assertEquals(FFlogParseColorTier.PINK, FFlogParseColorTier.fromAllStarPercent(99.999))
        assertEquals(FFlogParseColorTier.GOLD, FFlogParseColorTier.fromAllStarPercent(100.0))
        assertEquals(FFlogParseColorTier.GOLD, FFlogParseColorTier.fromAllStarPercent(123.4))
    }

    @Test
    fun `각 티어의 색상 코드는 FFLogs_WarcraftLogs 팔레트와 동일하다`() {
        assertEquals(0x9D9D9D, FFlogParseColorTier.GRAY.hexColor)
        assertEquals(0x1EFF00, FFlogParseColorTier.GREEN.hexColor)
        assertEquals(0x0070FF, FFlogParseColorTier.BLUE.hexColor)
        assertEquals(0xA335EE, FFlogParseColorTier.PURPLE.hexColor)
        assertEquals(0xFF8000, FFlogParseColorTier.ORANGE.hexColor)
        assertEquals(0xE268A8, FFlogParseColorTier.PINK.hexColor)
        assertEquals(0xE5CC80, FFlogParseColorTier.GOLD.hexColor)
    }
}
