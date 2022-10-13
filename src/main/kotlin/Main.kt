import database.NameTeachingTable
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import feature.FFLogFeature
import feature.NameTeachingFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

suspend fun main() = withContext(Dispatchers.IO) {
    Database.connect("jdbc:sqlite:database/romangway.db", "org.sqlite.JDBC")

    val kord = Kord(Prop.getDiscordBotToken())

    // delete all commands
    kord.getGlobalApplicationCommands().toList().forEach {
        kord.rest.interaction.deleteGlobalApplicationCommand(it.applicationId, it.id)
    }

    val fflogFeature = FFLogFeature(kord)
    val nameTeachingFeature = NameTeachingFeature(kord)

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        listOf(fflogFeature, nameTeachingFeature)
            .first { it.command == command.data.name.value }
            .onGuildChatInputCommand(interaction)
    }

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