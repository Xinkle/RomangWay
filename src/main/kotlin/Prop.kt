import java.io.File
import java.util.*

object Prop {
    private const val KEY_DISCORD_BOT_TOKEN = "discord_bot_token"
    private const val KEY_FFLOG_CLIENT_ID = "fflog_client_id"
    private const val KEY_FFLOG_CLIENT_SECRET = "fflog_client_secret"

    val keyList = listOf(
        KEY_DISCORD_BOT_TOKEN,
        KEY_FFLOG_CLIENT_ID,
        KEY_FFLOG_CLIENT_SECRET
    )

    private val prop = Properties().apply {
        load(File("secret_profile.properties").inputStream())

        keyList.forEach { key ->
            requireNotNull(getProperty(key)) {
                "$key Can't be empty!!"
            }
        }
    }

    fun getDiscordBotToken(): String = prop.getProperty(KEY_DISCORD_BOT_TOKEN)
    fun getFFlogClientId(): String = prop.getProperty(KEY_FFLOG_CLIENT_ID)
    fun getFFLogClientSecret(): String = prop.getProperty(KEY_FFLOG_CLIENT_SECRET)
}