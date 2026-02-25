package feature

import dev.kord.common.entity.DiscordApplicationCommand
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
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
    suspend fun onGuildChatInputCommandSafely(interaction: ChatInputCommandInteraction) {
        try {
            onGuildChatInputCommand(interaction)
        } catch (e: Exception) {
            handleUnexpectedCommandError(interaction, e)
        }
    }

    suspend fun handleUnexpectedCommandError(interaction: ChatInputCommandInteraction, e: Exception) {
        e.printStackTrace()

        val errorText = buildUnexpectedErrorMessage(e)

        runCatching {
            val originalResponse = interaction.getOriginalInteractionResponseOrNull()

            if (originalResponse != null) {
                // If the command already deferred/responded, edit the original response to end the loading state.
                originalResponse.edit {
                    content = errorText
                }
                return
            }

            interaction.respondEphemeral {
                content = errorText
            }
        }.onFailure { responseError ->
            println("예상치 못한 커맨드 오류 응답 전송 실패: $responseError")
        }
    }

    fun buildUnexpectedErrorMessage(e: Exception): String {
        val detail = (e.message ?: e::class.simpleName ?: "알 수 없는 오류")
            .replace("\n", " ")
            .take(1500)

        return "예상하지 못한 오류가 발생하여 명령 처리가 중단되었습니다.\n오류: $detail"
    }

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
