package feature.webdriver

import Prop
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.net.URL

private val logger = LoggerFactory.getLogger(SeleniumDriverFactory::class.java)

object SeleniumDriverFactory {
    fun create(
        width: Int = 835,
        height: Int = 1080
    ): WebDriver {
        val endpoint = Prop.getChromeDriver()
        logger.info("[SELENIUM] 원격 드라이버 연결 시도: endpoint={}, viewport={}x{}", endpoint, width, height)

        return RemoteWebDriver(
            URL(endpoint),
            ChromeOptions().addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--force-device-scale-factor=1.5"
            )
        ).apply {
            manage().window().size = Dimension(width, height)
            logger.info("[SELENIUM] 원격 드라이버 연결 완료")
        }
    }
}
