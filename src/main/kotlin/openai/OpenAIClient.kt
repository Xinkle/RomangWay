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
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

private val logger = LoggerFactory.getLogger(OpenAIClient::class.java)

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
        val startedAt = System.nanoTime()
        logger.info("OpenAI 요청 시작: promptLength={}", prompt.length)
        val appendedPrompt = StringBuilder(prompt)
        val responseString = StringBuilder()

        do {
            val response = requestOpenAiCompletion(appendedPrompt.toString())
            appendedPrompt.append(response.choices.firstOrNull()?.text)
            responseString.append(response.choices.firstOrNull()?.text)

        } while (response.choices.firstOrNull()?.finishReason != "stop")

        logger.info("OpenAI 요청 완료: responseLength={}, elapsedMs={}", responseString.length, elapsedMs(startedAt))
        return responseString.toString()
    }

    private suspend fun requestOpenAiCompletion(prompt: String): OpenAIResponse {
        logger.debug("OpenAI completion 요청: promptLength={}", prompt.length)
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

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}
