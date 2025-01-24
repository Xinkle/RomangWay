package feature.topsimulator

import kotlin.random.Random

const val DEBUFF_FAR = "\uD83E\uDDCD||\uD83E\uDDCD"
const val DEBUFF_MIDDLE = "|\uD83E\uDDCD\uD83E\uDDCD|"

@Suppress("ClassName")
class Phase2_PartySynergy(val userName: String) {
    private var question: List<String> = emptyList()
    private var answer: IntArray = IntArray(9)
    private var isMiddle: Boolean = false
    var position: Int = -1

    fun introduce() = StringBuilder().apply {
        appendLine("Phase 2 - Party Synergy")
        appendLine("공략방식 - 09STOP")
        appendLine(" H1(1) T1(2) T2(3) D1(4) D2(5) D3(6) D4(7) H2(8)")
        appendLine("본인의 포지션을 숫자(1~8)로 입력하세요: ")
    }.toString()

    fun makeQuestion(): String = StringBuilder().apply {
        question = listOf("") + listOf("▢", "◯", "▽", "⨯", "◯", "⨯", "▢", "▽").shuffled()
        isMiddle = Random.nextBoolean()

        appendLine("|              ${getMiddle(isMiddle)}              |")
        appendLine(" ${question[1]}  ${question[2]}  ${question[3]}  ${question[4]}  ${question[5]}  ${question[6]}  ${question[7]}  ${question[8]}")
        appendLine("H1 T1 T2 D1 D2 D3 D4 H2")

        answer = IntArray(9) { -1 }
        val positionOrder = (1..8)

        positionOrder.forEachIndexed { idx, userPosition ->
            val answerPosition = question[userPosition].toPosition()

            if (answer[answerPosition] == -1) {
                answer[answerPosition] = userPosition
            } else {
                answer[answerPosition + 4] = userPosition
            }
        }

        if (!isMiddle) {
            answer.swap(5, 8)
            answer.swap(6, 7)
        }
    }.toString()

    fun getAnswer(): String = answer.indexOf(position).let {
        val answerBuilder = StringBuilder()

        if (it > 4) {
            answerBuilder.append("R")
        } else {
            answerBuilder.append("L")
        }

        answerBuilder.append(if (it > 4) it - 4 else it)

        if (isMiddle) {
            answerBuilder.append("M")
        } else {
            answerBuilder.append("F")
        }

        answerBuilder.toString()
    }


    fun isCorrect(userAns: String): Boolean {
        var userAnsPos = 0

        if (userAns[0].uppercase() == "R") {
            userAnsPos += 4
        }

        userAnsPos += userAns[1].toString().toInt()

        val isMiddleFarCorrect = if (isMiddle) {
            userAns[2].uppercase() == "M"
        } else {
            userAns[2].uppercase() == "F"
        }

        return (answer[userAnsPos] == position) && isMiddleFarCorrect
    }

    private fun String.toPosition(): Int = when (this) {
        "◯" -> 1
        "▽" -> 2
        "▢" -> 3
        "⨯" -> 4
        else -> -1
    }

    private fun getMiddle(isMiddle: Boolean) =
        if (isMiddle) DEBUFF_MIDDLE else DEBUFF_FAR

    private fun IntArray.swap(first: Int, second: Int) {
        val temp = get(first)

        this[first] = get(second)
        this[second] = temp
    }
}

fun ArrayList<Phase2_PartySynergy>.addNew(userName: String): Phase2_PartySynergy {
    removeIf { it.userName == userName }
    return Phase2_PartySynergy(userName).also { add(it) }
}

fun ArrayList<Phase2_PartySynergy>.pick(userName: String): Phase2_PartySynergy =
    firstOrNull { it.userName == userName } ?: let {
        Phase2_PartySynergy(userName).also { add(it) }
    }

