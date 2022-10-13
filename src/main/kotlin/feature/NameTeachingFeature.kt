package feature

import database.NameTeaching
import database.NameTeachingTable
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext

private const val COMMAND_NAMETEACHING = "register"

class NameTeachingFeature(val kord: Kord) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    init {
        println("$COMMAND_NAMETEACHING module registered!")
        launch {
            kord.createGlobalChatInputCommand(
                COMMAND_NAMETEACHING,
                "이름과 설명 등록"
            ) {
                string("이름", "명령어를 설정합니다 ex:)!낭만봇, 낭만봇") {
                    required = true
                }
                string("설명", "이 명령어를 호출했을때 낭만봇이 대답해줄 말을 설정합니다.") {
                    required = true
                }
            }

            kord.on<GuildChatInputCommandInteractionCreateEvent> {
                val command = interaction.command

                if (command.data.name.value == COMMAND_NAMETEACHING) {
                    val response = interaction.deferPublicResponse()
                    try {
                        println("New register requested!")

                        val writer = interaction.user.memberData.nick.value ?: interaction.user.data.username
                        println("Writer -> ${interaction.user.memberData}")

                        val name = command.strings["이름"]!!.trimStart('!')
                        val modifiedName = "!$name"
                        val description = command.strings["설명"]!!

                        transaction {
                            val newRecord = NameTeaching.new {
                                this.name = modifiedName
                                this.description = description
                                this.writer = writer
                                this.isOverridable = false
                                this.createDate = System.currentTimeMillis()
                            }

                            println(newRecord)
                        }

                        response.respond {
                            content = "$name... 기억할게요 ${writer}님!"
                        }
                    } catch (e: Exception) {
                        println("Error occurred -> $e")

                        response.respond {
                            content = "알수없는 오류가 발생했어요..."
                        }
                    }
                }
            }

            kord.on<MessageCreateEvent> { // runs every time a message is created that our bot can read
                // ignore other bots, even ourselves. We only serve humans here!
                if (message.author?.isBot != false) return@on

                // check if our command is being invoked
                if (message.content.startsWith("!")) {
                    val nameTaught = transaction {
                        NameTeaching.find(NameTeachingTable.name eq message.content).first()
                    }
                    message.channel.createMessage(nameTaught.description)
                }
            }
        }
    }
}