package feature

import creat.xinkle.Romangway.GetFFlogRanking
import creat.xinkle.Romangway.GetFFlogSavageZones
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.option
import feature.model.FFlogRanking
import feature.model.FFlogRankingSummary
import feature.model.FFlogZones
import fflog.FFlogJson
import fflog.FFLogClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import dev.kord.rest.builder.message.EmbedBuilder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_NAME = "이름"
private const val ARGUMENT_SERVER = "서버"
private const val ARGUMENT_EXPOSABLE = "공개여부"
private const val FFLOG_RANKING_SELECT_PREFIX = "FFLOG_RANKING_SPEC"

class FFLogFeature(
    private val kord: Kord,
    private val fflogClient: FFLogClient
) : CoroutineScope, ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
    private val json = FFlogJson.parser
    private val rankingSelectSessions = ConcurrentHashMap<String, RankingSelectSession>()

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

    init {
        launch {
            kord.on<GuildSelectMenuInteractionCreateEvent> {
                if (!interaction.componentId.startsWith("$FFLOG_RANKING_SELECT_PREFIX:")) return@on

                val sessionId = interaction.componentId.substringAfter(":", missingDelimiterValue = "")
                val session = rankingSelectSessions[sessionId] ?: return@on
                val selectedIndex = interaction.values.firstOrNull()?.toIntOrNull() ?: return@on
                val selectedSummary = session.summaries.getOrNull(selectedIndex) ?: return@on

                if (session.isPublic) {
                    interaction.updatePublicMessage {
                        content = ""
                        embeds = mutableListOf(buildRankingEmbed(session, selectedSummary))
                        components = mutableListOf(buildRankingSelectActionRow(sessionId, session.summaries, selectedIndex))
                    }
                } else {
                    interaction.updateEphemeralMessage {
                        content = ""
                        embeds = mutableListOf(buildRankingEmbed(session, selectedSummary))
                        components = mutableListOf(buildRankingSelectActionRow(sessionId, session.summaries, selectedIndex))
                    }
                }
            }
        }
    }

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
            val specNames = FFlogRankingLogic.extractSpecNames(ranking)
            val savageRaidZoneCandidates = getSavageRaidZoneCandidates()

            val summaries = specNames.mapNotNull { specName ->
                val specRanking = findLatestSavageClearRanking(
                    name = name,
                    server = mappedServer,
                    spec = specName,
                    savageRaidZoneCandidates = savageRaidZoneCandidates
                ) ?: return@mapNotNull null

                if (!FFlogRankingLogic.hasTierClearRanking(specRanking)) {
                    return@mapNotNull null
                }

                FFlogRankingSummary.fromRanking(specRanking, name, server)
            }.sortedByDescending { it.allStarPointValueOrDefault() }

            val displaySummaries = if (summaries.isNotEmpty()) {
                summaries
            } else {
                listOf(FFlogRankingSummary.fromRanking(ranking, name, server))
            }
            val defaultIndex = 0
            val selectedSummary = displaySummaries[defaultIndex]
            val sessionId = UUID.randomUUID().toString()

            rankingSelectSessions[sessionId] = RankingSelectSession(
                name = name,
                server = server,
                isPublic = isExposable,
                summaries = displaySummaries
            )

            response.respond {
                embeds = mutableListOf(
                    buildRankingEmbed(
                        session = rankingSelectSessions.getValue(sessionId),
                        selectedSummary = selectedSummary
                    )
                )
                if (displaySummaries.size > 1) {
                    components = mutableListOf(
                        buildRankingSelectActionRow(
                            sessionId = sessionId,
                            summaries = displaySummaries,
                            selectedIndex = defaultIndex
                        )
                    )
                }
                content = ""
            }
        } catch (e: Exception) {
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }

    private suspend fun findLatestSavageClearRanking(
        name: String,
        server: String,
        spec: String,
        savageRaidZoneCandidates: List<SavageRaidZoneCandidate>
    ): FFlogRanking? {
        val latestRanking = getFFlog(name = name, server = server, spec = spec)
        if (FFlogRankingLogic.hasTierClearRanking(latestRanking)) {
            return latestRanking
        }

        val currentZoneId = FFlogRankingLogic.getZoneId(latestRanking)

        for (zone in savageRaidZoneCandidates) {
            if (zone.zoneId == currentZoneId) continue

            val ranking = getFFlog(
                name = name,
                server = server,
                spec = spec,
                zoneId = zone.zoneId,
                difficulty = zone.difficultyId
            )

            if (FFlogRankingLogic.hasTierClearRanking(ranking)) {
                return ranking
            }
        }

        return null
    }

    private suspend fun getSavageRaidZoneCandidates(): List<SavageRaidZoneCandidate> {
        val result = fflogClient.executeQuery(GetFFlogSavageZones())
        val zones: FFlogZones = json.decodeFromString(result)

        return FFlogRankingLogic.toSavageRaidZoneCandidates(zones)
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

    private fun buildRankingEmbed(
        session: RankingSelectSession,
        selectedSummary: FFlogRankingSummary
    ): EmbedBuilder = EmbedBuilder().apply {
        title = "프프로그 조회 결과"
        description = """
            이름: ${session.name}
            서버: ${session.server}

            ${selectedSummary.toDetailDescriptionWithoutIdentity()}
        """.trimIndent()
    }

    private fun buildRankingSelectActionRow(
        sessionId: String,
        summaries: List<FFlogRankingSummary>,
        selectedIndex: Int
    ): ActionRowBuilder = ActionRowBuilder().apply {
        stringSelect("$FFLOG_RANKING_SELECT_PREFIX:$sessionId") {
            placeholder = "직업을 선택하세요 (기본: 올스타 점수 최고)"
            summaries.forEachIndexed { index, summary ->
                option(
                    summary.job ?: "Unknown",
                    index.toString()
                ) {
                    description = "올스타 ${summary.allStarPoint ?: "N/A"}"
                    default = index == selectedIndex
                }
            }
        }
    }

    private data class RankingSelectSession(
        val name: String,
        val server: String,
        val isPublic: Boolean,
        val summaries: List<FFlogRankingSummary>
    )
}
