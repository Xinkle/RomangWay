package openai


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    @SerialName("frequency_penalty")
    val frequencyPenalty: Int,
    @SerialName("max_tokens")
    val maxTokens: Int,
    @SerialName("model")
    val model: String,
    @SerialName("presence_penalty")
    val presencePenalty: Int,
    @SerialName("prompt")
    val prompt: String,
    @SerialName("temperature")
    val temperature: Double,
    @SerialName("top_p")
    val topP: Int
)