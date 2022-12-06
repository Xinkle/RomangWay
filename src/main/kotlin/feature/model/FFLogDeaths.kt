package feature.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFLogDeaths(
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
                    val `data`: List<Data>
                ) {
                    @Serializable
                    data class Data(
                        @SerialName("ability")
                        val ability: Ability,
                        @SerialName("actorPotencyRatio")
                        val actorPotencyRatio: Double?,
                        @SerialName("amount")
                        val amount: Int?,
                        @SerialName("buffs")
                        val buffs: String?,
                        @SerialName("directHitPercentage")
                        val directHitPercentage: Int?,
                        @SerialName("expectedAmount")
                        val expectedAmount: Int?,
                        @SerialName("expectedCritRate")
                        val expectedCritRate: Int?,
                        @SerialName("fight")
                        val fight: Int,
                        @SerialName("finalizedAmount")
                        val finalizedAmount: Double?,
                        @SerialName("guessAmount")
                        val guessAmount: Double?,
                        @SerialName("hitType")
                        val hitType: Int?,
                        @SerialName("killerID")
                        val killerID: Int?,
                        @SerialName("killerInstance")
                        val killerInstance: Int?,
                        @SerialName("killingAbility")
                        val killingAbility: KillingAbility?,
                        @SerialName("mitigated")
                        val mitigated: Int?,
                        @SerialName("multiplier")
                        val multiplier: Double?,
                        @SerialName("overkill")
                        val overkill: Int?,
                        @SerialName("packetID")
                        val packetID: Int?,
                        @SerialName("simulated")
                        val simulated: Boolean?,
                        @SerialName("source")
                        val source: Source,
                        @SerialName("sourceInstance")
                        val sourceInstance: Int?,
                        @SerialName("sourceResources")
                        val sourceResources: SourceResources?,
                        @SerialName("target")
                        val target: Target,
                        @SerialName("targetResources")
                        val targetResources: TargetResources?,
                        @SerialName("tick")
                        val tick: Boolean?,
                        @SerialName("timestamp")
                        val timestamp: Int,
                        @SerialName("type")
                        val type: String,
                        @SerialName("unmitigatedAmount")
                        val unmitigatedAmount: Int?
                    ) {
                        @Serializable
                        data class Ability(
                            @SerialName("abilityIcon")
                            val abilityIcon: String,
                            @SerialName("guid")
                            val guid: Int,
                            @SerialName("name")
                            val name: String,
                            @SerialName("type")
                            val type: Int
                        )

                        @Serializable
                        data class KillingAbility(
                            @SerialName("abilityIcon")
                            val abilityIcon: String,
                            @SerialName("guid")
                            val guid: Int,
                            @SerialName("name")
                            val name: String,
                            @SerialName("type")
                            val type: Int
                        )

                        @Serializable
                        data class Source(
                            @SerialName("guid")
                            val guid: Int,
                            @SerialName("icon")
                            val icon: String,
                            @SerialName("id")
                            val id: Int,
                            @SerialName("name")
                            val name: String,
                            @SerialName("server")
                            val server: String?,
                            @SerialName("type")
                            val type: String
                        )

                        @Serializable
                        data class SourceResources(
                            @SerialName("absorb")
                            val absorb: Int,
                            @SerialName("facing")
                            val facing: Int,
                            @SerialName("hitPoints")
                            val hitPoints: Int,
                            @SerialName("maxHitPoints")
                            val maxHitPoints: Int,
                            @SerialName("maxMP")
                            val maxMP: Int,
                            @SerialName("maxTP")
                            val maxTP: Int,
                            @SerialName("mp")
                            val mp: Int,
                            @SerialName("tp")
                            val tp: Int,
                            @SerialName("x")
                            val x: Int,
                            @SerialName("y")
                            val y: Int
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

                        @Serializable
                        data class TargetResources(
                            @SerialName("absorb")
                            val absorb: Int,
                            @SerialName("facing")
                            val facing: Int,
                            @SerialName("hitPoints")
                            val hitPoints: Int,
                            @SerialName("maxHitPoints")
                            val maxHitPoints: Int,
                            @SerialName("maxMP")
                            val maxMP: Int,
                            @SerialName("maxTP")
                            val maxTP: Int,
                            @SerialName("mp")
                            val mp: Int,
                            @SerialName("tp")
                            val tp: Int,
                            @SerialName("x")
                            val x: Int,
                            @SerialName("y")
                            val y: Int
                        )
                    }
                }
            }
        }
    }
}