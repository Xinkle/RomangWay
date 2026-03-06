package feature

import creat.xinkle.Romangway.GetFFlogRanking
import creat.xinkle.Romangway.GetFFlogSavageZones
import dev.kord.common.entity.MessageFlag
import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.ContainerBuilder
import dev.kord.rest.builder.component.MessageComponentBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.messageFlags
import feature.model.FFlogRanking
import feature.model.FFlogParseColorTier
import feature.model.FFlogRankingSummary
import feature.model.FFlogZones
import fflog.FFlogJson
import fflog.FFLogClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_NAME = "이름"
private const val ARGUMENT_SERVER = "서버"
private const val ARGUMENT_EXPOSABLE = "공개여부"
private const val FFLOG_RANKING_SELECT_PREFIX = "FFLOG_RANKING_SPEC"
private const val FFLOG_ZONE_QUERY_TIMEOUT_MS = 20_000L
private const val FFLOG_RANKING_QUERY_TIMEOUT_MS = 25_000L

private val logger = LoggerFactory.getLogger(FFLogFeature::class.java)

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
                        messageFlags { +MessageFlag.IsComponentsV2 }
                        content = null
                        embeds = null
                        components = buildRankingComponents(session, selectedSummary, sessionId, selectedIndex)
                    }
                } else {
                    interaction.updateEphemeralMessage {
                        messageFlags { +MessageFlag.IsComponentsV2 }
                        content = null
                        embeds = null
                        components = buildRankingComponents(session, selectedSummary, sessionId, selectedIndex)
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
        response.respond {
            content = "진행 중: 프프로그 조회를 시작합니다."
        }

        requireNotNull(mappedServer) { "서버 이름이 올바르지 않습니다." }

        updateDeferredProgress(interaction, "1/5 최상위 영식 레이드 정보를 조회 중입니다.")
        val zones = runWithCommandTimeout("최상위 영식 레이드 조회", FFLOG_ZONE_QUERY_TIMEOUT_MS) {
            getFFlogSavageZones()
        }
        val currentTopSavageZone = requireNotNull(FFlogRankingLogic.findCurrentTopSavageZoneCandidate(zones)) {
            "현재 Savage 레이드 정보를 찾을 수 없습니다."
        }

        updateDeferredProgress(
            interaction,
            "2/5 ${currentTopSavageZone.zoneName} 기준 기본 랭킹을 조회 중입니다."
        )
        val currentTierRanking = runWithCommandTimeout("기본 랭킹 조회", FFLOG_RANKING_QUERY_TIMEOUT_MS) {
            getFFlog(
                name = name,
                server = mappedServer,
                zoneId = currentTopSavageZone.zoneId,
                difficulty = currentTopSavageZone.difficultyId
            )
        }

        updateDeferredProgress(interaction, "3/5 클리어 직업 목록을 정리 중입니다.")
        val specNames = FFlogRankingLogic.extractSpecNames(currentTierRanking)

        val summaries = specNames.mapIndexedNotNull { index, specName ->
            updateDeferredProgress(
                interaction,
                "4/5 직업별 랭킹 조회 중 (${index + 1}/${specNames.size}): $specName"
            )
            val specRanking = runWithCommandTimeout("직업별 랭킹 조회($specName)", FFLOG_RANKING_QUERY_TIMEOUT_MS) {
                getFFlog(
                    name = name,
                    server = mappedServer,
                    spec = specName,
                    zoneId = currentTopSavageZone.zoneId,
                    difficulty = currentTopSavageZone.difficultyId
                )
            }

            if (!FFlogRankingLogic.hasAnyTierProgress(specRanking)) {
                return@mapIndexedNotNull null
            }

            FFlogRankingSummary.fromRanking(specRanking, name, server)
        }.sortedByDescending { it.allStarPointValueOrDefault() }

        updateDeferredProgress(interaction, "5/5 조회 결과를 정리하여 전송 중입니다.")
        val originalResponse = interaction.getOriginalInteractionResponseOrNull()
        if (originalResponse != null) {
            originalResponse.edit {
                messageFlags { +MessageFlag.IsComponentsV2 }
                content = null
                embeds = null
                if (summaries.isEmpty()) {
                    components = mutableListOf(
                        buildNoClearRankingContainer(
                            raidName = currentTopSavageZone.zoneName,
                            name = name,
                            server = server
                        )
                    )
                } else {
                    val defaultIndex = 0
                    val selectedSummary = summaries[defaultIndex]
                    val sessionId = UUID.randomUUID().toString()

                    rankingSelectSessions[sessionId] = RankingSelectSession(
                        raidName = currentTopSavageZone.zoneName,
                        name = name,
                        server = server,
                        isPublic = isExposable,
                        summaries = summaries
                    )

                    components = buildRankingComponents(
                        session = rankingSelectSessions.getValue(sessionId),
                        selectedSummary = selectedSummary,
                        sessionId = sessionId,
                        selectedIndex = defaultIndex
                    )
                }
            }
        } else {
            logger.warn("원본 응답이 없어 최종 결과를 신규 응답으로 전송합니다.")
            response.respond {
                messageFlags { +MessageFlag.IsComponentsV2 }
                content = null
                embeds = null
                if (summaries.isEmpty()) {
                    components = mutableListOf(
                        buildNoClearRankingContainer(
                            raidName = currentTopSavageZone.zoneName,
                            name = name,
                            server = server
                        )
                    )
                } else {
                    val defaultIndex = 0
                    val selectedSummary = summaries[defaultIndex]
                    val sessionId = UUID.randomUUID().toString()

                    rankingSelectSessions[sessionId] = RankingSelectSession(
                        raidName = currentTopSavageZone.zoneName,
                        name = name,
                        server = server,
                        isPublic = isExposable,
                        summaries = summaries
                    )

                    components = buildRankingComponents(
                        session = rankingSelectSessions.getValue(sessionId),
                        selectedSummary = selectedSummary,
                        sessionId = sessionId,
                        selectedIndex = defaultIndex
                    )
                }
            }
        }
    }

    private suspend fun getFFlogSavageZones(): FFlogZones {
        val result = fflogClient.executeQuery(GetFFlogSavageZones())
        return json.decodeFromString(result)
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
        logger.debug("FFLog ranking 조회 응답 파싱 완료: name={}, server={}, spec={}", name, server, spec)

        return fflogRanking
    }

    private fun buildRankingContainer(
        session: RankingSelectSession,
        selectedSummary: FFlogRankingSummary
    ): ContainerBuilder {
        val colorTier = FFlogParseColorTier.fromAllStarPercent(selectedSummary.allStarPercentValueOrNull())
        return ContainerBuilder().apply {
            accentColor = colorTier.accentColor
            textDisplay(
                buildString {
                    appendLine("## ${session.raidName}")
                    appendLine("이름: ${session.name}")
                    appendLine("서버: ${session.server}")
                    appendLine()
                    appendLine("직업: ${selectedSummary.toEmbedFieldName()}")
                    appendLine("올스타 포인트: ${selectedSummary.allStarPoint ?: "N/A"}")
                    appendLine("올스타 백분위: ${selectedSummary.allStarPercent ?: "N/A"}")
                    appendLine("올스타 등수: ${selectedSummary.allStarRankings ?: "N/A"}")
                    appendLine("1층: ${selectedSummary.firstFloor ?: "N/A"}")
                    appendLine("2층: ${selectedSummary.secondFloor ?: "N/A"}")
                    appendLine("3층: ${selectedSummary.thirdFloor ?: "N/A"}")
                    appendLine("4층전반: ${selectedSummary.fourthFloor ?: "N/A"}")
                    append("4층후반: ${selectedSummary.fifthFloor ?: "N/A"}")
                }
            )
        }
    }

    private fun buildNoClearRankingContainer(
        raidName: String,
        name: String,
        server: String
    ): ContainerBuilder = ContainerBuilder().apply {
        accentColor = FFlogParseColorTier.GRAY.accentColor
        textDisplay(
            buildString {
                appendLine("## $raidName")
                appendLine("이름: $name")
                appendLine("서버: $server")
                appendLine()
                append("클리어 기록 없음")
            }
        )
    }

    private fun buildRankingComponents(
        session: RankingSelectSession,
        selectedSummary: FFlogRankingSummary,
        sessionId: String,
        selectedIndex: Int
    ): MutableList<MessageComponentBuilder> = mutableListOf<MessageComponentBuilder>().apply {
        add(buildRankingContainer(session, selectedSummary))
        if (session.summaries.size > 1) {
            add(buildRankingSelectActionRow(sessionId, session.summaries, selectedIndex))
        }
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
                    summary.toJobKoreanName(),
                    index.toString()
                ) {
                    description = "올스타 ${summary.allStarPoint ?: "N/A"}"
                    default = index == selectedIndex
                }
            }
        }
    }

    private data class RankingSelectSession(
        val raidName: String,
        val name: String,
        val server: String,
        val isPublic: Boolean,
        val summaries: List<FFlogRankingSummary>
    )
}
