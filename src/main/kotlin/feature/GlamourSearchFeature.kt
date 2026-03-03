package feature

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.edit
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.MessageComponentBuilder
import feature.eorzea.EorzeaArmorSlot
import feature.eorzea.EorzeaCollectionGlamourClient
import feature.eorzea.EorzeaGlamourResult
import feature.tar.TarItemSearchClient
import feature.tar.TarItemSearchResult
import feature.tar.toDiscordMessage
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.io.path.createTempFile
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_ITEM_NAME = "아이템이름"
private const val GLAMOUR_RESULT_LIMIT = 6
private const val MAX_BUTTONS_PER_ROW = 5
private const val TAR_ITEM_SEARCH_TIMEOUT_MS = 45_000L
private const val EORZEA_SEARCH_TIMEOUT_MS = 75_000L
private const val IMAGE_DOWNLOAD_TIMEOUT_MS = 30_000L
private const val IMAGE_CONNECT_TIMEOUT_MS = 8_000
private const val IMAGE_READ_TIMEOUT_MS = 20_000

class GlamourSearchFeature : CoroutineScope, ChatInputCommandInteractionListener {
    private val tarItemSearchClient = TarItemSearchClient()
    private val eorzeaCollectionGlamourClient = EorzeaCollectionGlamourClient()

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()

    override val command: String = "외형검색"

    override val arguments: List<CommandArgument> = listOf(
        CommandArgument(
            ARGUMENT_ITEM_NAME,
            "검색할 아이템 이름",
            true,
            ArgumentType.STRING
        )
    )

    override suspend fun onGuildChatInputCommand(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferPublicResponse()
        val initialized = runCatching {
            response.respond {
                content = "진행 중: 외형검색을 준비하고 있습니다."
            }
        }.isSuccess
        if (!initialized) {
            updateDeferredProgress(interaction, "외형검색을 준비하고 있습니다.")
        }
        val itemName = command.strings[ARGUMENT_ITEM_NAME]!!

        updateDeferredProgress(interaction, "TAR에서 아이템 정보를 조회 중입니다.")
        val tarResult = runWithCommandTimeout("TAR 아이템 검색", TAR_ITEM_SEARCH_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                tarItemSearchClient.searchAndCapture(itemName)
            }
        }

        when (tarResult) {
            is TarItemSearchResult.NotMatched -> {
                if (!updateDeferredMessage(interaction, tarResult.toDiscordMessage())) {
                    response.respond { content = tarResult.toDiscordMessage() }
                }
            }

            is TarItemSearchResult.Matched -> {
                val englishName = tarResult.result.englishName
                    .takeIf { it.isNotBlank() }
                    ?: run {
                        if (!updateDeferredMessage(interaction, "영문 아이템 이름 확인 불가")) {
                            response.respond { content = "영문 아이템 이름 확인 불가" }
                        }
                        return
                    }

                val slot = EorzeaArmorSlot.fromTarCategory(tarResult.result.itemCategoryKorean)
                    ?: run {
                        if (!updateDeferredMessage(
                                interaction,
                                "외형검색은 머리/몸통/손/다리/발 방어구만 지원합니다. (현재: ${tarResult.result.itemCategoryKorean ?: "미확인"})"
                            )
                        ) {
                            response.respond {
                                content = "외형검색은 머리/몸통/손/다리/발 방어구만 지원합니다. (현재: ${tarResult.result.itemCategoryKorean ?: "미확인"})"
                            }
                        }
                        return
                    }

                updateDeferredProgress(interaction, "Eorzea Collection 결과를 조회 중입니다.")
                val glamours = runWithCommandTimeout("외형 검색", EORZEA_SEARCH_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        eorzeaCollectionGlamourClient.findTopGlamours(
                            slot = slot,
                            itemEnglishName = englishName,
                            limit = GLAMOUR_RESULT_LIMIT
                        )
                    }
                }
                updateDeferredProgress(interaction, "외형 이미지를 다운로드 중입니다.")
                val attachments = runWithCommandTimeout("외형 이미지 다운로드", IMAGE_DOWNLOAD_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        downloadGlamourImages(glamours)
                    }
                }
                updateDeferredProgress(interaction, "결과를 디스코드로 전송 중입니다.")

                try {
                    val edited = runCatching {
                        interaction.getOriginalInteractionResponseOrNull()?.edit {
                            content = "아이템: $englishName"
                            attachments.forEach { file ->
                                files.add(file.toNamedFile())
                            }
                            components = buildGlamourLinkButtons(attachments).toMutableList()
                        } != null
                    }.getOrDefault(false)

                    if (!edited) {
                        response.respond {
                            content = "아이템: $englishName"
                            attachments.forEach { file ->
                                files.add(file.toNamedFile())
                            }
                            components = buildGlamourLinkButtons(attachments).toMutableList()
                        }
                    }
                } finally {
                    attachments.forEach { it.deleteQuietly() }
                }
            }
        }
    }

    private fun buildGlamourLinkButtons(files: List<DownloadedGlamourFile>): List<MessageComponentBuilder> =
        files.chunked(MAX_BUTTONS_PER_ROW).mapIndexed { rowIndex, chunk ->
            ActionRowBuilder().apply {
                chunk.forEachIndexed { chunkIndex, file ->
                    val displayIndex = rowIndex * MAX_BUTTONS_PER_ROW + chunkIndex + 1
                    linkButton(file.pageUrl) {
                        label = "${displayIndex}번 원본"
                    }
                }
            }
        }

    private fun downloadGlamourImages(glamours: List<EorzeaGlamourResult>): List<DownloadedGlamourFile> =
        glamours.mapIndexed { index, glamour ->
            val extension = extractImageExtension(glamour.imageUrl)
            val localFile = createTempFile(
                prefix = "glamour-${index + 1}-",
                suffix = ".$extension"
            ).toFile()

            val connection = URL(glamour.imageUrl).openConnection().apply {
                connectTimeout = IMAGE_CONNECT_TIMEOUT_MS
                readTimeout = IMAGE_READ_TIMEOUT_MS
            }

            connection.getInputStream().use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            DownloadedGlamourFile(
                pageUrl = glamour.pageUrl,
                localFile = localFile,
                fileName = "glamour-${index + 1}.$extension"
            )
        }

    private fun extractImageExtension(url: String): String {
        val path = runCatching { URI(url).path }.getOrDefault("")
        val extension = path.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "webp", "gif" -> extension
            else -> "jpg"
        }
    }
}

private data class DownloadedGlamourFile(
    val pageUrl: String,
    val localFile: File,
    val fileName: String
) {
    fun toNamedFile(): NamedFile =
        NamedFile(fileName, ChannelProvider { localFile.inputStream().toByteReadChannel() })

    fun deleteQuietly() {
        runCatching { localFile.delete() }
    }
}
