package feature.tar

import com.microsoft.playwright.Locator
import dev.kord.rest.NamedFile
import feature.webdriver.PlaywrightBrowserFactory
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.createTempFile

data class TarItemMatchedResult(
    val itemName: String,
    val itemPageLink: String,
    val itemIdFromPageLink: Int?,
    val itemCategoryKorean: String?,
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

        PlaywrightBrowserFactory.create().use { session ->
            val page = session.page
            page.navigate(listUrl)

            val itemCandidates = page.locator(".results a")
            val itemCandidateCount = itemCandidates.count()
            val candidateLinks = (0 until itemCandidateCount).map { index ->
                val candidate = itemCandidates.nth(index)
                CandidateLink(
                    text = candidate.innerText().orEmpty(),
                    href = candidate.getAttribute("href").orEmpty()
                )
            }

            val itemPageLink = candidateLinks.firstOrNull {
                it.text.replace(" ", "") == itemName.replace(" ", "")
            }?.href?.takeIf { it.isNotBlank() } ?: run {
                return TarItemSearchResult.NotMatched(
                    candidateNames = candidateLinks.take(5).map { it.text }
                )
            }

            val itemIdFromSearchResult = extractItemIdFromItemPageLink(itemPageLink)
            page.navigate(itemPageLink)

            val screenshotFile = captureContentsScreenshot(page.locator("#contents"))
            val itemCategoryKorean = page.locator("#item-category")
                .firstOrNullText()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val englishName = page.locator("#item-name-lang > span:nth-child(1)")
                .firstOrNullText()
                .orEmpty()

            return TarItemSearchResult.Matched(
                TarItemMatchedResult(
                    itemName = itemName,
                    itemPageLink = itemPageLink,
                    itemIdFromPageLink = itemIdFromSearchResult,
                    itemCategoryKorean = itemCategoryKorean,
                    englishName = englishName,
                    screenshotFile = screenshotFile
                )
            )
        }
    }

    private fun captureContentsScreenshot(contentsLocator: Locator): File {
        val tempPath = createTempFile(prefix = "tar-item-", suffix = ".jpg")
        contentsLocator.screenshot(
            Locator.ScreenshotOptions().setPath(tempPath)
        )
        return tempPath.toFile()
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

    private data class CandidateLink(
        val text: String,
        val href: String
    )

    private fun Locator.firstOrNullText(): String? {
        if (count() <= 0) return null
        return nth(0).textContent()
    }
}
