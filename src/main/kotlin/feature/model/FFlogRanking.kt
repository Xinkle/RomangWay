package feature.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFlogRanking(
    @SerialName("data")
    val `data`: Data?
) {
    @Serializable
    data class Data(
        @SerialName("characterData")
        val characterData: CharacterData?
    ) {
        @Serializable
        data class CharacterData(
            @SerialName("character")
            val character: Character?
        ) {
            @Serializable
            data class Character(
                @SerialName("zoneRankings")
                val zoneRankings: ZoneRankings?
            ) {
                @Serializable
                data class ZoneRankings(
                    @SerialName("allStars")
                    val allStars: List<AllStar?>?,
                    @SerialName("bestPerformanceAverage")
                    val bestPerformanceAverage: Double?,
                    @SerialName("difficulty")
                    val difficulty: Int?,
                    @SerialName("medianPerformanceAverage")
                    val medianPerformanceAverage: Double?,
                    @SerialName("metric")
                    val metric: String?,
                    @SerialName("partition")
                    val partition: Int?,
                    @SerialName("rankings")
                    val rankings: List<Ranking?>?,
                    @SerialName("zone")
                    val zone: Int?
                ) {
                    @Serializable
                    data class AllStar(
                        @SerialName("partition")
                        val partition: Int?,
                        @SerialName("points")
                        val points: Double?,
                        @SerialName("possiblePoints")
                        val possiblePoints: Int?,
                        @SerialName("rank")
                        val rank: Int?,
                        @SerialName("rankPercent")
                        val rankPercent: Double?,
                        @SerialName("regionRank")
                        val regionRank: Int?,
                        @SerialName("serverRank")
                        val serverRank: Int?,
                        @SerialName("spec")
                        val spec: String?,
                        @SerialName("total")
                        val total: Int?
                    )

                    @Serializable
                    data class Ranking(
                        @SerialName("allStars")
                        val allStars: AllStars?,
                        @SerialName("bestAmount")
                        val bestAmount: Double?,
                        @SerialName("bestSpec")
                        val bestSpec: String?,
                        @SerialName("encounter")
                        val encounter: Encounter?,
                        @SerialName("fastestKill")
                        val fastestKill: Int?,
                        @SerialName("lockedIn")
                        val lockedIn: Boolean?,
                        @SerialName("medianPercent")
                        val medianPercent: Double?,
                        @SerialName("rankPercent")
                        val rankPercent: Double?,
                        @SerialName("spec")
                        val spec: String?,
                        @SerialName("totalKills")
                        val totalKills: Int?
                    ) {
                        @Serializable
                        data class AllStars(
                            @SerialName("partition")
                            val partition: Int?,
                            @SerialName("points")
                            val points: Double?,
                            @SerialName("possiblePoints")
                            val possiblePoints: Int?,
                            @SerialName("rank")
                            val rank: Int?,
                            @SerialName("rankPercent")
                            val rankPercent: Double?,
                            @SerialName("regionRank")
                            val regionRank: Int?,
                            @SerialName("serverRank")
                            val serverRank: Int?,
                            @SerialName("total")
                            val total: Int?
                        )

                        @Serializable
                        data class Encounter(
                            @SerialName("id")
                            val id: Int?,
                            @SerialName("name")
                            val name: String?
                        )
                    }
                }
            }
        }
    }
}