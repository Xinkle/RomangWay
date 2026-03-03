package feature

import database.ItemTableDao
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import feature.tar.TarItemSearchClient
import feature.tar.TarItemSearchResult
import feature.tar.toDiscordMessage
import feature.universalis.JsonItemFinder
import feature.universalis.UniversalisClient
import feature.universalis.UniversalisWorlds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템_이름"
private const val TAR_ITEM_SEARCH_TIMEOUT_MS = 45_000L

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
        val initialized = runCatching {
            response.respond {
                content = "진행 중: 아이템 조회를 준비하고 있습니다."
            }
        }.isSuccess
        if (!initialized) {
            updateDeferredProgress(interaction, "아이템 조회를 준비하고 있습니다.")
        }

        val itemName = command.strings[ARGUMENT_ITEM_NAME]!!
        updateDeferredProgress(interaction, "TAR에서 아이템 정보를 조회 중입니다.")
        val tarResult = runWithCommandTimeout("TAR 아이템 검색", TAR_ITEM_SEARCH_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                tarItemSearchClient.searchAndCapture(itemName)
            }
        }
        if (tarResult is TarItemSearchResult.NotMatched) {
            if (!updateDeferredMessage(interaction, tarResult.toDiscordMessage())) {
                response.respond { content = tarResult.toDiscordMessage() }
            }
            return
        }
        tarResult as TarItemSearchResult.Matched

        val file = tarResult.result.toNamedFile()

        // Search Item Price
        updateDeferredProgress(interaction, "시세 정보를 조회 중입니다.")
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

        updateDeferredProgress(interaction, "결과를 디스코드로 전송 중입니다.")
        val edited = runCatching {
            interaction.getOriginalInteractionResponseOrNull()?.edit {
                files.add(file)
                content = itemPrices
            } != null
        }.getOrDefault(false)

        if (!edited) {
            response.respond {
                files.add(file)
                content = itemPrices
            }
        }
    }
}
