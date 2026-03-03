package feature.eorzea

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import feature.webdriver.PlaywrightBrowserFactory
import org.slf4j.LoggerFactory

private const val EORZEA_BASE_URL = "https://ffxiv.eorzeacollection.com"
private const val EORZEA_GLAMOURS_URL =
    "https://ffxiv.eorzeacollection.com/glamours?filter%5BorderBy%5D=loves&filter%5BdatePeriod%5D=past-year&filter%5Bgender%5D=any&filter%5Bserver%5D=any&search=&author=&filter%5Bclass%5D=&filter%5Bstyle%5D=&filter%5Btheme%5D=&filter%5Bcolor%5D=&filter%5Bjob%5D=all&filter%5BminimumLvl%5D=1&filter%5BmaximumLvl%5D=100&filter%5BheadPiece%5D=&filter%5BbodyPiece%5D=&filter%5BhandsPiece%5D=&filter%5BlegsPiece%5D=&filter%5BfeetPiece%5D=&filter%5BweaponPiece%5D=&filter%5BoffhandPiece%5D=&filter%5BearringsPiece%5D=&filter%5BnecklacePiece%5D=&filter%5BbraceletsPiece%5D=&filter%5BringPiece%5D=&filter%5BfashionPiece%5D=&filter%5BfacePiece%5D=&page=1"
private const val EORZEA_NAVIGATION_TIMEOUT_MS = 60_000.0
private const val EORZEA_NAVIGATION_MAX_ATTEMPTS = 2
private const val EORZEA_NAVIGATION_RETRY_DELAY_MS = 700.0
private const val EORZEA_FILTER_INPUT_WAIT_TIMEOUT_MS = 15_000L

private val logger = LoggerFactory.getLogger(EorzeaCollectionGlamourClient::class.java)

data class EorzeaGlamourResult(
    val imageUrl: String,
    val pageUrl: String
)

enum class EorzeaArmorSlot(
    val tarCategory: String,
    val filterParam: String,
    val placeholderToken: String
) {
    HEAD("머리 방어구", "headPiece", "head"),
    BODY("몸통 방어구", "bodyPiece", "body"),
    HANDS("손 방어구", "handsPiece", "hands"),
    LEGS("다리 방어구", "legsPiece", "legs"),
    FEET("발 방어구", "feetPiece", "feet");

    companion object {
        fun fromTarCategory(tarCategory: String?): EorzeaArmorSlot? {
            if (tarCategory.isNullOrBlank()) return null
            return entries.firstOrNull { it.tarCategory == tarCategory.trim() }
        }
    }
}

class EorzeaCollectionGlamourClient {

    fun findTopGlamours(
        slot: EorzeaArmorSlot,
        itemEnglishName: String,
        limit: Int = 6
    ): List<EorzeaGlamourResult> {
        val startedAt = System.nanoTime()
        require(itemEnglishName.isNotBlank()) { "영문 아이템 이름이 비어 있습니다." }
        logger.info(
            "[EORZEA] 외형검색 시작: slot={}, item='{}', limit={}",
            slot.filterParam,
            itemEnglishName,
            limit
        )

        try {
            PlaywrightBrowserFactory.createForGlamour(width = 1600, height = 1800).use { session ->
                val page = session.page

                val navigateStartedAt = System.nanoTime()
                logger.info("[EORZEA] 목록 페이지 접속 시도")
                navigateWithRetry(page, EORZEA_GLAMOURS_URL, "목록")
                logger.info("[EORZEA] 목록 페이지 접속 완료: elapsedMs={}", elapsedMs(navigateStartedAt))
                checkCloudflareBlocked(page)

                val input = findFilterInput(page, slot)
                logger.info("[EORZEA] 필터 입력 필드 탐색 완료: slot={}", slot.filterParam)
                input.fill(itemEnglishName)
                logger.info("[EORZEA] 필터 입력 완료: '{}'", itemEnglishName)

                selectDropdownOption(page, input, itemEnglishName)
                val beforeApplyUrl = page.url()
                val beforeApplyGlamours = extractGlamours(page).take(limit)
                logger.info(
                    "[EORZEA] Apply 전 프리뷰 결과: count={}, url={}",
                    beforeApplyGlamours.size,
                    beforeApplyUrl
                )
                clickApplyFilter(page)
                logger.info("[EORZEA] Apply Filter 클릭 완료")

                val waitResultStartedAt = System.nanoTime()
                waitForFilteredResults(page, beforeApplyUrl, beforeApplyGlamours)
                logger.info("[EORZEA] 필터 결과 반영 확인 완료: elapsedMs={}", elapsedMs(waitResultStartedAt))

                val glamours = extractGlamours(page).take(limit)
                if (glamours.isEmpty()) {
                    throw IllegalStateException("외형 결과를 찾지 못했습니다.")
                }

                logger.info(
                    "[EORZEA] 외형검색 완료: resultCount={}, elapsedMs={}",
                    glamours.size,
                    elapsedMs(startedAt)
                )
                glamours.forEachIndexed { index, result ->
                    logger.info(
                        "[EORZEA] 결과 {}: pageUrl={}, imageUrl={}",
                        index + 1,
                        result.pageUrl,
                        result.imageUrl
                    )
                }

                return glamours
            }
        } catch (e: Exception) {
            logger.error(
                "[EORZEA] 외형검색 실패: slot={}, item='{}', elapsedMs={}",
                slot.filterParam,
                itemEnglishName,
                elapsedMs(startedAt),
                e
            )
            throw e
        }
    }

