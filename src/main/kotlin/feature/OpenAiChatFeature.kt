package feature

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import openai.OpenAIClient
import kotlin.coroutines.CoroutineContext

class OpenAiChatFeature(private val kord: Kord) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    init {
        kord.on<MessageCreateEvent> {
            // ignore other bots, even ourselves. We only serve humans here!
            if (message.author?.isBot != false) return@on

            val chat = message.content

            if (chat.startsWith(";")) {
                val response = OpenAIClient().request(chat.drop(1))
                message.channel.createMessage(response)
            }
        }
    }
}