package openai


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIResponse(
    @SerialName("choices")
    val choices: List<Choice>,
    @SerialName("created")
    val created: Int,
    @SerialName("id")
    val id: String,
) {
    @Serializable
    data class Choice(
        @SerialName("finish_reason")
        val finishReason: String,
        @SerialName("index")
        val index: Int,
        @SerialName("text")
        val text: String
    )
}