package openai

import Prop
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates

class OpenAIClient {
    private var client: HttpClient by Delegates.notNull()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    init {
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(Prop.getOpenAIKey(), "")
                    }
                }
            }
        }
    }

    suspend fun request(prompt: String): String {
        val appendedPrompt = StringBuilder(prompt)
        val responseString = StringBuilder()

        do {
            val response = requestOpenAiCompletion(appendedPrompt.toString())
            appendedPrompt.append(response.choices.firstOrNull()?.text)
            responseString.append(response.choices.firstOrNull()?.text)

        } while (response.choices.firstOrNull()?.finishReason != "stop")

        return responseString.toString()
    }

    private suspend fun requestOpenAiCompletion(prompt: String): OpenAIResponse {
        println("Request OpenAI Completion with prompt -> $prompt")
        val request = OpenAIRequest(
            0,
            256,
            "text-davinci-003",
            0,
            prompt,
            0.7,
            1
        )
        return client.post("https://api.openai.com/v1/completions") {
            expectSuccess = true
            setBody(
                TextContent(
                    Json.encodeToString(request),
                    ContentType.Application.Json
                )
            )
        }.body<String>().let {
            val openAIResponse: OpenAIResponse = jsonParser.decodeFromString(it)
            openAIResponse
        }

    }
}