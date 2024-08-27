package maestro.ai

import kotlinx.serialization.json.JsonObject
import java.io.Closeable

data class CompletionData(
    val prompt: String,
    val model: String,
    val temperature: Float,
    val maxTokens: Int,
    val images: List<String>,
    val response: String,
)

abstract class AI(
    val defaultModel: String,
) : Closeable {

    /**
     * Chat completion with the AI model.
     *
     * Caveats:
     *  - `jsonSchema` is only supported by OpenAI ("Structured Outputs" feature)
     */
    abstract suspend fun chatCompletion(
        prompt: String,
        images: List<ByteArray> = listOf(),
        temperature: Float? = null,
        model: String? = null,
        maxTokens: Int? = null,
        imageDetail: String? = null,
        identifier: String? = null,
        jsonSchema: JsonObject? = null,
    ): CompletionData

    companion object {
        const val AI_KEY_ENV_VAR = "MAESTRO_CLI_AI_KEY"
        const val AI_MODEL_ENV_VAR = "MAESTRO_CLI_AI_MODEL"

        // We use JSON mode/Structured Outputs to define the schema of the response we expect from the LLM.
        // * OpenAI: https://platform.openai.com/docs/guides/structured-outputs
        // * Gemini: https://ai.google.dev/gemini-api/docs/json-mode

        val askForDefectsSchema: String = run {
            val resourceStream = this::class.java.getResourceAsStream("/askForDefects_schema.json")
                ?: throw IllegalStateException("Could not find askForDefects_schema.json in resources")

            resourceStream.bufferedReader().use { it.readText() }
        }
    }

}
