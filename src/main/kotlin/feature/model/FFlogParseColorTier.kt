package feature.model

import dev.kord.common.Color

enum class FFlogParseColorTier(
    val label: String,
    val accentColor: Color
) {
    GRAY("회색", Color(102, 102, 102)),
    GREEN("초록", Color(30, 255, 0)),
    BLUE("파랑", Color(0, 112, 255)),
    PURPLE("보라", Color(163, 53, 238)),
    ORANGE("주황", Color(255, 128, 0)),
    PINK("핑크", Color(226, 104, 168)),
    GOLD("골드", Color(229, 204, 128));

    companion object {
        fun fromAllStarPercent(percent: Double?): FFlogParseColorTier {
            if (percent == null) return GRAY

            return when {
                percent >= 100.0 -> GOLD
                percent >= 99.0 -> PINK
                percent >= 95.0 -> ORANGE
                percent >= 75.0 -> PURPLE
                percent >= 50.0 -> BLUE
                percent >= 25.0 -> GREEN
                else -> GRAY
            }
        }
    }
}
