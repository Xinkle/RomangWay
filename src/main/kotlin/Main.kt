import database.CommandTeachingTable
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import feature.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


suspend fun main() = withContext(Dispatchers.IO) {
    Database.connect(
        "jdbc:mysql://${Prop.getDatabase()}", driver = "com.mysql.cj.jdbc.Driver",
        user = Prop.getDatabaseId(), password = Prop.getDatabasePw()
    )

    val kord = Kord(Prop.getDiscordBotToken())

    // delete all commands
//    kord.getGlobalApplicationCommands().toList().forEach {
//        kord.rest.interaction.deleteGlobalApplicationCommand(it.applicationId, it.id)
//    }

    val fflogFeature = FFLogFeature(kord)
    val commandTeachingFeature = CommandTeachingFeature(kord)
    val commandFindingFeature = CommandFindingFeature(kord)
    val itemSearchFeature = ItemSearchFeature(kord)
    val directHitCalculatorFeature = DirectHitCalculatorFeature(kord)

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        try {
            listOf(
                fflogFeature,
                commandTeachingFeature,
                commandFindingFeature,
                itemSearchFeature,
                directHitCalculatorFeature
            )
                .first { it.command == command.data.name.value }
                .onGuildChatInputCommand(interaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(CommandTeachingTable)
    }

    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}