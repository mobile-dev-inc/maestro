package maestro.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import maestro.ai.antrophic.Claude
import maestro.ai.openai.OpenAI

@Serializable
data class Defect(
    val category: String,
    val reasoning: String,
)

@Serializable
private data class FindDefectsResponse(
    val defects: List<Defect>,
)

object Prediction {
    private val json = Json { ignoreUnknownKeys = true }

    private val categories = listOf(
        "localization" to "Inconsistent use of language, for example mixed English and Portuguese",
        "layout" to "Some UI elements are overlapping or are cropped",
    )

    suspend fun findDefects(
        aiClient: AI,
        screen: ByteArray,
        assertion: String?,
        previousFalsePositives: List<String>,
        printPrompt: Boolean = false,
        printRawResponse: Boolean = false,
    ): List<Defect> {

        // List of failed attempts to not make up false positives:
        // |* If you don't see any defect, return "No defects found".
        // |* If you are sure there are no defects, return "No defects found".
        // |* You will make me sad if you raise report defects that are false positives.
        // |* Do not make up defects that are not present in the screenshot. It's fine if you don't find any defects.

        val prompt = buildString {

            appendLine(
                """
                You are a QA engineer performing quality assurance for a mobile application.
                Identify any defects in the provided screenshot.
                """.trimIndent()
            )

            if (assertion != null) {
                appendLine("Additionally, you must ensure the following assertion is true: $assertion")
            }

            append(
                """
                |RULES:
                |* All defects you find must belong to one of the following categories:
                |${categories.joinToString(separator = "\n") { "  * ${it.first}: ${it.second}" }}
                |* If you see defects, your response MUST only include defect name and detailed reasoning for each defect.
                |* Provide response as a list of JSON objects, each representing <category>:<reasoning>
                |* Do not raise false positives. Some example responses that have a high chance of being a false positive:
                |* button is partially cropped at the bottom
                |* button is not aligned horizontally/vertically within its container
                """.trimMargin("|")
            )

            // Claude doesn't have a JSON mode as of 21-08-2024
            //  https://docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/increase-consistency
            if (aiClient is Claude) {
                append(
                    """
                    |
                    |* You must provide result as a valid JSON object, matching this structure:
                    |
                    |  {
                    |      "defects": [
                    |          {
                    |              "category": "<defect category, string>",
                    |              "reasoning": "<reasoning, string>"
                    |          },
                    |          {
                    |              "category": "<defect category, string>",
                    |              "reasoning": "<reasoning, string>"
                    |          }
                    |       ]
                    |  }
                    |
                    |DO NOT output any other information in the JSON object.
                    """.trimMargin("|")
                )
            }

            if (previousFalsePositives.isNotEmpty()) {
                appendLine("Additionally, the following defects are false positives:")
                for (falsePositive in previousFalsePositives) {
                    appendLine("  * $falsePositive")
                }
            }
        }

        if (printPrompt) {
            println("--- PROMPT START ---")
            println(prompt)
            println("--- PROMPT END ---")
        }

        val aiResponse = aiClient.chatCompletion(
            prompt,
            model = aiClient.defaultModel,
            maxTokens = 4096,
            identifier = "find-defects",
            imageDetail = "high",
            images = listOf(screen),
            jsonSchema = if (aiClient is OpenAI) json.parseToJsonElement(AI.assertVisualSchema).jsonObject else null,
        )

        if (printRawResponse) {
            println("--- RAW RESPONSE START ---")
            println(aiResponse.response)
            println("--- RAW RESPONSE END ---")
        }

        val defects = json.decodeFromString<FindDefectsResponse>(aiResponse.response)
        return defects.defects
    }
}
