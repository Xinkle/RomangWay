package feature

import Prop
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import creat.xinkle.Romangway.GetFFlogQuery
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import feature.model.FFlogRanking
import feature.model.FFlogRankingSummary
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.kotlinx.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private const val ARGUMENT_NAME = "이름"
private const val ARGUMENT_SERVER = "서버"
private const val ARGUMENT_EXPOSABLE = "공개여부"

class FFLogFeature(private val kord: Kord) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    private val bearerTokenStorage = mutableListOf<BearerTokens>()
    private var client: HttpClient by Delegates.notNull()

    private val serverMapping = mapOf(
        "톤베리" to "Tonberry",
        "카벙클" to "Carbuncle",
        "초코보" to "Chocobo",
        "펜리르" to "Fenrir",
        "모그리" to "Moogle"
    )
    override val command: String = "프프로그"

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command

        val name = command.strings[ARGUMENT_NAME]!!
        val server = command.strings[ARGUMENT_SERVER]!!
        val isExposable = command.booleans[ARGUMENT_EXPOSABLE]!!
        val mappedServer = serverMapping[server]

        val response = if (isExposable) {
            interaction.deferPublicResponse()
        } else {
            interaction.deferEphemeralResponse()
        }

        try {
            requireNotNull(mappedServer) { "서버 이름이 올바르지 않습니다." }

            val ranking = getFFlog(name, mappedServer)

            response.respond {
                content = FFlogRankingSummary.fromRanking(ranking, name, server).toString()
            }
        } catch (e: Exception) {
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }

    init {
        println("$command module registered!")

        launch {
            refreshToken()

            kord.createGlobalChatInputCommand(
                command, "프프로그 정보를 가져옵니다."
            ) {
                string(ARGUMENT_NAME, "가져올 유저의 이름.") {
                    required = true
                }
                string(ARGUMENT_SERVER, "가져올 유저의 서버") {
                    required = true
                }
                boolean(ARGUMENT_EXPOSABLE, "조회한 로그의 공개여부") {
                    required = true
                }
            }
        }
    }

    private suspend fun getFFlog(name: String, server: String): FFlogRanking {
        val result = client.executeQuery(
            URL("https://www.fflogs.com/api/v2/client"), GetFFlogQuery(
                GetFFlogQuery.Variables(name = name, server = server)
            )
        )

        val fflogRanking: FFlogRanking = Json.decodeFromString(result)
        println(fflogRanking)

        return fflogRanking
    }

    private suspend fun refreshToken() {
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
}

private suspend fun <T : Any> HttpClient.executeQuery(
    url: URL, graphQL: GraphQLClientRequest<T>
): String {
    return post(url) {
        expectSuccess = true
        setBody(TextContent(GraphQLClientKotlinxSerializer().serialize(graphQL), ContentType.Application.Json))
    }.body()
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)