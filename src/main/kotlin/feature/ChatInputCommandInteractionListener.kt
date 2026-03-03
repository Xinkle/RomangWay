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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ChatInputCommandInteractionListener::class.java)
private const val DEFAULT_COMMAND_STEP_TIMEOUT_MS = 90_000L

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

class CommandExecutionTimeoutException(
    val stepName: String,
    val timeoutMs: Long,
    cause: Throwable
) : RuntimeException("$stepName 처리 시간이 ${timeoutMs / 1000}초를 초과했습니다.", cause)

interface ChatInputCommandInteractionListener {
    val command: String
    val arguments: List<CommandArgument>

    suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction)
    suspend fun onGuildChatInputCommandSafely(interaction: ChatInputCommandInteraction) {
        val commandName = interaction.command.data.name.value
        val userId = interaction.user.id.toString()
        val startedAt = System.nanoTime()

        logger.info("커맨드 실행 시작: name={}, userId={}", commandName, userId)
        try {
            onGuildChatInputCommand(interaction)
            logger.info(
                "커맨드 실행 완료: name={}, userId={}, elapsedMs={}",
                commandName,
                userId,
                elapsedMs(startedAt)
            )
        } catch (e: Exception) {
            logger.error(
                "커맨드 실행 실패: name={}, userId={}, elapsedMs={}",
                commandName,
                userId,
                elapsedMs(startedAt),
                e
            )
            handleUnexpectedCommandError(interaction, e)
        }
    }

    suspend fun handleUnexpectedCommandError(interaction: ChatInputCommandInteraction, e: Exception) {
        val errorText = buildUnexpectedErrorMessage(e)

        val originalResponse = runCatching {
            interaction.getOriginalInteractionResponseOrNull()
        }.onFailure { responseError ->
            logger.warn("원본 인터랙션 응답 조회 실패", responseError)
        }.getOrNull()

        if (originalResponse != null) {
            val edited = runCatching {
                originalResponse.edit {
                    content = errorText
                }
            }.onFailure { responseError ->
                logger.warn("원본 인터랙션 응답 수정 실패", responseError)
            }.isSuccess

            if (edited) return
        }

        runCatching {
            interaction.respondEphemeral {
                content = errorText
            }
        }.onFailure { responseError ->
            logger.error("예상치 못한 커맨드 오류 응답 전송 실패", responseError)
        }
    }

    fun buildUnexpectedErrorMessage(e: Exception): String {
        if (e is CommandExecutionTimeoutException) {
            return "요청 처리 시간이 초과되어 명령 처리가 중단되었습니다.\n오류: ${e.message}"
        }

        val detail = (e.message ?: e::class.simpleName ?: "알 수 없는 오류")
            .replace("\n", " ")
            .take(1500)

        return "예상하지 못한 오류가 발생하여 명령 처리가 중단되었습니다.\n오류: $detail"
    }

    suspend fun <T> runWithCommandTimeout(
        stepName: String,
        timeoutMs: Long = DEFAULT_COMMAND_STEP_TIMEOUT_MS,
        block: suspend () -> T
    ): T {
        return try {
            withTimeout(timeoutMs) { block() }
        } catch (e: TimeoutCancellationException) {
            throw CommandExecutionTimeoutException(stepName, timeoutMs, e)
        }
    }

    suspend fun updateDeferredProgress(
        interaction: ChatInputCommandInteraction,
        stepMessage: String
    ): Boolean = updateDeferredMessage(interaction, "진행 중: $stepMessage")

    suspend fun updateDeferredMessage(
        interaction: ChatInputCommandInteraction,
        message: String
    ): Boolean {
        return runCatching {
            val originalResponse = interaction.getOriginalInteractionResponseOrNull() ?: return false
            originalResponse.edit {
                content = message
            }
            true
        }.onFailure { progressError ->
            logger.warn("진행상황 업데이트 실패: command={}", interaction.command.data.name.value, progressError)
        }.getOrDefault(false)
    }

    suspend fun registerCommand(kord: Kord, existCommands: List<DiscordApplicationCommand>) {
        val isExist = existCommands.find { it.name == command } != null

        if (isExist) {
            logger.info("글로벌 커맨드 '{}' 이미 등록되어 있음", command)
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
            logger.info("글로벌 커맨드 '{}' 등록 완료", command)
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

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}
