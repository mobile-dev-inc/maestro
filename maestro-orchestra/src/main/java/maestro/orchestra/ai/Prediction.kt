package maestro.orchestra.ai

object Prediction {

    private val categories = listOf(
        "localization" to "Inconsistent use of language, for example mixed English and Portuguese",
        "layout" to "Some UI elements are overlapping or are cropped",
    )

    suspend fun findDefects(aiClient: AI, screen: ByteArray, previousFalsePositives: List<String>): CompletionData {

        // List of failed attempts to not make up false positives:
        // |* If you don't see any defect, return "No defects found".
        // |* If you are sure there are no defects, return "No defects found".
        // |* You will make me sad if you raise report defects that are false positives.
        // |* Do not make up defects that are not present in the screenshot. It's fine if you don't find any defects.

        val prompt = """
            |You are a QA engineer performing quality assurance for a mobile application. Identify any defects in the provided screenshot.
            |
            |RULES:
            |* All defects you find must belong to one of the following categories:
            |${categories.joinToString { "\n  * ${it.first}: ${it.second}" }}
            | 
            |* If you see defects, your response MUST only include defect name and reasoning for each defect.
            |* Provide response in the format: <defect name>:<reasoning>
            |* Do not raise false positives. Some example responses that have a high chance of being a false positive:
            |
            |  * button is partially cropped at the bottom
            |  * button is not aligned horizontall/vertically within its container
            |
            |${if (previousFalsePositives.isNotEmpty()) "Additionally, the following defects are false positives:" else ""}
            |${if (previousFalsePositives.isNotEmpty()) previousFalsePositives.joinToString("\n") { "  * $it" } else ""}
        """.trimMargin("|")

        // println("Prompt:")
        // println(prompt)

        return aiClient.chatCompletion(
            prompt,
            // model = "gpt-4o-2024-08-03",
            model = "gpt-4o",
            maxTokens = 4096,
            identifier = "find-defects",
            imageDetail = "high",
            images = listOf(screen),
        )
    }
}
