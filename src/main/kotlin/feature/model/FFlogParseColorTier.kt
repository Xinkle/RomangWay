package feature.model

import dev.kord.common.Color

enum class FFlogParseColorTier(
    val label: String,
    val hexColor: Int
) {
    GRAY("회색", 0x9D9D9D),
    GREEN("초록", 0x1EFF00),
    BLUE("파랑", 0x0070FF),
    PURPLE("보라", 0xA335EE),
    ORANGE("주황", 0xFF8000),
    PINK("핑크", 0xE268A8),
    GOLD("골드", 0xE5CC80);

    val accentColor: Color
        get() = Color(hexColor)

    companion object {
        fun fromAllStarPercent(percent: Double?): FFlogParseColorTier {
            if (percent == null) return GRAY
            val normalized = percent.coerceIn(0.0, 100.0)

            return when {
                normalized >= 100.0 -> GOLD
                normalized >= 99.0 -> PINK
                normalized >= 95.0 -> ORANGE
                normalized >= 75.0 -> PURPLE
                normalized >= 50.0 -> BLUE
                normalized >= 25.0 -> GREEN
                else -> GRAY
            }
        }
    }
}
