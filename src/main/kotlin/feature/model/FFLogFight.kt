package feature.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFLogFight(
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
                @SerialName("fights")
                val fights: List<Fight>
            ) {
                @Serializable
                data class Fight(
                    @SerialName("endTime")
                    val endTime: Int,
                    @SerialName("id")
                    val id: Int,
                    @SerialName("kill")
                    val kill: Boolean,
                    @SerialName("name")
                    val name: String,
                    @SerialName("startTime")
                    val startTime: Int
                )
            }
        }
    }
}