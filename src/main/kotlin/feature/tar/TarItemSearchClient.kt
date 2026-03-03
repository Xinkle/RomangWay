package feature.tar

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import dev.kord.rest.NamedFile
import feature.webdriver.PlaywrightBrowserFactory
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.createTempFile

private val logger = LoggerFactory.getLogger(TarItemSearchClient::class.java)
private const val TAR_NAVIGATION_TIMEOUT_MS = 60_000.0
private const val TAR_NAVIGATION_MAX_ATTEMPTS = 2
private const val TAR_NAVIGATION_RETRY_DELAY_MS = 500.0

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

        try {
            PlaywrightBrowserFactory.createForTar().use { session ->
                val page = session.page
                val listNavigateStartedAt = System.nanoTime()
                logger.info("[TAR] 목록 페이지 접속 시도")
                navigateWithRetry(page, listUrl, "목록")
                logger.info(
                    "[TAR] 목록 페이지 접속 완료: elapsedMs={}",
                    elapsedMs(listNavigateStartedAt)
                )

                val itemCandidates = page.locator(".results a")
                val itemCandidateCount = itemCandidates.count()
                logger.info("[TAR] 검색 후보 추출: count={}", itemCandidateCount)

                val candidateLinks = (0 until itemCandidateCount).map { index ->
                    val candidate = itemCandidates.nth(index)
                    CandidateLink(
                        text = candidate.innerText().orEmpty(),
                        href = resolveItemPageLink(candidate.getAttribute("href").orEmpty())
                    )
                }

                val itemPageLink = candidateLinks.firstOrNull {
                    it.text.replace(" ", "") == itemName.replace(" ", "")
                }?.href?.takeIf { it.isNotBlank() } ?: run {
                    logger.info(
                        "[TAR] 일치 항목 없음: itemName='{}', suggestions={}, elapsedMs={}",
                        itemName,
                        candidateLinks.take(5).map { it.text },
                        elapsedMs(startedAt)
                    )
                    return TarItemSearchResult.NotMatched(
                        candidateNames = candidateLinks.take(5).map { it.text }
                    )
                }

                val itemIdFromSearchResult = extractItemIdFromItemPageLink(itemPageLink)
                logger.info("[TAR] 항목 선택 완료: pageLink={}, itemId={}", itemPageLink, itemIdFromSearchResult)

                val itemNavigateStartedAt = System.nanoTime()
                navigateWithRetry(page, itemPageLink, "상세")
                logger.info(
                    "[TAR] 아이템 상세 페이지 접속 완료: elapsedMs={}",
                    elapsedMs(itemNavigateStartedAt)
                )

                val screenshotStartedAt = System.nanoTime()
                val screenshotFile = captureContentsScreenshot(page.locator("#contents"))
                logger.info(
                    "[TAR] 스크린샷 캡처 완료: path={}, elapsedMs={}",
                    screenshotFile.absolutePath,
                    elapsedMs(screenshotStartedAt)
                )

                val itemCategoryKorean = page.locator("#item-category")
                    .firstOrNullText()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val englishName = page.locator("#item-name-lang > span:nth-child(1)")
                    .firstOrNullText()
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
            }
        } catch (e: Exception) {
            logger.error(
                "[TAR] 검색 실패: itemName='{}', elapsedMs={}",
                itemName,
                elapsedMs(startedAt),
                e
            )
            throw e
        }
    }

    private fun navigateWithRetry(page: Page, url: String, stepName: String) {
        var lastError: Throwable? = null

        repeat(TAR_NAVIGATION_MAX_ATTEMPTS) { attempt ->
            val attemptNo = attempt + 1
            val succeeded = runCatching {
                page.navigate(
                    url,
                    Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(TAR_NAVIGATION_TIMEOUT_MS)
                )
            }.onFailure { error ->
                lastError = error
                logger.warn(
                    "[TAR] {} 페이지 접속 실패: attempt={}/{}, url={}, message={}",
                    stepName,
                    attemptNo,
                    TAR_NAVIGATION_MAX_ATTEMPTS,
                    url,
                    error.message
                )
            }.isSuccess

            if (succeeded) return

            if (attemptNo < TAR_NAVIGATION_MAX_ATTEMPTS) {
                page.waitForTimeout(TAR_NAVIGATION_RETRY_DELAY_MS)
            }
        }

        throw IllegalStateException(
            "TAR $stepName 페이지 접속에 실패했습니다. url=$url",
            lastError
        )
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
        val href: String?
    )

    private fun Locator.firstOrNullText(): String? {
        if (count() <= 0) return null
        return nth(0).textContent()
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
