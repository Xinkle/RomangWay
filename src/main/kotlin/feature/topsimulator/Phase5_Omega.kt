package feature.topsimulator

import kotlin.random.Random

const val EMOJI_M_SWORD = "⚔️"
const val EMOJI_M_SHIELD = "🛡️"
const val EMOJI_F_STAFF = "🪄"
const val EMOJI_F_KNIFE = "🔪"
const val UPDOWN_ARROW = "↕"
const val LEFT_RIGHT_ARROW = "⟺"

@Suppress("ClassName")
class Phase5_Omega(val userName: String) {
    private var emojiF: String = ""
    private var emojiM: String = ""
    private var emojiArrow: String = ""
    private var randomType: Int = 0

    fun makeQuestion(): String {
        val seed = System.currentTimeMillis()
        val random = Random(seed)

        emojiF = listOf(EMOJI_F_KNIFE, EMOJI_F_STAFF).random(random)
        emojiM = listOf(EMOJI_M_SWORD, EMOJI_M_SHIELD).random(random)
        emojiArrow = listOf(LEFT_RIGHT_ARROW, UPDOWN_ARROW).random(random)
        //1 ->TOP-LEFT First / 2 -> TOP-RIGHT First
        randomType = listOf(1, 2).random(random)

        return print(emojiF, emojiM, emojiArrow, randomType)
    }

    private fun print(emojiF: String, emojiM: String, emojiArrow: String, randomType: Int): String =
        StringBuilder().apply {
            appendLine("|             A          ")
            appendLine("|    4              1    ")
            if (randomType == 1) {
                appendLine("|       $emojiF           ")
            } else {
                appendLine("|                $emojiF      ")
            }
            appendLine("|D         $emojiArrow         B")
            if (randomType == 1) {
                appendLine("|                $emojiM      ")
            } else {
                appendLine("|       $emojiM           ")
            }
            appendLine("|    3              2    ")
            appendLine("|             C          ")
        }.toString()

    fun getAnswer(): String {
        return if (randomType == 1) {
            if (emojiArrow == LEFT_RIGHT_ARROW) {
                when {
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SWORD -> "c3"
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SHIELD -> "c2"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SWORD -> "a1"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SHIELD -> "c1"
                    else -> "??"
                }
            } else {
                when {
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SWORD -> "b3"
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SHIELD -> "b2"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SWORD -> "d1"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SHIELD -> "b1"
                    else -> "??"
                }
            }
        } else {
            if (emojiArrow == LEFT_RIGHT_ARROW) {
                when {
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SWORD -> "c3"
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SHIELD -> "c2"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SWORD -> "a1"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SHIELD -> "c1"
                    else -> "??"
                }
            } else {
                when {
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SWORD -> "d3"
                    emojiF == EMOJI_F_STAFF && emojiM == EMOJI_M_SHIELD -> "d2"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SWORD -> "b1"
                    emojiF == EMOJI_F_KNIFE && emojiM == EMOJI_M_SHIELD -> "d1"
                    else -> "??"
                }
            }
        }
    }
}

fun ArrayList<Phase5_Omega>.addNew(userName: String): Phase5_Omega {
    removeIf { it.userName == userName }
    return Phase5_Omega(userName).also { add(it) }
}

fun ArrayList<Phase5_Omega>.pick(userName: String): Phase5_Omega =
    firstOrNull { it.userName == userName } ?: let {
        Phase5_Omega(userName).also { add(it) }
    }
