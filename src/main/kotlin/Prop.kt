import java.io.File
import java.util.*

object Prop {
    private const val KEY_DISCORD_BOT_TOKEN = "discord_bot_token"
    private const val KEY_FFLOG_CLIENT_ID = "fflog_client_id"
    private const val KEY_FFLOG_CLIENT_SECRET = "fflog_client_secret"
    private const val KEY_CHROMEDRIVER = "chromedriver"
    private const val KEY_DATABASE = "database"
    private const val KEY_DATABASE_ID = "db_id"
    private const val KEY_DATABASE_PW = "db_pw"
    private const val KEY_OPENAI = "open_ai_key"

    private val keyList = listOf(
        KEY_DISCORD_BOT_TOKEN,
        KEY_FFLOG_CLIENT_ID,
        KEY_FFLOG_CLIENT_SECRET,
        KEY_CHROMEDRIVER,
        KEY_DATABASE,
        KEY_OPENAI
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
    fun getChromeDriver(): String = prop.getProperty(KEY_CHROMEDRIVER)
    fun getDatabase(): String = prop.getProperty(KEY_DATABASE)
    fun getDatabaseId(): String = prop.getProperty(KEY_DATABASE_ID)
    fun getDatabasePw(): String = prop.getProperty(KEY_DATABASE_PW)
    fun getOpenAIKey(): String = prop.getProperty(KEY_OPENAI)
}