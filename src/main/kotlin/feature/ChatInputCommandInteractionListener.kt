package feature

import dev.kord.common.entity.DiscordApplicationCommand
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string

data class CommandArgument(
    val argName: String,
    val argDescription: String,
    val isRequired: Boolean,
    val argType: ArgumentType,
    val choices: List<Pair<String, String>>? = null
)

enum class ArgumentType {
    STRING,
    INTEGER,
    BOOLEAN
}

interface ChatInputCommandInteractionListener {
    val command: String
    val arguments: List<CommandArgument>

    suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction)
    suspend fun registerCommand(kord: Kord, existCommands: List<DiscordApplicationCommand>) {
        val isExist = existCommands.find { it.name == command } != null

        if (isExist) {
            println("글로벌 커맨드 '$command' 이미 등록되어 있음")
        } else {
            kord.createGlobalChatInputCommand(
                command, "명령어를 검색합니다."
            ) {
                arguments.forEach { arg ->
                    when (arg.argType) {
                        ArgumentType.STRING -> string(arg.argName, arg.argDescription) {
                            required = arg.isRequired
                            arg.choices?.forEach {
                                choice(it.first, it.second)
                            }
                        }

                        ArgumentType.INTEGER -> integer(arg.argName, arg.argDescription) {
                            required = arg.isRequired
                        }

                        ArgumentType.BOOLEAN -> boolean(arg.argName, arg.argDescription) {
                            required = arg.isRequired
                        }
                    }
                }
            }
            println("글로벌 커맨드 '$command' 등록 완료")
        }
    }

    fun ChatInputCommandInteraction.getWriter(): String =
        if (this is GuildChatInputCommandInteraction) {
            user.nickname
                ?: user.globalName
                ?: user.username
        } else {
            user.globalName
                ?: user.username
        }
}
