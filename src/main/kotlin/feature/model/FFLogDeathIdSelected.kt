package feature.model

import kotlinx.serialization.Serializable

@Serializable
data class FFLogDeathIdSelected(
    val deathNum: Int,
    val reportCode: String,
    val fightId: Int
)