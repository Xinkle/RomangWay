import database.CommandTeachingTable
import database.ItemTable
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
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
    // 데이터베이스 연결 설정 (MariaDB 사용)
    Database.connect(
        "jdbc:mariadb://${Prop.getDatabase()}",
        driver = "org.mariadb.jdbc.Driver",
        user = Prop.getDatabaseId(),
        password = Prop.getDatabasePw()
    )

    // Discord 봇 인스턴스 초기화
    val kord = Kord(Prop.getDiscordBotToken())

    // Kord REST API를 통해 현재 등록된 글로벌 커맨드 목록을 조회합니다.
    val existingCommands = try {
        kord.rest.interaction.getGlobalApplicationCommands(kord.selfId)
    } catch (e: Exception) {
        println("커맨드 목록 조회 실패: $e")
        emptyList()
    }

    // FFLog 클라이언트 초기화 및 토큰 갱신
    val fflogClient = FFLogClient()
    fflogClient.refreshToken()

    // 각 기능(피처)들의 인스턴스 생성
    val fflogFeature = FFLogFeature(fflogClient)
    val ffLogDeathAnalyzeFeature = FFLogDeathAnalyzeFeature(kord, fflogClient)
    val commandTeachingFeature = CommandTeachingFeature(kord)
    val commandFindingFeature = CommandFindingFeature()
    val itemSearchFeature = ItemSearchFeature()
    val directHitCalculatorFeature = DirectHitCalculatorFeature()
    val openAiChatFeature = OpenAiChatFeature(kord)
    val topSimulatorFeature = TopSimulatorFeature(kord)
    val commandDeletingFeature = CommandDeletingFeature()
    val commandRestoringFeature = CommandRestoringFeature()

    // 등록할 커맨드 기능 리스트 구성
    val commandList = listOf(
        fflogFeature,
        commandTeachingFeature,
        commandFindingFeature,
        itemSearchFeature,
        directHitCalculatorFeature,
        ffLogDeathAnalyzeFeature,
        topSimulatorFeature,
        commandDeletingFeature,
        commandRestoringFeature
    )

    // 각 커맨드 기능을 등록 (이미 등록된 글로벌 커맨드 목록과 비교하여 처리)
    commandList.forEach {
        it.registerCommand(kord, existingCommands)
    }

    // Guild(서버) 내에서 채팅 입력 커맨드 상호작용 이벤트 리스너 등록
    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        try {
            // 들어온 커맨드 이름에 해당하는 기능을 찾아서 실행
            commandList.first { it.command == command.data.name.value }
                .onGuildChatInputCommand(interaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // 데이터베이스 스키마 설정: 필요한 테이블 생성
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(CommandTeachingTable)
        SchemaUtils.create(ItemTable)
    }

    println("Romangway Login...")

    // 봇 로그인: 봇이 Discord 이벤트를 수신하도록 로그인
    kord.login {
        // 메시지 내용을 수신하기 위해 필수 권한(Intents) 설정
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
