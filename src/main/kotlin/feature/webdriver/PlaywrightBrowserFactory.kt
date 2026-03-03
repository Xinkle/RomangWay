package feature.webdriver

import Prop
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright

class PlaywrightSession(
    private val playwright: Playwright,
    val browser: Browser,
    val context: BrowserContext,
    val page: Page
) : AutoCloseable {
    override fun close() {
        runCatching { context.close() }
        runCatching { browser.close() }
        runCatching { playwright.close() }
    }
}

object PlaywrightBrowserFactory {
    fun create(width: Int = 835, height: Int = 1080): PlaywrightSession {
        val endpoint = normalizeEndpoint(Prop.getChromeCdp())
        val playwright = Playwright.create(
            Playwright.CreateOptions().setEnv(
                HashMap(System.getenv()).apply {
                    put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
                }
            )
        )
        val browser = connectOverCdp(playwright, endpoint)
        val context = browser.newContext(
            Browser.NewContextOptions().setViewportSize(width, height)
        )
        val page = context.newPage()

        return PlaywrightSession(
            playwright = playwright,
            browser = browser,
            context = context,
            page = page
        )
    }

    private fun connectOverCdp(playwright: Playwright, endpoint: String): Browser = runCatching {
        playwright.chromium().connectOverCDP(endpoint)
    }.getOrElse { cause ->
        throw IllegalStateException(
            "Playwright CDP 연결에 실패했습니다. CHROME_CDP 값을 확인해주세요: $endpoint",
            cause
        )
    }

    private fun normalizeEndpoint(rawEndpoint: String): String {
        val trimmed = rawEndpoint.trim().removeSuffix("/")
        require(trimmed.isNotBlank()) { "CHROME_CDP 값이 비어 있습니다." }

        return when {
            trimmed.endsWith("/webdriver") -> trimmed.removeSuffix("/webdriver")
            trimmed.endsWith("/wd/hub") -> trimmed.removeSuffix("/wd/hub")
            else -> trimmed
        }
    }
}
