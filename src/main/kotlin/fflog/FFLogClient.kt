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
import org.slf4j.LoggerFactory
import java.net.URL

private val logger = LoggerFactory.getLogger(FFLogClient::class.java)

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
        logger.info("FFLogs 토큰 강제 갱신 요청")
        refreshToken(force = true)
    }

    private suspend fun refreshToken(force: Boolean): String = tokenRefreshMutex.withLock {
        val now = System.currentTimeMillis()
        if (!force && isTokenValid(now)) {
            logger.info("FFLogs 토큰 재사용: force={}, expiresAtMillis={}", force, tokenExpiresAtMillis)
            return tokenInfo!!.accessToken
        }

        val startedAt = System.nanoTime()
        logger.info("FFLogs 토큰 발급 요청 시작: force={}", force)
        val tokenInfo: TokenInfo =
            tokenClient.submitForm(url = "https://www.fflogs.com/oauth/token", formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", Prop.getFFlogClientId())
                append("client_secret", Prop.getFFLogClientSecret())
            }).body()

        this.tokenInfo = tokenInfo
        tokenExpiresAtMillis = now + (tokenInfo.expiresIn * 1000L)
        logger.info(
            "FFLogs 토큰 갱신 완료: expiresInSec={}, elapsedMs={}",
            tokenInfo.expiresIn,
            elapsedMs(startedAt)
        )

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
        val operationName = getFFlogQuery.operationName ?: getFFlogQuery::class.simpleName ?: "unknown"
        val accessToken = ensureValidAccessToken()
        val startedAt = System.nanoTime()
        logger.info("FFLogs 쿼리 실행 시작: operation={}", operationName)

        return try {
            client.executeQuery(apiUrl, getFFlogQuery, accessToken).also {
                logger.info(
                    "FFLogs 쿼리 실행 완료: operation={}, elapsedMs={}",
                    operationName,
                    elapsedMs(startedAt)
                )
            }
        } catch (e: ClientRequestException) {
            if (e.response.status != HttpStatusCode.Unauthorized) {
                throw e
            }

            logger.warn("FFLogs 쿼리 인증 실패(401), 토큰 재발급 후 재시도: operation={}", operationName)
            val refreshedToken = refreshToken(force = true)
            client.executeQuery(apiUrl, getFFlogQuery, refreshedToken).also {
                logger.info(
                    "FFLogs 쿼리 재시도 완료: operation={}, elapsedMs={}",
                    operationName,
                    elapsedMs(startedAt)
                )
            }
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

    private fun elapsedMs(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)
