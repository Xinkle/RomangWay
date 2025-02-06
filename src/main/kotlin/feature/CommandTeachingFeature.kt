package feature

import database.CommandTeaching
import database.findCommand
import database.findSimilarCommands
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_COMMAND_NAME = "명령어"
private const val ARGUMENT_COMMAND_DESCRIPTION = "설명"

class CommandTeachingFeature(kord: Kord) :
    CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "등록"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_COMMAND_NAME,
            "명령어를 설정합니다 ex:)!로망웨이, 로망웨이",
            true,
            ArgumentType.STRING
        ),
        CommandArgument(
            ARGUMENT_COMMAND_DESCRIPTION,
            "이 명령어를 호출했을때 낭만봇이 대답해줄 말을 설정합니다.",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()

        try {
            println("New command register requested!")

            val writer = interaction.user.memberData.nick.value ?: interaction.user.data.username
            val writerId = interaction.user.id

            println("Writer -> ${interaction.user.memberData}")

            val commandName = command.strings[ARGUMENT_COMMAND_NAME]!!.trimStart('!')
            val modifiedName = "!$commandName"
            val description = command.strings[ARGUMENT_COMMAND_DESCRIPTION]!!

            transaction {
                val newRecord = CommandTeaching.new {
                    this.name = modifiedName
                    this.description = description
                    this.writer = writer
                    this.isOverridable = false
                    this.createDate = System.currentTimeMillis()
                }
                println(newRecord)
            }

            response.respond {
                content = "$commandName... 기억할게요 <@${writerId}>님!"
            }
        } catch (e: Exception) {
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }

    init {
        kord.on<MessageCreateEvent> { // runs every time a message is created that our bot can read
            // ignore other bots, even ourselves. We only serve humans here!
            if (message.author?.isBot != false) return@on

            val commandName = message.content

            if (commandName.startsWith("!")) {
                findCommand(commandName)?.also {
                    message.channel.createMessage("${it.writer}님이 알려주셨어요! ${it.description}")
                    return@on
                }

                findSimilarCommands(commandName).also { similarNameList ->
                    if (similarNameList.isNotEmpty()) {
                        val responseMessage = StringBuilder()
                            .appendLine("그런 명령어는 없어요 혹시 아래의 명령어를 찾으시나요?")
                            .apply {
                                similarNameList
                                    .distinctBy {
                                        it.name
                                    }
                                    .forEach {
                                        appendLine(it.name)
                                    }
                            }.toString()

                        message.channel.createMessage(responseMessage)
                    }
                }
            }
        }
    }
}
