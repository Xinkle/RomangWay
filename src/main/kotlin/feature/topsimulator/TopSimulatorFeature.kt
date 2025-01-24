package feature.topsimulator

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.MessageComponentBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.interaction.string
import feature.GuildChatInputCommandInteractionListener
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val ARGUMENT_TOP_PHASE = "절메가페이즈"
private const val ID_P2_POSITION = "ID_P2_POSITION"
private const val ID_P2_ANSWER_L1F = "L1F"
private const val ID_P2_ANSWER_L2F = "L2F"
private const val ID_P2_ANSWER_L3F = "L3F"
private const val ID_P2_ANSWER_L4F = "L4F"
private const val ID_P2_ANSWER_R1F = "R1F"
private const val ID_P2_ANSWER_R2F = "R2F"
private const val ID_P2_ANSWER_R3F = "R3F"
private const val ID_P2_ANSWER_R4F = "R4F"
private const val ID_P2_ANSWER_L1M = "L1M"
private const val ID_P2_ANSWER_L2M = "L2M"
private const val ID_P2_ANSWER_L3M = "L3M"
private const val ID_P2_ANSWER_L4M = "L4M"
private const val ID_P2_ANSWER_R1M = "R1M"
private const val ID_P2_ANSWER_R2M = "R2M"
private const val ID_P2_ANSWER_R3M = "R3M"
private const val ID_P2_ANSWER_R4M = "R4M"

private const val ID_P5_ANSWER_A1 = "A1"
private const val ID_P5_ANSWER_A2 = "A2"
private const val ID_P5_ANSWER_A3 = "A3"
private const val ID_P5_ANSWER_B1 = "B1"
private const val ID_P5_ANSWER_B2 = "B2"
private const val ID_P5_ANSWER_B3 = "B3"
private const val ID_P5_ANSWER_C1 = "C1"
private const val ID_P5_ANSWER_C2 = "C2"
private const val ID_P5_ANSWER_C3 = "C3"
private const val ID_P5_ANSWER_D1 = "D1"
private const val ID_P5_ANSWER_D2 = "D2"
private const val ID_P5_ANSWER_D3 = "D3"

class TopSimulatorFeature(private val kord: Kord) : CoroutineScope, GuildChatInputCommandInteractionListener {
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob()
    override val command: String = "절메가"

    companion object {
        val p2PhaseList = ArrayList<Phase2_PartySynergy>()
        val p5PhaseList = ArrayList<Phase5_Omega>()
    }

    private val p2AnswerList = listOf(
        ID_P2_ANSWER_L1F,
        ID_P2_ANSWER_L2F,
        ID_P2_ANSWER_L3F,
        ID_P2_ANSWER_L4F,
        ID_P2_ANSWER_R1F,
        ID_P2_ANSWER_R2F,
        ID_P2_ANSWER_R3F,
        ID_P2_ANSWER_R4F,
        ID_P2_ANSWER_L1M,
        ID_P2_ANSWER_L2M,
        ID_P2_ANSWER_L3M,
        ID_P2_ANSWER_L4M,
        ID_P2_ANSWER_R1M,
        ID_P2_ANSWER_R2M,
        ID_P2_ANSWER_R3M,
        ID_P2_ANSWER_R4M
    )

    private val p5AnswerList = listOf(
        ID_P5_ANSWER_A1,
        ID_P5_ANSWER_A2,
        ID_P5_ANSWER_A3,
        ID_P5_ANSWER_B1,
        ID_P5_ANSWER_B2,
        ID_P5_ANSWER_B3,
        ID_P5_ANSWER_C1,
        ID_P5_ANSWER_C2,
        ID_P5_ANSWER_C3,
        ID_P5_ANSWER_D1,
        ID_P5_ANSWER_D2,
        ID_P5_ANSWER_D3,
    )

    private val p2QuestionButtons = mutableListOf(
        ActionRowBuilder().apply {
            answerButton(ID_P2_ANSWER_L1F)
            answerButton(ID_P2_ANSWER_L1M)
            answerButton(ID_P2_ANSWER_R1M)
            answerButton(ID_P2_ANSWER_R1F)
        },
        ActionRowBuilder().apply {
            answerButton(ID_P2_ANSWER_L2F)
            answerButton(ID_P2_ANSWER_L2M)
            answerButton(ID_P2_ANSWER_R2M)
            answerButton(ID_P2_ANSWER_R2F)
        },
        ActionRowBuilder().apply {
            answerButton(ID_P2_ANSWER_L3F)
            answerButton(ID_P2_ANSWER_L3M)
            answerButton(ID_P2_ANSWER_R3M)
            answerButton(ID_P2_ANSWER_R3F)
        },
        ActionRowBuilder().apply {
            answerButton(ID_P2_ANSWER_L4F)
            answerButton(ID_P2_ANSWER_L4M)
            answerButton(ID_P2_ANSWER_R4M)
            answerButton(ID_P2_ANSWER_R4F)
        }
    )

