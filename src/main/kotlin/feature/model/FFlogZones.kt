package feature.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFlogZones(
    @SerialName("data")
    val data: Data?
) {
    @Serializable
    data class Data(
        @SerialName("worldData")
        val worldData: WorldData?
    ) {
        @Serializable
        data class WorldData(
            @SerialName("zones")
            val zones: List<Zone?>?
        ) {
            @Serializable
            data class Zone(
                @SerialName("id")
                val id: Int?,
                @SerialName("name")
                val name: String?,
                @SerialName("encounters")
                val encounters: List<Encounter?>?,
                @SerialName("difficulties")
                val difficulties: List<Difficulty?>?
            ) {
                @Serializable
                data class Encounter(
                    @SerialName("id")
                    val id: Int?,
                    @SerialName("name")
                    val name: String?
                )

                @Serializable
                data class Difficulty(
                    @SerialName("id")
                    val id: Int?,
                    @SerialName("name")
                    val name: String?,
                    @SerialName("sizes")
                    val sizes: List<Int?>?
                )
            }
        }
    }
}
