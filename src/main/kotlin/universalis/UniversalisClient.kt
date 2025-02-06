package universalis

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class UniversalisClient {

    // Ktor HTTP Client with JSON support
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Ignore unknown fields in JSON responses
            })
        }
    }

    // Fetch item price data from Universalis API
    suspend fun fetchItemPrice(worldId: Int, itemId: Int): UniversalisItemPrice? {
        val url = "https://universalis.app/api/v2/aggregated/$worldId/$itemId"
        return try {
            client.get(url).body() // Automatically deserializes to UniversalisItemPrice
        } catch (e: Exception) {
            println("Error fetching data: ${e.message}")
            null
        }
    }

    // Close the HTTP client when done
    fun close() {
        client.close()
    }
}