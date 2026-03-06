import database.CommandTeachingTable
import database.ItemTable
import dev.kord.common.entity.DiscordApplicationCommand
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
import org.slf4j.LoggerFactory
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = LoggerFactory.getLogger("Main")
private const val LEGACY_GLAMOUR_COMMAND = "외형검색"
private const val LEGACY_FFLOG_COMMAND = "프프로그"

suspend fun main() = withContext(Dispatchers.IO) {
    logger.info("Romangway 애플리케이션 시작")

    // 데이터베이스 연결 설정 (MariaDB 사용)
    logger.info("데이터베이스 연결 시도: jdbc:mariadb://{}", Prop.getDatabase())
    Database.connect(
        "jdbc:mariadb://${Prop.getDatabase()}",
        driver = "org.mariadb.jdbc.Driver",
        user = Prop.getDatabaseId(),
        password = Prop.getDatabasePw()
    )
    logger.info("데이터베이스 연결 완료")

    // Discord 봇 인스턴스 초기화
    logger.info("Discord Kord 인스턴스 초기화")
    val kord = Kord(Prop.getDiscordBotToken())

    // Kord REST API를 통해 현재 등록된 글로벌 커맨드 목록을 조회합니다.
    logger.info("기존 글로벌 커맨드 목록 조회 시작")
    val existingCommands = try {
        kord.rest.interaction.getGlobalApplicationCommands(kord.selfId)
    } catch (e: Exception) {
        logger.warn("커맨드 목록 조회 실패: {}", e.message, e)
        emptyList()
    }
    logger.info("기존 글로벌 커맨드 목록 조회 완료: {}개", existingCommands.size)
    deleteLegacyCommandIfExists(kord, existingCommands, LEGACY_GLAMOUR_COMMAND)
    deleteLegacyCommandIfExists(kord, existingCommands, LEGACY_FFLOG_COMMAND)

    // FFLog 클라이언트 초기화 및 토큰 갱신
    logger.info("FFLog 클라이언트 초기화 및 토큰 갱신 시작")
    val fflogClient = FFLogClient()
    fflogClient.refreshToken()
    logger.info("FFLog 토큰 갱신 완료")

    // 각 기능(피처)들의 인스턴스 생성
    val fflogFeature = FFLogFeature(kord, fflogClient)
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
    logger.info("커맨드 등록 시작: {}개", commandList.size)
    commandList.forEach {
        it.registerCommand(kord, existingCommands)
    }
    logger.info("커맨드 등록 완료")

    // Guild(서버) 내에서 채팅 입력 커맨드 상호작용 이벤트 리스너 등록
    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        try {
            // 들어온 커맨드 이름에 해당하는 기능을 찾아서 실행
            commandList.first { it.command == command.data.name.value }
                .onGuildChatInputCommandSafely(interaction)
        } catch (e: Exception) {
            logger.error("커맨드 라우팅 실패: command={}", command.data.name.value, e)
        }
    }


    // 데이터베이스 스키마 설정: 필요한 테이블 생성
    logger.info("데이터베이스 스키마 초기화 시작")
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(CommandTeachingTable)
        SchemaUtils.create(ItemTable)
    }
    logger.info("데이터베이스 스키마 초기화 완료")

    logger.info("Discord 로그인 시작")

    // 봇 로그인: 봇이 Discord 이벤트를 수신하도록 로그인
    kord.login {
        // 메시지 내용을 수신하기 위해 필수 권한(Intents) 설정
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}

private suspend fun deleteLegacyCommandIfExists(
    kord: Kord,
    existingCommands: List<DiscordApplicationCommand>,
    legacyCommandName: String
) {
    val legacyCommand = existingCommands.firstOrNull { it.name == legacyCommandName } ?: return

    runCatching {
        kord.rest.interaction.deleteGlobalApplicationCommand(kord.selfId, legacyCommand.id)
    }.onSuccess {
        logger.info("레거시 글로벌 커맨드 삭제 완료: name={}, id={}", legacyCommandName, legacyCommand.id.value)
    }.onFailure { e ->
        logger.warn("레거시 글로벌 커맨드 삭제 실패: name={}, id={}", legacyCommandName, legacyCommand.id.value, e)
    }
}
