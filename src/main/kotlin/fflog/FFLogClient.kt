package fflog

import Prop
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL
import kotlin.properties.Delegates

class FFLogClient {
    private val bearerTokenStorage = mutableListOf<BearerTokens>()
    private var client: HttpClient by Delegates.notNull()

    suspend fun refreshToken() {
        val tokenClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val tokenInfo: TokenInfo =
            tokenClient.submitForm(url = "https://www.fflogs.com/oauth/token", formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", Prop.getFFlogClientId())
                append("client_secret", Prop.getFFLogClientSecret())
            }).body()

        println(tokenInfo)

        bearerTokenStorage.add(
            BearerTokens(
                tokenInfo.accessToken, ""
            )
        )

        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokenStorage.last()
                    }
                    sendWithoutRequest { req ->
                        req.url.host == "www.fflogs.com"
                    }
                }
            }
        }
    }

    suspend fun <T : Any> executeQuery(getFFlogQuery: GraphQLClientRequest<T>): String {
        return client.executeQuery(
            URL("https://www.fflogs.com/api/v2/client"), getFFlogQuery
        )
    }

    private suspend fun <T : Any> HttpClient.executeQuery(
        url: URL, graphQL: GraphQLClientRequest<T>
    ): String {
        return post(url) {
            expectSuccess = true
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