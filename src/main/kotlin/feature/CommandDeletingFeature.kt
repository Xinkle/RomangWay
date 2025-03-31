package feature

import database.findAllCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_COMMAND_NAME = "명령어"

class CommandDeletingFeature : CoroutineScope, ChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    // 명령어 이름은 "삭제"로 설정
    override val command: String = "삭제"

    override val arguments = listOf(
        CommandArgument(
            ARGUMENT_COMMAND_NAME,
            "삭제할 명령어 이름을 입력하세요 (예: !로망웨이)",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val commandInput = interaction.command
        val response = interaction.deferPublicResponse()
        try {
            val writer = interaction.getWriter()
            val writerId = interaction.user.id

            // 인자로 받은 명령어에서 앞의 '!'를 제거한 뒤 다시 붙여 일관된 형식으로 만듭니다.
            val commandName = commandInput.strings[ARGUMENT_COMMAND_NAME]!!.trimStart('!')
            val modifiedName = "!$commandName"

            // 트랜잭션 내에서 해당 명령어를 찾아 is_deleted를 업데이트합니다.
            val deletionSuccess = transaction {
                val commandsToDelete = findAllCommand(modifiedName)
                if (commandsToDelete.isNotEmpty()) {
                    commandsToDelete.forEach {
                        it.isDeleted = true
                    }
                    println("Command $modifiedName marked as deleted by $writer")
                    true
                } else {
                    println("Command $modifiedName not found")
                    false
                }
            }

            if (deletionSuccess) {
                response.respond {
                    content = "$commandName 명령어를 삭제했습니다, <@$writerId>님!"
                }
            } else {
                response.respond {
                    content = "$commandName 명령어가 존재하지 않습니다."
                }
            }
        } catch (e: Exception) {
            println("Error occurred during command deletion: $e")
            response.respond {
                content = "명령어 삭제 중 오류가 발생했습니다."
            }
        }
    }
}
