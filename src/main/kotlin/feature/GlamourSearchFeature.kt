package feature

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import feature.tar.TarItemSearchClient
import feature.tar.TarItemSearchResult
import feature.tar.toDiscordMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템이름"

class GlamourSearchFeature : CoroutineScope, ChatInputCommandInteractionListener {
    private val tarItemSearchClient = TarItemSearchClient()

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "외형검색"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_ITEM_NAME,
            "검색할 아이템 이름",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()
        val itemName = command.strings[ARGUMENT_ITEM_NAME]!!

        when (val tarResult = tarItemSearchClient.searchAndCapture(itemName)) {
            is TarItemSearchResult.NotMatched -> response.respond {
                content = tarResult.toDiscordMessage()
            }

            is TarItemSearchResult.Matched -> response.respond {
                files.add(tarResult.result.toNamedFile())
                content = tarResult.result.englishName
                    .takeIf { it.isNotBlank() }
                    ?: "영문 아이템 이름 확인 불가"
            }
        }
    }
}
