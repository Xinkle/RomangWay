package feature

import Prop
import database.ItemTableDao
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.NamedFile
import feature.universalis.JsonItemFinder
import feature.universalis.UniversalisClient
import feature.universalis.UniversalisWorlds
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.transaction
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.net.URL
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템_이름"

class ItemSearchFeature : CoroutineScope, ChatInputCommandInteractionListener {
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
        val url = "https://ff14.tar.to/item/list?keyword=$itemName"

        val driver: WebDriver = RemoteWebDriver(
            URL(Prop.getChromeDriver()),
            ChromeOptions().addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--force-device-scale-factor=1.5"
            )
        ).apply {
            manage().window().size = Dimension(835, 1080)
        }

        driver.get(url)

        val itemCandidates = driver.findElement(
            By.className("results")
        ).findElements(
            By.tagName("a")
        )

        val itemPageLink = itemCandidates.firstOrNull {
            it.text.replace(" ", "") == itemName.replace(" ", "")
        }?.getAttribute("href") ?: run {
            val itemCandidatesNames = itemCandidates.take(5).map { it.text }

            if (itemCandidatesNames.isEmpty()) {
                response.respond {
                    content = "찾으시는 아이템이 없어요..."
                }
            } else {
                response.respond {
                    content = "다음의 아이템을 찾고 계신가요? $itemCandidatesNames"
                }
            }

            return
        }

        val itemIdFromSearchResult = extractItemIdFromItemPageLink(itemPageLink)

        driver.get(itemPageLink)

        val srcFile = driver.findElement(
            By.id("contents")
        ).getScreenshotAs(
            OutputType.FILE
        )

        val engName: String = driver.findElement(
            By.cssSelector("#item-name-lang > span:nth-child(1)")
        ).text

        val file = NamedFile("$itemName.jpg", ChannelProvider { srcFile.inputStream().toByteReadChannel() })

        // Search Item Price
        val itemId: Int? = ItemTableDao.getItemIdByName(itemName) ?: run {
            (itemIdFromSearchResult ?: JsonItemFinder.findItemIdByEnField(engName))?.also { resolvedItemId ->
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

        driver.quit()
    }

    private fun extractItemIdFromItemPageLink(itemPageLink: String): Int? {
        val patterns = listOf(
            """/db/item/(\d+)""".toRegex(),
            """/item/(\d+)""".toRegex(),
            """[?&]id=(\d+)""".toRegex(),
            """/(\d+)(?:/)?$""".toRegex()
        )

        return patterns.firstNotNullOfOrNull { regex ->
            regex.find(itemPageLink)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
    }
}
