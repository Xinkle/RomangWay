package fflog

import Prop
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

class FFLogClient {
    private val tokenRefreshMutex = Mutex()
    private val tokenRefreshLeewayMillis = 60_000L
    private var tokenInfo: TokenInfo? = null
    private var tokenExpiresAtMillis: Long = 0L

    private val tokenClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun refreshToken() {
        refreshToken(force = true)
    }

    private suspend fun refreshToken(force: Boolean): String = tokenRefreshMutex.withLock {
        val now = System.currentTimeMillis()
        if (!force && isTokenValid(now)) {
            return tokenInfo!!.accessToken
        }

        val tokenInfo: TokenInfo =
            tokenClient.submitForm(url = "https://www.fflogs.com/oauth/token", formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", Prop.getFFlogClientId())
                append("client_secret", Prop.getFFLogClientSecret())
            }).body()

        this.tokenInfo = tokenInfo
        tokenExpiresAtMillis = now + (tokenInfo.expiresIn * 1000L)
        println("FFLogs token refreshed. expiresIn=${tokenInfo.expiresIn}s")

        tokenInfo.accessToken
    }

    private fun isTokenValid(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val currentTokenInfo = tokenInfo ?: return false
        if (currentTokenInfo.accessToken.isBlank()) return false

        return nowMillis < (tokenExpiresAtMillis - tokenRefreshLeewayMillis)
    }

    private suspend fun ensureValidAccessToken(): String {
        if (isTokenValid()) {
            return tokenInfo!!.accessToken
        }

        return refreshToken(force = false)
    }

    suspend fun <T : Any> executeQuery(getFFlogQuery: GraphQLClientRequest<T>): String {
        val apiUrl = URL("https://www.fflogs.com/api/v2/client")
        val accessToken = ensureValidAccessToken()

        return try {
            client.executeQuery(apiUrl, getFFlogQuery, accessToken)
        } catch (e: ClientRequestException) {
            if (e.response.status != HttpStatusCode.Unauthorized) {
                throw e
            }

            val refreshedToken = refreshToken(force = true)
            client.executeQuery(apiUrl, getFFlogQuery, refreshedToken)
        }
    }

    private suspend fun <T : Any> HttpClient.executeQuery(
        url: URL,
        graphQL: GraphQLClientRequest<T>,
        accessToken: String
    ): String {
        return post(url) {
            expectSuccess = true
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(
                TextContent(
                    GraphQLClientKotlinxSerializer().serialize(graphQL),
                    ContentType.Application.Json
                )
            )
        }.body()
    }
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)