    private val p5QuestionButtons = mutableListOf<MessageComponentBuilder>(
        ActionRowBuilder().apply {
            answerButton(ID_P5_ANSWER_A1)
            answerButton(ID_P5_ANSWER_B1)
            answerButton(ID_P5_ANSWER_C1)
            answerButton(ID_P5_ANSWER_D1)
        },
        ActionRowBuilder().apply {
            answerButton(ID_P5_ANSWER_A2)
            answerButton(ID_P5_ANSWER_B2)
            answerButton(ID_P5_ANSWER_C2)
            answerButton(ID_P5_ANSWER_D2)
        },
        ActionRowBuilder().apply {
            answerButton(ID_P5_ANSWER_A3)
            answerButton(ID_P5_ANSWER_B3)
            answerButton(ID_P5_ANSWER_C3)
            answerButton(ID_P5_ANSWER_D3)
        }
    )

    init {
        launch {
            kord.createGlobalChatInputCommand(
                command, "절메가 시뮬레이션을 시작합니다"
            ) {
                string(ARGUMENT_TOP_PHASE, "연습할 페이즈 선택") {
                    required = true
                    choice("2페이즈 - 파티 시너지", "p2")
                    choice("5페이즈 - 오메가", "p5")
                }
            }

            kord.on<GuildButtonInteractionCreateEvent> {
                val userName = interaction.user.data.username

                if (p2AnswerList.contains(interaction.component.customId)) {
                    val p2 = p2PhaseList.pick(userName)
                    val p2Answer = interaction.component.customId!!

                    val trueAnswer = p2.getAnswer()
                    val isCorrect = p2.isCorrect(p2Answer)

                    val responseContent = if (isCorrect) {
                        "정답입니다!\n${p2.makeQuestion()}"
                    } else {
                        "오답입니다! 정답 -> $trueAnswer\n${p2.makeQuestion()}"
                    }

                    interaction.updateEphemeralMessage {
                        content = responseContent
                        components?.clear()
                        components?.addAll(p2QuestionButtons)
                    }
                } else if (p5AnswerList.contains(interaction.component.customId)) {
                    val p5 = p5PhaseList.pick(userName)
                    val p5Answer = interaction.component.customId!!

                    val trueAnswer = p5.getAnswer().toUpperCasePreservingASCIIRules()
                    val isCorrect = p5Answer == trueAnswer

                    val responseContent = if (isCorrect) {
                        "정답입니다!\n${p5.makeQuestion()}"
                    } else {
                        "오답입니다! 정답 -> $trueAnswer\n${p5.makeQuestion()}"
                    }

                    interaction.updateEphemeralMessage {
                        content = responseContent
                        components?.clear()
                        components?.addAll(p5QuestionButtons)
                    }
                }
            }

            kord.on<GuildSelectMenuInteractionCreateEvent> {
                val userName = interaction.user.data.username
                val selected = interaction.data.data.values.value!!.first()
                when (interaction.component.customId) {
                    ID_P2_POSITION -> {
                        val p2 = p2PhaseList.pick(userName)
                        p2.position = selected.toInt()

                        interaction.updateEphemeralMessage {
                            content = p2.makeQuestion()
                            components?.clear()
                            components?.addAll(p2QuestionButtons)
                        }
                    }
                }
            }
        }
    }

    private fun ActionRowBuilder.answerButton(label: String) {
        interactionButton(ButtonStyle.Secondary, label) {
            this.label = label
        }
    }

    override suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction) {
        val command = interaction.command
        val response = interaction.deferEphemeralResponse()

        try {
            when (command.strings[ARGUMENT_TOP_PHASE]!!) {
                "p2" -> {
                    val p2 = p2PhaseList.addNew(interaction.user.data.username)
                    response.respond {
                        content = p2.introduce()
                        components = mutableListOf(
                            ActionRowBuilder().apply {
                                stringSelect(ID_P2_POSITION) {
                                    (1..8).forEach {
                                        this.option("$it", "$it")
                                    }
                                }
                            }
                        )
                    }
                }

                "p5" -> {
                    val p5 = p5PhaseList.addNew(interaction.user.data.username)
                    response.respond {
                        content = p5.makeQuestion()
                        components = p5QuestionButtons
                    }
                }
            }
        } catch (e: Exception) {
            println("Error occurred -> $e")

            response.respond {
                content = "알수없는 오류가 발생했어요..."
            }
        }
    }
}