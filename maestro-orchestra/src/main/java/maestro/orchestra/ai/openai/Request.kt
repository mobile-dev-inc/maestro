package maestro.orchestra.ai.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageContent>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: ResponseFormat?,
    val seed: Int,
)

@Serializable
data class ResponseFormat(
    val type: String,
)

@Serializable
data class MessageContent(
    val role: String,
    val content: List<ContentDetail>,
)

@Serializable
data class ContentDetail(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: Base64Image? = null,
)

@Serializable
data class Base64Image(
    val url: String,
    val detail: String,
)