    private fun findFilterInput(page: Page, slot: EorzeaArmorSlot): Locator {
        val slotToken = slot.placeholderToken

        val selectors = listOf(
            "input[name='filter[${slot.filterParam}]']",
            "xpath=//input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'any') and " +
                "contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'$slotToken')]",
                "xpath=//input[contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'$slotToken')]"
        )

        val deadlineAt = System.currentTimeMillis() + EORZEA_FILTER_INPUT_WAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadlineAt) {
            checkCloudflareBlocked(page)
            val visibleInput = firstVisibleLocator(page, selectors)
            if (visibleInput != null) return visibleInput
            page.waitForTimeout(250.0)
        }

        throw IllegalStateException(
            "Eorzea 필터 입력 필드를 찾지 못했습니다. slot=${slot.filterParam}, title=${page.title()}, url=${page.url()}"
        )
    }

    private fun firstVisibleLocator(page: Page, selectors: List<String>): Locator? =
        selectors
            .asSequence()
            .map { page.locator(it).firstOrNullVisible() }
            .firstOrNull { it != null }

    private fun selectDropdownOption(page: Page, input: Locator, itemEnglishName: String) {
        val normalizedTarget = normalizeText(itemEnglishName)
        val deadlineAt = System.currentTimeMillis() + 12_000L
        logger.info("[EORZEA] 드롭다운 선택 시도 시작: target='{}'", itemEnglishName)

        while (System.currentTimeMillis() < deadlineAt) {
            val options = optionSelectors
                .asSequence()
                .flatMap { selector -> page.locator(selector).allVisible().asSequence() }
                .toList()

            val exact = options.firstOrNull { normalizeText(it.innerText()) == normalizedTarget }
            if (exact != null) {
                exact.click()
                logger.info("[EORZEA] 드롭다운 정확 일치 선택 완료")
                return
            }

            val partial = options.firstOrNull { normalizeText(it.innerText()).contains(normalizedTarget) }
            if (partial != null) {
                partial.click()
                logger.info("[EORZEA] 드롭다운 부분 일치 선택 완료")
                return
            }

            page.waitForTimeout(200.0)
        }

        logger.info("[EORZEA] 드롭다운 항목 미발견, Enter fallback 적용")
        input.press("Enter")
    }

    private fun clickApplyFilter(page: Page) {
        val applyButton = firstVisibleLocator(page, applyFilterSelectors)
            ?: throw IllegalStateException("Apply Filter 버튼을 찾지 못했습니다.")

        applyButton.click()
    }

    private fun waitForFilteredResults(
        page: Page,
        beforeApplyUrl: String,
        beforeApplyGlamours: List<EorzeaGlamourResult>
    ) {
        val deadlineAt = System.currentTimeMillis() + 20_000L
        while (System.currentTimeMillis() < deadlineAt) {
            page.waitForLoadState()
            val currentGlamours = extractGlamours(page)
            val urlChanged = page.url() != beforeApplyUrl
            val linksChanged =
                currentGlamours.take(beforeApplyGlamours.size).map { it.imageUrl } != beforeApplyGlamours.map { it.imageUrl }

            if (currentGlamours.isNotEmpty() && (urlChanged || linksChanged)) {
                return
            }
            page.waitForTimeout(250.0)
        }
        throw IllegalStateException("Apply Filter 이후 결과가 갱신되지 않았습니다.")
    }

    private fun extractGlamours(page: Page): List<EorzeaGlamourResult> {
        val cardBasedResults = glamourCardSelectors
            .asSequence()
            .flatMap { selector -> page.locator(selector).allVisible().asSequence() }
            .mapNotNull { card ->
                val pageUrl = card.absoluteHref()?.takeIf(::isGlamourPageUrl) ?: return@mapNotNull null
                val image = card.locator("img").firstOrNullVisible() ?: return@mapNotNull null
                val imageUrl =
                    (image.getAttribute("src") ?: image.getAttribute("data-src"))?.normalizeImageUrl()
                        ?.takeIf(::isGlamourImageUrl)
                        ?: return@mapNotNull null

                EorzeaGlamourResult(
                    imageUrl = imageUrl,
                    pageUrl = pageUrl
                )
            }
            .distinctBy { it.pageUrl }
            .toList()

        if (cardBasedResults.isNotEmpty()) {
            return cardBasedResults
        }

        return imageSelectors
            .asSequence()
            .flatMap { selector -> page.locator(selector).allVisible().asSequence() }
            .mapNotNull { image ->
                val imageUrl =
                    (image.getAttribute("src") ?: image.getAttribute("data-src"))?.normalizeImageUrl()
                        ?.takeIf(::isGlamourImageUrl)
                        ?: return@mapNotNull null
                val pageUrl = image.closestAnchorHref()?.takeIf(::isGlamourPageUrl) ?: return@mapNotNull null

                EorzeaGlamourResult(
                    imageUrl = imageUrl,
                    pageUrl = pageUrl
                )
            }
            .distinctBy { it.pageUrl }
            .toList()
    }

    private fun checkCloudflareBlocked(page: Page) {
        val title = runCatching { page.title() }.getOrDefault("")
        val url = runCatching { page.url() }.getOrDefault("")
        val content = runCatching { page.content() }.getOrDefault("")

        val titleBlocked = title.contains("Attention Required", ignoreCase = true) ||
            title.contains("Just a moment", ignoreCase = true) ||
            title.contains("Checking your browser", ignoreCase = true)
        val bodyBlocked = content.contains("cf-error-details", ignoreCase = true) ||
            content.contains("challenge-platform", ignoreCase = true) ||
            content.contains("cf_chl", ignoreCase = true)
        val urlBlocked = url.contains("/cdn-cgi/challenge-platform", ignoreCase = true)

        if (titleBlocked || bodyBlocked || urlBlocked) {
            logger.warn(
                "[EORZEA] Cloudflare 차단 감지: titleBlocked={}, bodyBlocked={}, urlBlocked={}, title={}, url={}",
                titleBlocked,
                bodyBlocked,
                urlBlocked,
                title,
                url
            )
            throw IllegalStateException("Eorzea Collection 접근이 차단되었습니다. Cloudflare 차단 상태를 확인해주세요.")
        }
    }

    private fun navigateWithRetry(page: Page, url: String, stepName: String) {
        var lastError: Throwable? = null

        repeat(EORZEA_NAVIGATION_MAX_ATTEMPTS) { attempt ->
            val attemptNo = attempt + 1
            val succeeded = runCatching {
                page.navigate(
                    url,
                    Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(EORZEA_NAVIGATION_TIMEOUT_MS)
                )
            }.onFailure { error ->
                lastError = error
                logger.warn(
                    "[EORZEA] {} 페이지 접속 실패: attempt={}/{}, url={}, message={}",
                    stepName,
                    attemptNo,
                    EORZEA_NAVIGATION_MAX_ATTEMPTS,
                    url,
                    error.message
                )
            }.isSuccess

            if (succeeded) return

            if (attemptNo < EORZEA_NAVIGATION_MAX_ATTEMPTS) {
                page.waitForTimeout(EORZEA_NAVIGATION_RETRY_DELAY_MS)
            }
        }

        throw IllegalStateException(
            "Eorzea $stepName 페이지 접속에 실패했습니다. url=$url",
            lastError
        )
    }

    private fun normalizeText(input: String): String =
        input.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private val optionSelectors = listOf(
            ".multiselect__content-wrapper .multiselect__option",
            ".multiselect__content-wrapper li",
            "[role='listbox'] [role='option']",
            ".ui-menu-item",
            ".autocomplete-suggestion"
        )

        private val applyFilterSelectors = listOf(
            "button:has-text('Apply Filter')",
            "xpath=//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'apply filter')]",
            "xpath=//input[@type='submit' and contains(@value,'Apply')]"
        )

        private val imageSelectors = listOf(
            "img[src*='https://glamours.eorzeacollection.com/']",
            "img[data-src*='https://glamours.eorzeacollection.com/']"
        )

        private val glamourCardSelectors = listOf(
            "a[href*='/glamour/']",
            "a[href*='://ffxiv.eorzeacollection.com/glamour/']"
        )
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}

