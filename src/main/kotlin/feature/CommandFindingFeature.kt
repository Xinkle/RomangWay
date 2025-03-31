package feature

import database.findSimilarCommands
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class CommandFindingFeature : CoroutineScope,
    ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "명령어검색"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            "명령어",
            "검색할 명령어 ex:) 흑마.",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferEphemeralResponse()

        val commandName = command.strings[arguments.firstOrNull()?.argName]!!.trimStart('!')

        findSimilarCommands(commandName).also { similarNameList ->
            if (similarNameList.isEmpty()) {
                response.respond { content = "명령어를 찾을수 없어요..." }
            } else {
                val responseMessage = StringBuilder()
                    .appendLine("아래의 명령어를 찾았어요!")
                    .apply {
                        similarNameList
                            .distinctBy {
                                it.name
                            }
                            .forEach {
                                appendLine(it.name)
                            }
                    }.toString()

                response.respond { content = responseMessage }
            }
        }
    }
}