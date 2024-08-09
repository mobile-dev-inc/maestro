package maestro.orchestra.ai.openai


import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import maestro.orchestra.ai.AI
import maestro.orchestra.ai.CompletionData
import org.slf4j.LoggerFactory

private const val API_URL = "https://api.openai.com/v1/chat/completions"

private val logger = LoggerFactory.getLogger(OpenAI::class.java)

class OpenAI(
    private val apiKey: String,
    private val defaultModel: String = "gpt-4o",
    private val defaultTemperature: Double = 0.2,
    private val defaultMaxTokens: Int = 2048,
    private val defaultImageDetail: String = "low",
) : AI() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            Json {
                ignoreUnknownKeys = true
            }
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 60000
            requestTimeoutMillis = 60000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chatCompletion(
        prompt: String,
        images: List<ByteArray>,
        temperature: Double?,
        model: String?,
        maxTokens: Int?,
        imageDetail: String?,
        identifier: String?,
    ): CompletionData {
        val imagesBase64 = images.map { it.encodeBase64() }

        // Fallback to OpenAI defaults
        val actualTemperature = temperature ?: defaultTemperature
        val actualModel = model ?: defaultModel
        val actualMaxTokens = maxTokens ?: defaultMaxTokens
        val actualImageDetail = imageDetail ?: defaultImageDetail

        val imagesContent = imagesBase64.map { image ->
            ContentDetail(
                type = "image_url",
                imageUrl = Base64Image(url = "data:image/png;base64,$image", detail = actualImageDetail),
            )
        }
        val textContent = ContentDetail(type = "text", text = prompt)

        val messages = listOf(
            MessageContent(
                role = "user",
                content = imagesContent + textContent,
            )
        )

        val chatCompletionRequest = ChatCompletionRequest(
            model = actualModel,
            temperature = actualTemperature,
            messages = messages,
            maxTokens = actualMaxTokens,
            seed = 1566,
            responseFormat = null,
        )

        val chatCompletionResponse = try {
            val httpResponse = client.post(API_URL) {
                contentType(ContentType.Application.Json)
                headers["Authorization"] = "Bearer $apiKey"
                setBody(json.encodeToString(chatCompletionRequest))
            }

            json.decodeFromString<ChatCompletionResponse>(httpResponse.bodyAsText())
        } catch (e: Exception) {
            logger.error("Failed to complete request to OpenAI", e)
            throw e
        }

        return CompletionData(
            prompt = prompt,
            temperature = actualTemperature.toFloat(),
            maxTokens = actualMaxTokens,
            images = imagesBase64,
            model = actualModel,
            response = chatCompletionResponse.choices.first().message.content,
        )
    }

    override fun close() = client.close()
}
