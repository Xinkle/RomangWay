package feature.eorzea

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import feature.webdriver.PlaywrightBrowserFactory

private const val EORZEA_GLAMOURS_URL =
    "https://ffxiv.eorzeacollection.com/glamours?filter%5BorderBy%5D=loves&filter%5BdatePeriod%5D=past-year&filter%5Bgender%5D=any&filter%5Bserver%5D=any&search=&author=&filter%5Bclass%5D=&filter%5Bstyle%5D=&filter%5Btheme%5D=&filter%5Bcolor%5D=&filter%5Bjob%5D=all&filter%5BminimumLvl%5D=1&filter%5BmaximumLvl%5D=100&filter%5BheadPiece%5D=&filter%5BbodyPiece%5D=&filter%5BhandsPiece%5D=&filter%5BlegsPiece%5D=&filter%5BfeetPiece%5D=&filter%5BweaponPiece%5D=&filter%5BoffhandPiece%5D=&filter%5BearringsPiece%5D=&filter%5BnecklacePiece%5D=&filter%5BbraceletsPiece%5D=&filter%5BringPiece%5D=&filter%5BfashionPiece%5D=&filter%5BfacePiece%5D=&page=1"

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

    fun findTopGlamourImageLinks(
        slot: EorzeaArmorSlot,
        itemEnglishName: String,
        limit: Int = 8
    ): List<String> {
        require(itemEnglishName.isNotBlank()) { "영문 아이템 이름이 비어 있습니다." }

        PlaywrightBrowserFactory.create(width = 1600, height = 1800).use { session ->
            val page = session.page

            page.navigate(EORZEA_GLAMOURS_URL)
            page.waitForLoadState()
            checkCloudflareBlocked(page)

            val input = findFilterInput(page, slot)
            input.fill(itemEnglishName)

            selectDropdownOption(page, input, itemEnglishName)
            val beforeApplyUrl = page.url()
            val beforeApplyLinks = extractImageLinks(page).take(limit)
            clickApplyFilter(page)

            waitForFilteredResults(page, beforeApplyUrl, beforeApplyLinks)

            val links = extractImageLinks(page).take(limit)
            if (links.isEmpty()) {
                throw IllegalStateException("외형 결과 이미지 링크를 찾지 못했습니다.")
            }

            return links
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

        return firstVisibleLocator(page, selectors)
            ?: throw IllegalStateException("Eorzea 필터 입력 필드를 찾지 못했습니다. slot=${slot.filterParam}")
    }

    private fun firstVisibleLocator(page: Page, selectors: List<String>): Locator? =
        selectors
            .asSequence()
            .map { page.locator(it).firstOrNullVisible() }
            .firstOrNull { it != null }

    private fun selectDropdownOption(page: Page, input: Locator, itemEnglishName: String) {
        val normalizedTarget = normalizeText(itemEnglishName)
        val deadlineAt = System.currentTimeMillis() + 12_000L

        while (System.currentTimeMillis() < deadlineAt) {
            val options = optionSelectors
                .asSequence()
                .flatMap { selector -> page.locator(selector).allVisible().asSequence() }
                .toList()

            val exact = options.firstOrNull { normalizeText(it.innerText()) == normalizedTarget }
            if (exact != null) {
                exact.click()
                return
            }

            val partial = options.firstOrNull { normalizeText(it.innerText()).contains(normalizedTarget) }
            if (partial != null) {
                partial.click()
                return
            }

            page.waitForTimeout(200.0)
        }

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
        beforeApplyLinks: List<String>
    ) {
        val deadlineAt = System.currentTimeMillis() + 20_000L
        while (System.currentTimeMillis() < deadlineAt) {
            page.waitForLoadState()
            val currentLinks = extractImageLinks(page)
            val urlChanged = page.url() != beforeApplyUrl
            val linksChanged = currentLinks.take(beforeApplyLinks.size) != beforeApplyLinks

            if (currentLinks.isNotEmpty() && (urlChanged || linksChanged)) {
                return
            }
            page.waitForTimeout(250.0)
        }
        throw IllegalStateException("Apply Filter 이후 결과가 갱신되지 않았습니다.")
    }

    private fun extractImageLinks(page: Page): List<String> {
        val directSrc = imageSelectors
            .asSequence()
            .flatMap { selector -> page.locator(selector).allVisible().asSequence() }
            .mapNotNull { element -> element.getAttribute("src") ?: element.getAttribute("data-src") }
            .filter { it.contains("https://glamours.eorzeacollection.com/") }
            .toList()

        val pageSourceLinks = IMAGE_LINK_REGEX
            .findAll(page.content())
            .map { it.value }
            .toList()

        return (directSrc + pageSourceLinks).distinct()
    }

    private fun checkCloudflareBlocked(page: Page) {
        val titleBlocked = page.title().contains("Attention Required", ignoreCase = true)
        val bodyBlocked = page.content().contains("cf-error-details", ignoreCase = true)
        if (titleBlocked || bodyBlocked) {
            throw IllegalStateException("Eorzea Collection 접근이 차단되었습니다. Cloudflare 차단 상태를 확인해주세요.")
        }
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

        private val IMAGE_LINK_REGEX =
            """https://glamours\.eorzeacollection\.com/[A-Za-z0-9/\-_.]+""".toRegex()
    }
}

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
