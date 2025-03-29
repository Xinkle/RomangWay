package feature.universalis

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Item(
    val en: String,
    val de: String,
    val ja: String,
    val fr: String
)

object JsonItemFinder {
    fun findItemIdByEnField(input: String): Int? {
        // Load the item.json file from the resources folder
        val classLoader = Thread.currentThread().contextClassLoader
        val file = classLoader.getResource("item.json")?.readText()
            ?: throw IllegalArgumentException("item.json not found in resources folder")

        // Parse the JSON to a Map
        val itemMap: Map<String, Item> = Json.decodeFromString(file)

        // Search for the item where the `en` field matches the input
        return itemMap.entries.firstOrNull { (_, item) ->
            item.en == input
        }?.key?.toInt() // Return the id (key) if found, otherwise null
    }
}
