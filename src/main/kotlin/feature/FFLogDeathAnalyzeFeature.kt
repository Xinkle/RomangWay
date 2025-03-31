package feature

import creat.xinkle.Romangway.GetFFlogDeath
import creat.xinkle.Romangway.GetFFlogFight
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.option
import feature.model.FFLogDeath
import feature.model.FFLogDeathIdSelected
import feature.model.FFLogDeathSummary
import feature.model.FFLogFight
import fflog.FFLogClient
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_LOG_URL = "로그주소"
private const val KEY_DETAIL_ANALYZED = "DEATH_ID"

class FFLogDeathAnalyzeFeature(
    private val kord: Kord,
    private val fflogClient: FFLogClient
) : CoroutineScope, ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
    override val command: String = "데스로그"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_LOG_URL,
            "분석할 로그의 주소.",
            true,
            ArgumentType.STRING
        )
    )

    init {
        launch {
            kord.on<GuildSelectMenuInteractionCreateEvent> {
                if (interaction.componentId == KEY_DETAIL_ANALYZED) {
                    val response = interaction.deferPublicResponse()

                    val ffLogDeathIdSelected =
                        Json.decodeFromString<FFLogDeathIdSelected>(interaction.data.data.values.value?.first()!!)

                    response.respond {
                        content = "${interaction.componentId} -> $ffLogDeathIdSelected"
                    }
                }
            }
        }
    }

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command

        //https://www.fflogs.com/reports/h2AtPxBG3Xmpg4Fd#fight=3
        val url = command.strings[ARGUMENT_LOG_URL]!!

        println("FFlog death report analyzing -> $url")

        val response = interaction.deferEphemeralResponse()

        try {
            val reportCode: String = Url(url).pathSegments.last { it.isNotEmpty() }
            val fightId: Int? = "fight=(.+)&".toRegex().find(Url(url).fragment)?.groupValues?.get(1)?.toInt()
                ?: "fight=(.+)".toRegex().find(Url(url).fragment)?.groupValues?.get(1)?.toInt()

            requireNotNull(fightId) { "Can't find fightId!" }

            val fightQueryResult = getFFlogFight(reportCode, fightId)
            val fightStartTime =
                Json.decodeFromString<FFLogFight>(fightQueryResult).data.reportData.report.fights.first().startTime
            val deathQueryResult = getFFLogDeathReport(reportCode, fightId)


            val fflogDeath: FFLogDeath = Json.decodeFromString(deathQueryResult)
            val ffLogDeathSummaryList = FFLogDeathSummary.fromFFLogDeath(fflogDeath, fightStartTime.toLong())

            val resultString = StringBuilder().apply {
                appendLine("$url 의 분석 결과 입니다.")
            }.toString()

            response.respond {
                content = resultString
                components =
                    mutableListOf(
                        ActionRowBuilder().apply {
                            this.stringSelect(
                                KEY_DETAIL_ANALYZED
                            ) {
                                placeholder = "세부 분석을 원하는 항목을 선택하세요."
                                ffLogDeathSummaryList.forEach {
                                    option(
                                        "$it",
                                        Json.encodeToString(FFLogDeathIdSelected(it.deathNum, reportCode, fightId))
                                    )
                                }
                            }
//                            this.interactionButton(
//                                ButtonStyle.Primary,
//                                "test",
//                            ) {
//                                label = "TEST"
//                            }
                        }
                    )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }

    private suspend fun getFFLogDeathReport(reportCode: String, fightId: Int): String =
        fflogClient.executeQuery(
            GetFFlogDeath(
                GetFFlogDeath.Variables(code = reportCode, fight = fightId)
            )
        )

    private suspend fun getFFlogFight(reportCode: String, fightId: Int): String =
        fflogClient.executeQuery(
            GetFFlogFight(
                GetFFlogFight.Variables(code = reportCode, fight = fightId)
            )
        )
}