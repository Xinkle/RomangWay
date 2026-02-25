package feature

import creat.xinkle.Romangway.GetFFlogRanking
import creat.xinkle.Romangway.GetFFlogSavageZones
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import feature.model.FFlogRanking
import feature.model.FFlogRankingSummary
import feature.model.FFlogZones
import fflog.FFLogClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_NAME = "이름"
private const val ARGUMENT_SERVER = "서버"
private const val ARGUMENT_EXPOSABLE = "공개여부"

private data class SavageRaidZoneCandidate(
    val zoneId: Int,
    val zoneName: String,
    val difficultyId: Int
)

class FFLogFeature(
    private val fflogClient: FFLogClient
) : CoroutineScope, ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val serverMapping = mapOf(
        "톤베리" to "Tonberry",
        "카벙클" to "Carbuncle",
        "초코보" to "Chocobo",
        "펜리르" to "Fenrir",
        "모그리" to "Moogle"
    )
    override val command: String = "프프로그"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_NAME,
            "가져올 유저의 이름.",
            true,
            ArgumentType.STRING
        ),
        CommandArgument(
            ARGUMENT_SERVER,
            "가져올 유저의 서버",
            true,
            ArgumentType.STRING
        ),
        CommandArgument(
            ARGUMENT_EXPOSABLE,
            "조회한 로그의 공개여부",
            true,
            ArgumentType.BOOLEAN
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command

        val name = command.strings[ARGUMENT_NAME]!!
        val server = command.strings[ARGUMENT_SERVER]!!
        val isExposable = command.booleans[ARGUMENT_EXPOSABLE]!!
        val mappedServer = serverMapping[server]

        val response = if (isExposable) {
            interaction.deferPublicResponse()
        } else {
            interaction.deferEphemeralResponse()
        }

        try {
            requireNotNull(mappedServer) { "서버 이름이 올바르지 않습니다." }

            val ranking = getFFlog(name, mappedServer)
            val specNames = ranking.data?.characterData?.character?.zoneRankings?.allStars
                ?.mapNotNull { it?.spec }
                ?.distinct()
                ?: emptyList()
            val savageRaidZoneCandidates = getSavageRaidZoneCandidates()

            val summaries = specNames.mapNotNull { specName ->
                val specRanking = findLatestSavageClearRanking(
                    name = name,
                    server = mappedServer,
                    spec = specName,
                    savageRaidZoneCandidates = savageRaidZoneCandidates
                ) ?: return@mapNotNull null

                if (!specRanking.hasTierClearRanking()) {
                    return@mapNotNull null
                }

                FFlogRankingSummary.fromRanking(specRanking, name, server)
            }

            val responseContent = if (summaries.isNotEmpty()) {
                summaries.joinToString("\n\n")
            } else {
                FFlogRankingSummary.fromRanking(ranking, name, server).toString()
            }

            response.respond {
                content = responseContent
            }
        } catch (e: Exception) {
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }

    private fun FFlogRanking.hasTierClearRanking(): Boolean {
        val rankings = data?.characterData?.character?.zoneRankings?.rankings ?: return false
        val finalEncounterRanking = rankings.lastOrNull() ?: return false
        return (finalEncounterRanking.totalKills ?: 0) > 0
    }

    private fun FFlogRanking.getZoneId(): Int? =
        data?.characterData?.character?.zoneRankings?.zone

    private suspend fun findLatestSavageClearRanking(
        name: String,
        server: String,
        spec: String,
        savageRaidZoneCandidates: List<SavageRaidZoneCandidate>
    ): FFlogRanking? {
        val latestRanking = getFFlog(name = name, server = server, spec = spec)
        if (latestRanking.hasTierClearRanking()) {
            return latestRanking
        }

        val currentZoneId = latestRanking.getZoneId()

        for (zone in savageRaidZoneCandidates) {
            if (zone.zoneId == currentZoneId) continue

            val ranking = getFFlog(
                name = name,
                server = server,
                spec = spec,
                zoneId = zone.zoneId,
                difficulty = zone.difficultyId
            )

            if (ranking.hasTierClearRanking()) {
                return ranking
            }
        }

        return null
    }

    private fun FFlogZones.toSavageRaidZoneCandidates(): List<SavageRaidZoneCandidate> {
        return data?.worldData?.zones
            ?.asSequence()
            ?.filterNotNull()
            ?.mapNotNull { zone ->
                val savageDifficulty = zone.difficulties
                    ?.filterNotNull()
                    ?.firstOrNull { difficulty ->
                        difficulty.name == "Savage" &&
                            (difficulty.sizes?.filterNotNull()?.contains(8) == true)
                    } ?: return@mapNotNull null

                val encounterCount = zone.encounters?.count { it?.id != null } ?: 0
                if (encounterCount < 4) return@mapNotNull null

                val zoneId = zone.id ?: return@mapNotNull null
                val difficultyId = savageDifficulty.id ?: return@mapNotNull null

                SavageRaidZoneCandidate(
                    zoneId = zoneId,
                    zoneName = zone.name ?: "Unknown Zone",
                    difficultyId = difficultyId
                )
            }
            ?.sortedByDescending { it.zoneId }
            ?.toList()
            ?: emptyList()
    }

    private suspend fun getSavageRaidZoneCandidates(): List<SavageRaidZoneCandidate> {
        val result = fflogClient.executeQuery(GetFFlogSavageZones())
        val zones: FFlogZones = json.decodeFromString(result)

        return zones.toSavageRaidZoneCandidates()
    }

    private suspend fun getFFlog(
        name: String,
        server: String,
        spec: String = "Any",
        zoneId: Int = 0,
        difficulty: Int = 0
    ): FFlogRanking {
        val result = fflogClient.executeQuery(
            GetFFlogRanking(
                GetFFlogRanking.Variables(
                    name = name,
                    server = server,
                    spec = spec,
                    zoneId = zoneId,
                    difficulty = difficulty
                )
            )
        )

        val fflogRanking: FFlogRanking = json.decodeFromString(result)
        println(fflogRanking)

        return fflogRanking
    }
}