private fun isGlamourPageUrl(url: String): Boolean {
    val normalized = url.trim()
    return normalized.startsWith("$EORZEA_BASE_URL/glamour/")
}

private fun isGlamourImageUrl(url: String): Boolean =
    url.contains("https://glamours.eorzeacollection.com/")

private fun String.normalizeImageUrl(): String {
    val value = trim()
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "$EORZEA_BASE_URL$value"
        else -> value
    }
}

private fun Locator.absoluteHref(): String? = runCatching {
    evaluate("el => el.href") as? String
}.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

private fun Locator.closestAnchorHref(): String? = runCatching {
    evaluate("el => el.closest('a')?.href") as? String
}.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

private fun Locator.firstOrNullVisible(): Locator? {
    val count = count()
    for (i in 0 until count) {
        val candidate = nth(i)
        if (runCatching { candidate.isVisible() }.getOrDefault(false)) {
            return candidate
        }
    }
    return null
}

private fun Locator.allVisible(limit: Int = 200): List<Locator> {
    val count = count().coerceAtMost(limit)
    val visible = mutableListOf<Locator>()
    for (i in 0 until count) {
        val candidate = nth(i)
        if (runCatching { candidate.isVisible() }.getOrDefault(false)) {
            visible += candidate
        }
    }
    return visible
}
