package feature

import database.ItemTableDao
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import feature.tar.TarItemSearchClient
import feature.tar.TarItemSearchResult
import feature.tar.toDiscordMessage
import feature.universalis.JsonItemFinder
import feature.universalis.UniversalisClient
import feature.universalis.UniversalisWorlds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템_이름"

class ItemSearchFeature : CoroutineScope, ChatInputCommandInteractionListener {
    private val tarItemSearchClient = TarItemSearchClient()

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "아이템검색"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_ITEM_NAME,
            "아이템 이름",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()

        val itemName = command.strings[ARGUMENT_ITEM_NAME]!!
        val tarResult = tarItemSearchClient.searchAndCapture(itemName)
        if (tarResult is TarItemSearchResult.NotMatched) {
            response.respond { content = tarResult.toDiscordMessage() }
            return
        }
        tarResult as TarItemSearchResult.Matched

        val file = tarResult.result.toNamedFile()

        // Search Item Price
        val itemId: Int? = ItemTableDao.getItemIdByName(itemName) ?: run {
            (tarResult.result.itemIdFromPageLink
                ?: tarResult.result.englishName.takeIf { it.isNotBlank() }?.let(JsonItemFinder::findItemIdByEnField))
                ?.also { resolvedItemId ->
                transaction {
                    ItemTableDao.insertOrReplaceItem(itemName, resolvedItemId)
                }
            }
        }

        val itemPrices = itemId?.let {
            UniversalisWorlds.entries.map { server ->
                UniversalisClient().fetchDetailItemPrice(server.worldId, itemId)
            }
        }?.sortedBy {
            it?.currentAveragePrice
        }?.map {
            it?.getSummary()
        }?.joinToString("\n")
            ?: "ItemID 확인 불가"

        response.respond {
            files.add(file)
            content = itemPrices
        }
    }
}
