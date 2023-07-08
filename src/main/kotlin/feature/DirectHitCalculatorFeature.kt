package feature

import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil

private const val ARGUMENT_DIRECT_HIT = "직격"

class DirectHitCalculatorFeature(private val kord: Kord) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "직격계산"

    init {
        println("$command module registered!")
        launch {
            kord.createGlobalChatInputCommand(
                command, "직격 관련 수치를 계산합니다."
            ) {
                integer(ARGUMENT_DIRECT_HIT, "직격 수치") {

                    required = true
                }
            }
        }
    }

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()

        val directHit = command.integers[ARGUMENT_DIRECT_HIT]!!
        val calculatedDirectHit = ((550 * (directHit - 400) / 1900.0)).toInt() / 10.0
        val nextDirectHit = ceil((calculatedDirectHit + 0.1) * 1900 / 55) + 400

        try {
            val builder = InteractionResponseModifyBuilder().apply {
                content =
                    "현재 직격확률은 $calculatedDirectHit%, 데미지 기대값은 ${calculatedDirectHit * 0.0025 + 1}배, 다음단계를 위한 직격수치는 $nextDirectHit 입니다!"
                components =
                    mutableListOf(
                        ActionRowBuilder().apply {
                            textInput(
                                TextInputStyle.Short,
                                "test",
                                "FightID"
                            )
                        }
                    )
            }

            println(builder.toRequest().toString())

            response.respond {
                content =
                    "현재 직격확률은 $calculatedDirectHit%, 데미지 기대값은 ${calculatedDirectHit * 0.0025 + 1}배, 다음단계를 위한 직격수치는 $nextDirectHit 입니다!"
                components =
                    mutableListOf(
                        ActionRowBuilder().apply {
                            this.
                            textInput(
                                TextInputStyle.Short,
                                "test",
                                "FightID"
                            )
                        }
                    )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}