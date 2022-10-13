import database.NameTeachingTable
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import feature.FFLogFeature
import feature.NameTeachingFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

const val KEY_DISCORD_BOT_TOKEN = "discord_bot_token"
suspend fun main() = withContext(Dispatchers.IO) {
    val props = Properties()
    props.load(File("secret_profile.properties").inputStream())
    val discordBotKey: String? = props.getProperty(KEY_DISCORD_BOT_TOKEN)

    requireNotNull(discordBotKey) { "Discord bot token can't be null!!" }

    val kord = Kord(discordBotKey)
    Database.connect("jdbc:sqlite:database/romangway.db", "org.sqlite.JDBC")

    FFLogFeature(kord)
    NameTeachingFeature(kord)

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(NameTeachingTable)
    }

    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}