package feature

import database.findSimilarCommands
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_COMMAND_NAME = "명령어"

class CommandFindingFeature(private val kord: Kord) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "명령어검색"

    init {
        println("$command module registered!")

        launch {
            kord.createGlobalChatInputCommand(
                command, "명령어를 검색합니다."
            ) {
                string(ARGUMENT_COMMAND_NAME, "검색할 명령어 ex:) 흑마") {
                    required = true
                }
            }
        }
    }

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferEphemeralResponse()

        val commandName = command.strings[ARGUMENT_COMMAND_NAME]!!.trimStart('!')

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