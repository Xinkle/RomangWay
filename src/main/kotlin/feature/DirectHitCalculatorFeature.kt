package feature

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil

private const val ARGUMENT_DIRECT_HIT = "직격"

class DirectHitCalculatorFeature : CoroutineScope, ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "직격계산"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_DIRECT_HIT,
            "직격 수치",
            true,
            ArgumentType.INTEGER
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()

        val directHit = command.integers[ARGUMENT_DIRECT_HIT]!!
        val calculatedDirectHit = ((550 * (directHit - 400) / 1900.0)).toInt() / 10.0
        val nextDirectHit = ceil((calculatedDirectHit + 0.1) * 1900 / 55) + 400

        try {
            response.respond {
                content =
                    "현재 직격확률은 $calculatedDirectHit%, 데미지 기대값은 ${calculatedDirectHit * 0.0025 + 1}배, 다음단계를 위한 직격수치는 $nextDirectHit 입니다!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}