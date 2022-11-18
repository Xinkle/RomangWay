package feature.model

import formatMillisecond

data class FFLogDeathSummary(
    val deathNum: Int,
    val timestamp: String,
    val sourceName: String,
    val targetName: String,
    val killingAbilityName: String
) {
    companion object {
        fun fromFFLogDeath(fflogDeath: FFLogDeath, startTime: Long): List<FFLogDeathSummary> =
            ArrayList<FFLogDeathSummary>().apply {
                fflogDeath.data.reportData.report.events.data.forEachIndexed { idx, data ->
                    add(
                        FFLogDeathSummary(
                            idx,
                            formatMillisecond(data.timestamp.toLong() - startTime),
                            data.source.name,
                            data.target.name,
                            data.killingAbility.name
                        )
                    )
                }
            }
    }

    override fun toString(): String =
        "[$deathNum][$timestamp] ${sourceName}의 ${killingAbilityName}에 의해 $targetName 사망."
}
