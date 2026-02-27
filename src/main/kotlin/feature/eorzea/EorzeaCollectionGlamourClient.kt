package feature.eorzea

import feature.webdriver.SeleniumDriverFactory
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

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

        val driver = SeleniumDriverFactory.create(width = 1600, height = 1800)
        try {
            driver.get(EORZEA_GLAMOURS_URL)
            waitForDocumentReady(driver)
            checkCloudflareBlocked(driver)

            val input = findFilterInput(driver, slot)
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"))
            input.sendKeys(Keys.DELETE)
            input.sendKeys(itemEnglishName)

            selectDropdownOption(driver, input, itemEnglishName)
            val beforeApplyUrl = driver.currentUrl
            val beforeApplyLinks = extractImageLinks(driver).take(limit)
            clickApplyFilter(driver)

            waitForDocumentReady(driver)
            waitForFilteredResults(driver, beforeApplyUrl, beforeApplyLinks)

            val links = extractImageLinks(driver).take(limit)
            if (links.isEmpty()) {
                throw IllegalStateException("외형 결과 이미지 링크를 찾지 못했습니다.")
            }

            return links
        } finally {
            driver.quit()
        }
    }

    private fun findFilterInput(driver: WebDriver, slot: EorzeaArmorSlot): WebElement {
        val slotToken = slot.placeholderToken

        val locators = listOf(
            By.cssSelector("input[name='filter[${slot.filterParam}]']"),
            By.xpath(
                "//input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'any') and " +
                    "contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'$slotToken')]"
            ),
            By.xpath(
                "//input[contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'$slotToken')]"
            )
        )

        return locators
            .asSequence()
            .flatMap { locator -> driver.findElements(locator).asSequence() }
            .firstOrNull { isInteractable(it) }
            ?: throw IllegalStateException("Eorzea 필터 입력 필드를 찾지 못했습니다. slot=${slot.filterParam}")
    }

    private fun selectDropdownOption(driver: WebDriver, input: WebElement, itemEnglishName: String) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(12))
        val normalizedTarget = normalizeText(itemEnglishName)

        val option = runCatching {
            wait.until {
                val options = optionLocators.asSequence()
                    .flatMap { locator -> driver.findElements(locator).asSequence() }
                    .filter { isInteractable(it) }
                    .toList()

                val exact = options.firstOrNull { normalizeText(it.text) == normalizedTarget }
                exact ?: options.firstOrNull { normalizeText(it.text).contains(normalizedTarget) }
            }
        }.getOrNull()

        if (option != null) {
            clickElement(driver, option)
            return
        }

        // Fallback: if dropdown selection is not found, submit current input.
        input.sendKeys(Keys.ENTER)
    }

    private fun clickApplyFilter(driver: WebDriver) {
        val applyButton = applyFilterLocators
            .asSequence()
            .flatMap { locator -> driver.findElements(locator).asSequence() }
            .firstOrNull { isInteractable(it) }
            ?: throw IllegalStateException("Apply Filter 버튼을 찾지 못했습니다.")

        clickElement(driver, applyButton)
    }

    private fun waitForFilteredResults(
        driver: WebDriver,
        beforeApplyUrl: String,
        beforeApplyLinks: List<String>
    ) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(20))
        wait.until {
            val currentLinks = extractImageLinks(driver)
            if (currentLinks.isEmpty()) {
                return@until false
            }

            val urlChanged = driver.currentUrl != beforeApplyUrl
            val linksChanged = currentLinks.take(beforeApplyLinks.size) != beforeApplyLinks

            urlChanged || linksChanged
        }
    }

    private fun extractImageLinks(driver: WebDriver): List<String> {
        val directSrc = imageLocators
            .asSequence()
            .flatMap { locator -> driver.findElements(locator).asSequence() }
            .mapNotNull { element ->
                element.getAttribute("src")
                    ?.takeIf { it.contains("https://glamours.eorzeacollection.com/") }
                    ?: element.getAttribute("data-src")
                        ?.takeIf { it.contains("https://glamours.eorzeacollection.com/") }
            }
            .toList()

        val pageSourceLinks = IMAGE_LINK_REGEX
            .findAll(driver.pageSource)
            .map { it.value }
            .toList()

        return (directSrc + pageSourceLinks).distinct()
    }

    private fun waitForDocumentReady(driver: WebDriver) {
        val wait = WebDriverWait(driver, Duration.ofSeconds(20))
        wait.until {
            (driver as JavascriptExecutor).executeScript("return document.readyState") == "complete"
        }
    }

    private fun checkCloudflareBlocked(driver: WebDriver) {
        val titleBlocked = driver.title.contains("Attention Required", ignoreCase = true)
        val bodyBlocked = driver.pageSource.contains("cf-error-details", ignoreCase = true)
        if (titleBlocked || bodyBlocked) {
            throw IllegalStateException("Eorzea Collection 접근이 차단되었습니다. Cloudflare 차단 상태를 확인해주세요.")
        }
    }

    private fun clickElement(driver: WebDriver, element: WebElement) {
        runCatching {
            (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView({block:'center'});", element)
            element.click()
        }.getOrElse {
            (driver as JavascriptExecutor).executeScript("arguments[0].click();", element)
        }
    }

    private fun isInteractable(element: WebElement): Boolean = runCatching {
        element.isDisplayed && element.isEnabled
    }.getOrDefault(false)

    private fun normalizeText(input: String): String =
        input.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private val optionLocators = listOf(
            By.cssSelector(".multiselect__content-wrapper .multiselect__option"),
            By.cssSelector(".multiselect__content-wrapper li"),
            By.cssSelector("[role='listbox'] [role='option']"),
            By.cssSelector(".ui-menu-item"),
            By.cssSelector(".autocomplete-suggestion")
        )

        private val applyFilterLocators = listOf(
            By.xpath(
                "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'apply filter')]"
            ),
            By.xpath("//input[@type='submit' and contains(@value,'Apply')]")
        )

        private val imageLocators = listOf(
            By.cssSelector("img[src*='https://glamours.eorzeacollection.com/']"),
            By.cssSelector("img[data-src*='https://glamours.eorzeacollection.com/']")
        )

        private val IMAGE_LINK_REGEX =
            """https://glamours\.eorzeacollection\.com/[A-Za-z0-9/\-_.]+""".toRegex()
    }
}
