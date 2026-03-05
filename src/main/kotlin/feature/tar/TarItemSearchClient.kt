package feature.tar

import dev.kord.rest.NamedFile
import feature.webdriver.SeleniumDriverFactory
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = LoggerFactory.getLogger(TarItemSearchClient::class.java)

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
        val startedAt = System.nanoTime()
        val encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8)
        val listUrl = "https://ff14.tar.to/item/list?keyword=$encodedItemName"
        logger.info("[TAR] 검색 시작: itemName='{}', listUrl={}", itemName, listUrl)

        val driver = SeleniumDriverFactory.create()
        try {
            val listNavigateStartedAt = System.nanoTime()
            driver.get(listUrl)
            logger.info("[TAR] 목록 페이지 접속 완료: elapsedMs={}", elapsedMs(listNavigateStartedAt))

            val itemCandidates = driver
                .findElements(By.className("results"))
                .firstOrNull()
                ?.findElements(By.tagName("a"))
                .orEmpty()

            logger.info("[TAR] 검색 후보 추출: count={}", itemCandidates.size)

            val itemPageLink = itemCandidates.firstOrNull {
                it.text.replace(" ", "") == itemName.replace(" ", "")
            }?.getAttribute("href")
                ?.let { resolveItemPageLink(it) }
                ?.takeIf { it.isNotBlank() } ?: run {
                logger.info(
                    "[TAR] 일치 항목 없음: itemName='{}', suggestions={}, elapsedMs={}",
                    itemName,
                    itemCandidates.take(5).map { it.text },
                    elapsedMs(startedAt)
                )
                return TarItemSearchResult.NotMatched(
                    candidateNames = itemCandidates.take(5).map { it.text }
                )
            }

            val itemIdFromSearchResult = extractItemIdFromItemPageLink(itemPageLink)
            logger.info("[TAR] 항목 선택 완료: pageLink={}, itemId={}", itemPageLink, itemIdFromSearchResult)

            val itemNavigateStartedAt = System.nanoTime()
            driver.get(itemPageLink)
            logger.info("[TAR] 아이템 상세 페이지 접속 완료: elapsedMs={}", elapsedMs(itemNavigateStartedAt))

            val screenshotStartedAt = System.nanoTime()
            val screenshotFile = driver.findElement(By.id("contents")).getScreenshotAs(OutputType.FILE)
            logger.info(
                "[TAR] 스크린샷 캡처 완료: path={}, elapsedMs={}",
                screenshotFile.absolutePath,
                elapsedMs(screenshotStartedAt)
            )

            val itemCategoryKorean = driver
                .findElements(By.id("item-category"))
                .firstOrNull()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val englishName = driver
                .findElements(By.cssSelector("#item-name-lang > span:nth-child(1)"))
                .firstOrNull()
                ?.text
                .orEmpty()

            logger.info(
                "[TAR] 검색 완료: itemName='{}', english='{}', category='{}', elapsedMs={}",
                itemName,
                englishName,
                itemCategoryKorean ?: "N/A",
                elapsedMs(startedAt)
            )

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
        } catch (e: Exception) {
            logger.error(
                "[TAR] 검색 실패: itemName='{}', elapsedMs={}",
                itemName,
                elapsedMs(startedAt),
                e
            )
            throw e
        } finally {
            driver.quit()
            logger.info("[TAR] Selenium 드라이버 종료")
        }
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

    companion object {
        private const val TAR_BASE_URL = "https://ff14.tar.to"

        internal fun resolveItemPageLink(rawHref: String): String? {
            val trimmed = rawHref.trim()
            if (trimmed.isEmpty()) return null

            return runCatching {
                val parsed = URI(trimmed)
                if (parsed.isAbsolute) {
                    parsed.toString()
                } else {
                    URI(TAR_BASE_URL).resolve(parsed).toString()
                }
            }.getOrNull()
        }
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}
