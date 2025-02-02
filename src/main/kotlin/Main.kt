import database.CommandTeachingTable
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import feature.*
import feature.topsimulator.TopSimulatorFeature
import fflog.FFLogClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction


suspend fun main() = withContext(Dispatchers.IO) {
    Database.connect(
        "jdbc:mariadb://${Prop.getDatabase()}",
        driver = "org.mariadb.jdbc.Driver",
        user = Prop.getDatabaseId(),
        password = Prop.getDatabasePw()
    )

    val kord = Kord(Prop.getDiscordBotToken())

    // delete all commands
//    kord.getGlobalApplicationCommands().toList().forEach {
//        kord.rest.interaction.deleteGlobalApplicationCommand(it.applicationId, it.id)
//    }

    val fflogClient = FFLogClient()
    fflogClient.refreshToken()

    val fflogFeature = FFLogFeature(kord, fflogClient)
    val ffLogDeathAnalyzeFeature = FFLogDeathAnalyzeFeature(kord, fflogClient)
    val commandTeachingFeature = CommandTeachingFeature(kord)
    val commandFindingFeature = CommandFindingFeature(kord)
    val itemSearchFeature = ItemSearchFeature(kord)
    val directHitCalculatorFeature = DirectHitCalculatorFeature(kord)
    val openAiChatFeature = OpenAiChatFeature(kord)
    val topSimulatorFeature = TopSimulatorFeature(kord)

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        try {
            listOf(
                fflogFeature,
                commandTeachingFeature,
                commandFindingFeature,
                itemSearchFeature,
                directHitCalculatorFeature,
                ffLogDeathAnalyzeFeature,
                topSimulatorFeature
            ).first { it.command == command.data.name.value }
                .onGuildChatInputCommand(interaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(CommandTeachingTable)
    }

    println("Login...")
    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}