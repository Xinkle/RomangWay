package feature

import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChatInputCommandInteractionListenerTest {

    private val listener = object : ChatInputCommandInteractionListener {
        override val command: String = "test"
        override val arguments: List<CommandArgument> = emptyList()

        override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) = Unit
    }

    @Test
    fun `runWithCommandTimeout returns result when completed in time`() = runBlocking {
        val result = listener.runWithCommandTimeout("테스트 단계", timeoutMs = 100) {
            "ok"
        }

        assertEquals("ok", result)
    }

    @Test
    fun `runWithCommandTimeout throws timeout exception with step metadata`() = runBlocking {
        val exception = assertFailsWith<CommandExecutionTimeoutException> {
            listener.runWithCommandTimeout("테스트 단계", timeoutMs = 50) {
                delay(200)
                "never"
            }
        }

        assertEquals("테스트 단계", exception.stepName)
        assertEquals(50L, exception.timeoutMs)
        assertTrue(exception.message.orEmpty().contains("초과"))
    }
}
