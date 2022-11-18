package feature

import creat.xinkle.Romangway.GetFFlogDeath
import creat.xinkle.Romangway.GetFFlogFight
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.cache.data.ComponentData
import dev.kord.core.entity.component.Component
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.component.MessageComponentBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import feature.model.FFLogDeath
import feature.model.FFLogDeathSummary
import feature.model.FFLogFight
import fflog.FFLogClient
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_LOG_URL = "로그주소"

class FFLogDeathAnalyzeFeature(
    private val kord: Kord,
    private val fflogClient: FFLogClient
) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
    override val command: String = "데스로그"

    init {
        println("$command module registered!")

        launch {
            kord.createGlobalChatInputCommand(
                command, "프프로그에서 사망로그를 분석합니다.."
            ) {
                string(ARGUMENT_LOG_URL, "분석할 로그의 주소.") {
                    required = true
                }
            }
        }
    }

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command

        //https://www.fflogs.com/reports/h2AtPxBG3Xmpg4Fd#fight=3
        val url = command.strings[ARGUMENT_LOG_URL]!!

        println("FFlog death report analyzing -> $url")

        val response = interaction.deferPublicResponse()

        try {
            val reportCode: String = Url(url).pathSegments.last()
            val fightId: Int? = "fight=(.)".toRegex().find(Url(url).fragment)?.groupValues?.get(1)?.toInt()

            requireNotNull(fightId) { "Can't find fightId!" }

            val fightQueryResult = getFFlogFight(reportCode, fightId)
            val fightStartTime =
                Json.decodeFromString<FFLogFight>(fightQueryResult).data.reportData.report.fights.first().startTime
            val deathQueryResult = getFFLogDeathReport(reportCode, fightId)


            val fflogDeath: FFLogDeath = Json.decodeFromString(deathQueryResult)
            val ffLogDeathSummaryList = FFLogDeathSummary.fromFFLogDeath(fflogDeath, fightStartTime.toLong())

            val resultString = StringBuilder().apply {
                appendLine("$url 의 분석 결과 입니다.")
                ffLogDeathSummaryList.forEach {
                    appendLine(it.toString())
                }
            }.toString()

            response.respond {
                content = resultString
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