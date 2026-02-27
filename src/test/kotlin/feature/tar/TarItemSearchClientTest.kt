package feature.tar

import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TarItemSearchClientTest {

    @Test
    fun `relative href is resolved to absolute tar url`() {
        val resolved = TarItemSearchClient.resolveItemPageLink("/item/view/28590")
        assertEquals("https://ff14.tar.to/item/view/28590", resolved)
    }

    @Test
    fun `absolute href remains unchanged`() {
        val resolved = TarItemSearchClient.resolveItemPageLink("https://ff14.tar.to/item/view/28590")
        assertEquals("https://ff14.tar.to/item/view/28590", resolved)
    }

    @Test
    fun `item search e2e returns rebel coat for 반역자 외투`() {
        val isEnabled = System.getProperty("RUN_BROWSER_E2E") == "true" ||
            System.getenv("RUN_BROWSER_E2E") == "true"
        assumeTrue(
            isEnabled,
            "RUN_BROWSER_E2E=true 설정 시에만 실행됩니다."
        )

        val result = TarItemSearchClient().searchAndCapture("반역자 외투")
        val matched = assertIs<TarItemSearchResult.Matched>(result)

        assertEquals("Rebel Coat", matched.result.englishName)
        assertEquals(28590, matched.result.itemIdFromPageLink)
        assertEquals("몸통 방어구", matched.result.itemCategoryKorean)
        assertTrue(matched.result.itemPageLink.startsWith("https://ff14.tar.to/item/view/"))
        assertTrue(matched.result.screenshotFile.exists())
        assertNotNull(matched.result.screenshotFile.path)
    }
}
