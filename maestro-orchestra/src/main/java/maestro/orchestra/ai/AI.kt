package maestro.orchestra.ai

import java.io.Closeable

data class CompletionData(
    val prompt: String,
    val model: String,
    val temperature: Float,
    val maxTokens: Int,
    val images: List<String>,
    val response: String,
)

abstract class AI : Closeable {

    abstract suspend fun chatCompletion(
        prompt: String,
        images: List<ByteArray> = listOf(),
        temperature: Double? = null,
        model: String? = null,
        maxTokens: Int? = null,
        imageDetail: String? = null,
        identifier: String? = null,
    ): CompletionData

}
