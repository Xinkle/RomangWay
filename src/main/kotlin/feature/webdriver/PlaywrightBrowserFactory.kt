package feature.webdriver

import Prop
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PlaywrightBrowserFactory::class.java)

class PlaywrightSession(
    private val playwright: Playwright,
    val browser: Browser,
    val context: BrowserContext,
    val page: Page
) : AutoCloseable {
    override fun close() {
        logger.info("[BROWSER] 세션 종료 시작")
        runCatching { context.close() }
            .onFailure { logger.warn("[BROWSER] context 종료 중 오류: {}", it.message) }
        runCatching { browser.close() }
            .onFailure { logger.warn("[BROWSER] browser 종료 중 오류: {}", it.message) }
        runCatching { playwright.close() }
            .onFailure { logger.warn("[BROWSER] playwright 종료 중 오류: {}", it.message) }
        logger.info("[BROWSER] 세션 종료 완료")
    }
}

object PlaywrightBrowserFactory {
    fun createForTar(width: Int = 835, height: Int = 1080): PlaywrightSession {
        val endpoint = normalizeEndpoint(Prop.getChromeDriver())
        logger.info("[BROWSER] TAR 브라우저 세션 생성 시작: endpoint={}, viewport={}x{}", endpoint, width, height)
        val playwright = newPlaywright()
        val browser = connectForTar(playwright, endpoint)
        logger.info("[BROWSER] TAR 브라우저 연결 완료")
        return createSession(playwright, browser, width, height)
    }

    fun createForGlamour(width: Int = 835, height: Int = 1080): PlaywrightSession {
        val endpoint = normalizeEndpoint(Prop.getChromeCdp())
        logger.info("[BROWSER] GLAMOUR 브라우저 세션 생성 시작: endpoint={}, viewport={}x{}", endpoint, width, height)
        val playwright = newPlaywright()
        val browser = connectOverCdp(playwright, endpoint)
        logger.info("[BROWSER] GLAMOUR 브라우저 연결 완료")
        return createSession(playwright, browser, width, height)
    }

    private fun newPlaywright(): Playwright =
        Playwright.create(
            Playwright.CreateOptions().setEnv(
                HashMap(System.getenv()).apply {
                    put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
                }
            )
        )

    private fun createSession(
        playwright: Playwright,
        browser: Browser,
        width: Int,
        height: Int
    ): PlaywrightSession {
        val startedAt = System.nanoTime()
        val context = browser.newContext(
            Browser.NewContextOptions().setViewportSize(width, height)
        )
        val page = context.newPage()
        logger.info("[BROWSER] 브라우저 context/page 생성 완료: elapsedMs={}", elapsedMs(startedAt))

        return PlaywrightSession(
            playwright = playwright,
            browser = browser,
            context = context,
            page = page
        )
    }

    private fun connectForTar(playwright: Playwright, endpoint: String): Browser = runCatching {
        val startedAt = System.nanoTime()
        logger.info("[BROWSER] TAR 연결 시도: endpoint={}", endpoint)
        if (endpoint.startsWith("ws://") || endpoint.startsWith("wss://")) {
            playwright.chromium().connect(endpoint).also {
                logger.info("[BROWSER] TAR WS 연결 성공: elapsedMs={}", elapsedMs(startedAt))
            }
        } else {
            playwright.chromium().connectOverCDP(endpoint).also {
                logger.info("[BROWSER] TAR CDP 연결 성공: elapsedMs={}", elapsedMs(startedAt))
            }
        }
    }.getOrElse { cause ->
        throw IllegalStateException(
            "Playwright 연결에 실패했습니다. CHROMEDRIVER 값을 확인해주세요: $endpoint",
            cause
        )
    }

    private fun connectOverCdp(playwright: Playwright, endpoint: String): Browser = runCatching {
        val startedAt = System.nanoTime()
        logger.info("[BROWSER] GLAMOUR CDP 연결 시도: endpoint={}", endpoint)
        playwright.chromium().connectOverCDP(endpoint).also {
            logger.info("[BROWSER] GLAMOUR CDP 연결 성공: elapsedMs={}", elapsedMs(startedAt))
        }
    }.getOrElse { cause ->
        throw IllegalStateException(
            "Playwright CDP 연결에 실패했습니다. CHROME_CDP 값을 확인해주세요: $endpoint",
            cause
        )
    }

    private fun normalizeEndpoint(rawEndpoint: String): String {
        val trimmed = rawEndpoint.trim().removeSuffix("/")
        require(trimmed.isNotBlank()) { "브라우저 엔드포인트 값이 비어 있습니다." }

        return when {
            trimmed.endsWith("/webdriver") -> trimmed.removeSuffix("/webdriver")
            trimmed.endsWith("/wd/hub") -> trimmed.removeSuffix("/wd/hub")
            else -> trimmed
        }
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}
