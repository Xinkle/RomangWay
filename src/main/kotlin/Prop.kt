import java.io.File
import java.util.*

object Prop {
    private const val KEY_SECRET_PROFILE_PATH = "SECRET_PROFILE_PATH"
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
        KEY_DATABASE_ID,
        KEY_DATABASE_PW,
        KEY_OPENAI
    )

    private val envKeyMapping = mapOf(
        KEY_DISCORD_BOT_TOKEN to listOf("DISCORD_BOT_TOKEN"),
        KEY_FFLOG_CLIENT_ID to listOf("FFLOGS_CLIENT_ID", "FFLOG_CLIENT_ID"),
        KEY_FFLOG_CLIENT_SECRET to listOf("FFLOGS_CLIENT_SECRET", "FFLOG_CLIENT_SECRET"),
        KEY_CHROMEDRIVER to listOf("CHROMEDRIVER"),
        KEY_DATABASE to listOf("DATABASE"),
        KEY_DATABASE_ID to listOf("DB_ID", "DATABASE_ID"),
        KEY_DATABASE_PW to listOf("DB_PW", "DATABASE_PW"),
        KEY_OPENAI to listOf("OPEN_AI_KEY", "OPENAI_API_KEY")
    )

    private fun resolveSecretProfileFile(): File {
        val customPath = System.getenv(KEY_SECRET_PROFILE_PATH)?.trim().orEmpty()
        if (customPath.isNotEmpty()) return File(customPath)
        return File("secret_profile.properties")
    }

    private val prop = Properties().apply {
        val secretProfileFile = resolveSecretProfileFile()
        if (secretProfileFile.exists()) {
            secretProfileFile.inputStream().use(::load)
        }

        // Environment variables override file values for containerized deployments.
        envKeyMapping.forEach { (propKey, envKeys) ->
            val envValue = envKeys
                .asSequence()
                .mapNotNull { System.getenv(it)?.trim()?.takeIf(String::isNotEmpty) }
                .firstOrNull()

            if (envValue != null) {
                setProperty(propKey, envValue)
            }
        }

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
