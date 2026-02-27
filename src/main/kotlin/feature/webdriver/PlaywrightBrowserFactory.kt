package feature.webdriver

import Prop
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
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
        val endpoint = normalizeEndpoint(Prop.getChromeDriver())
        val playwright = Playwright.create(
            Playwright.CreateOptions().setEnv(
                HashMap(System.getenv()).apply {
                    put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
                }
            )
        )
        val browser = connect(playwright.chromium(), endpoint)
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

    private fun connect(browserType: BrowserType, endpoint: String): Browser = runCatching {
        when {
            endpoint.startsWith("ws://") || endpoint.startsWith("wss://") -> {
                browserType.connect(endpoint)
            }

            else -> {
                browserType.connectOverCDP(endpoint)
            }
        }
    }.getOrElse { cause ->
        throw IllegalStateException(
            "Playwright 원격 브라우저 연결에 실패했습니다. CHROMEDRIVER(엔드포인트) 값을 확인해주세요: $endpoint",
            cause
        )
    }

    private fun normalizeEndpoint(rawEndpoint: String): String {
        val trimmed = rawEndpoint.trim().removeSuffix("/")

        return when {
            trimmed.endsWith("/webdriver") -> trimmed.removeSuffix("/webdriver")
            trimmed.endsWith("/wd/hub") -> trimmed.removeSuffix("/wd/hub")
            else -> trimmed
        }
    }
}
