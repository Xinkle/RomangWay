package feature.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFLogDeath(
    @SerialName("data")
    val `data`: Data
) {
    @Serializable
    data class Data(
        @SerialName("reportData")
        val reportData: ReportData
    ) {
        @Serializable
        data class ReportData(
            @SerialName("report")
            val report: Report
        ) {
            @Serializable
            data class Report(
                @SerialName("events")
                val events: Events
            ) {
                @Serializable
                data class Events(
                    @SerialName("data")
                    val `data`: List<Data>,
                ) {
                    @Serializable
                    data class Data(
                        @SerialName("ability")
                        val ability: Ability,
                        @SerialName("fight")
                        val fight: Int,
                        @SerialName("killerID")
                        val killerID: Int = -1,
                        @SerialName("killerInstance")
                        val killerInstance: Int = -1,
                        @SerialName("killingAbility")
                        val killingAbility: KillingAbility = KillingAbility(),
                        @SerialName("source")
                        val source: Source = Source(),
                        @SerialName("sourceInstance")
                        val sourceInstance: Int = -1,
                        @SerialName("target")
                        val target: Target,
                        @SerialName("timestamp")
                        val timestamp: Int,
                        @SerialName("type")
                        val type: String
                    ) {
                        @Serializable
                        data class Ability(
                            @SerialName("abilityIcon")
                            val abilityIcon: String = "",
                            @SerialName("guid")
                            val guid: Int = -1,
                            @SerialName("name")
                            val name: String = "UNKNOWN",
                            @SerialName("type")
                            val type: Int = -1
                        )

                        @Serializable
                        data class KillingAbility(
                            @SerialName("abilityIcon")
                            val abilityIcon: String = "",
                            @SerialName("guid")
                            val guid: Int = -1,
                            @SerialName("name")
                            val name: String = "UNKNOWN",
                            @SerialName("type")
                            val type: Int = -1
                        )

                        @Serializable
                        data class Source(
                            @SerialName("guid")
                            val guid: Int = -1,
                            @SerialName("icon")
                            val icon: String = "",
                            @SerialName("id")
                            val id: Int = -1,
                            @SerialName("name")
                            val name: String = "UNKNOWN",
                            @SerialName("type")
                            val type: String = "UNKNOWN"
                        )

                        @Serializable
                        data class Target(
                            @SerialName("guid")
                            val guid: Int,
                            @SerialName("icon")
                            val icon: String,
                            @SerialName("id")
                            val id: Int,
                            @SerialName("name")
                            val name: String,
                            @SerialName("server")
                            val server: String,
                            @SerialName("type")
                            val type: String
                        )
                    }
                }
            }
        }
    }
}