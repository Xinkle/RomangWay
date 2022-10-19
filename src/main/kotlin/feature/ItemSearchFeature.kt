package feature

import Prop
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템_이름"

class ItemSearchFeature(private val kord: Kord) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "아이템검색"

    init {
        System.setProperty("webdriver.chrome.driver", Prop.getChromeDriver())
    }

    private val driver: WebDriver = ChromeDriver(
        ChromeOptions().addArguments(
            "--headless",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--force-device-scale-factor=1.5"
        )
    ).apply {
        manage().window().size = Dimension(835, 1080)
    }

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()

        val itemName = command.strings[ARGUMENT_ITEM_NAME]!!
        val url = "https://ff14.tar.to/item/list?keyword=$itemName"

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

        driver.get(itemPageLink)

        val srcFile = driver.findElement(
            By.id("contents")
        ).getScreenshotAs(
            OutputType.FILE
        )

        val file = NamedFile("$itemName.jpg", srcFile.inputStream())

        response.respond {
            files = mutableListOf(file)
        }
    }

    init {
        println("$command module registered!")
        launch {
            kord.createGlobalChatInputCommand(
                command, "타로트맛 타로트에서 아이템을 검색합니다"
            ) {
                string(ARGUMENT_ITEM_NAME, "아이템 이름") {
                    required = true
                }
            }
        }
    }
}
