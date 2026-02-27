package feature.tar

import Prop
import dev.kord.rest.NamedFile
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

data class TarItemMatchedResult(
    val itemName: String,
    val itemPageLink: String,
    val itemIdFromPageLink: Int?,
    val englishName: String,
    val screenshotFile: File
) {
    fun toNamedFile(fileName: String = "$itemName.jpg"): NamedFile =
        NamedFile(fileName, ChannelProvider { screenshotFile.inputStream().toByteReadChannel() })
}

sealed interface TarItemSearchResult {
    data class Matched(val result: TarItemMatchedResult) : TarItemSearchResult
    data class NotMatched(val candidateNames: List<String>) : TarItemSearchResult
}

fun TarItemSearchResult.NotMatched.toDiscordMessage(): String =
    if (candidateNames.isEmpty()) {
        "찾으시는 아이템이 없어요..."
    } else {
        "다음의 아이템을 찾고 계신가요? $candidateNames"
    }

class TarItemSearchClient {

    fun searchAndCapture(itemName: String): TarItemSearchResult {
        val encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8)
        val listUrl = "https://ff14.tar.to/item/list?keyword=$encodedItemName"

        val driver = createDriver()
        try {
            driver.get(listUrl)

            val itemCandidates = driver
                .findElements(By.className("results"))
                .firstOrNull()
                ?.findElements(By.tagName("a"))
                .orEmpty()

            val itemPageLink = itemCandidates.firstOrNull {
                it.text.replace(" ", "") == itemName.replace(" ", "")
            }?.getAttribute("href") ?: run {
                return TarItemSearchResult.NotMatched(
                    candidateNames = itemCandidates.take(5).map { it.text }
                )
            }

            val itemIdFromSearchResult = extractItemIdFromItemPageLink(itemPageLink)

            driver.get(itemPageLink)

            val screenshotFile = driver.findElement(By.id("contents")).getScreenshotAs(OutputType.FILE)
            val englishName = driver
                .findElements(By.cssSelector("#item-name-lang > span:nth-child(1)"))
                .firstOrNull()
                ?.text
                .orEmpty()

            return TarItemSearchResult.Matched(
                TarItemMatchedResult(
                    itemName = itemName,
                    itemPageLink = itemPageLink,
                    itemIdFromPageLink = itemIdFromSearchResult,
                    englishName = englishName,
                    screenshotFile = screenshotFile
                )
            )
        } finally {
            driver.quit()
        }
    }

    private fun createDriver(): WebDriver =
        RemoteWebDriver(
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
