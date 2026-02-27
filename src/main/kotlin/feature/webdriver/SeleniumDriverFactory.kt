package feature.webdriver

import Prop
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.net.URL

object SeleniumDriverFactory {
    fun create(
        width: Int = 835,
        height: Int = 1080
    ): WebDriver =
        RemoteWebDriver(
            URL(Prop.getChromeDriver()),
            ChromeOptions().addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--force-device-scale-factor=1.5"
            )
        ).apply {
            manage().window().size = Dimension(width, height)
        }
}
