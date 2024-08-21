package maestro.ai

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import maestro.ai.openai.OpenAI
import java.io.File


data class TestCase(
    val screenshot: File,
    val appName: String,
    val prompt: String?,
    val hasDefects: Boolean,
    val index: Int,
)

// TODO(bartekpacia): Improvement ideas:
//  * --only-fail - show only failing test cases
//  * --json – to allow for easy filtering with JQ
//  * --show-prompts – show prompts that were used
//  * Possibility to pass a single screenshot
//  Note: maybe instead of building this purpose CLI program, we can use something
//  purpose-made for this.
/**
 * This is a small helper program to help evaluate LLM results against a directory of screenshots and prompts.
 *
 * ### Usage examples
 *
 * ```console
 * maestro-ai-demo *.png
 * ```
 *
 * ```console
 * maestro-ai-demo foo_bad_1.png
 * ```
 *
 * ### Input format
 *
 * Screenshot name format:
 * - {app_name}_{good|bad}_{screenshot_number}.png
 *
 * A screenshot can optionally have a prompt. To associate a prompt with a screenshot, prompt text file name must have
 * the following format:
 * - {app_name_{good|bad}_{screenshot_number}.txt
 *
 * For example:
 * - foo_bad_1.png
 * - bar_good_2.png
 */
fun main(args: Array<String>) = runBlocking {
    val apiKey = System.getenv("MAESTRO_CLI_AI_KEY")
    require(apiKey != null) { "OpenAI API key is not provided" }

    val testCases = args
        .map { File(it) }
        .map { file ->
            require(!file.isDirectory) { "Provided file is a directory, not a file" }
            require(file.exists()) { "Provided file does not exist" }
            require(file.extension == "png") { "Provided file is not a PNG file" }
            file
        }
        .map { file ->
            val filename = file.nameWithoutExtension
            val parts = filename.split("_")
            require(parts.size == 3) { "Screenshot name is invalid: ${file.name}" }

            val appName = parts[0]
            val index = parts[2].toInt()

            val promptFile = "${file.parent}/${appName}_${parts[1]}_$index.txt"
            println("Prompt file: $promptFile")
            val prompt = File(promptFile)
                .run {
                    if (exists()) {
                        readText()
                    } else {
                        println("There is no prompt for ${file.path}")
                        null
                    }
                }

            TestCase(
                screenshot = file,
                appName = appName,
                hasDefects = parts[1] == "bad",
                index = index,
                prompt = prompt,
            )
        }.toList()

    val aiClient = OpenAI(apiKey)

    // println("---\nRunning ${testCases.size} test cases\n---")

    testCases.forEach { testCase ->
        val bytes = testCase.screenshot.readBytes()

        launch {
            val defects = Prediction.findDefects(
                aiClient = aiClient,
                screen = bytes,
                previousFalsePositives = listOf(),
                assertion = testCase.prompt,
            )

            verify(testCase, defects)
        }
    }
}

private fun verify(testCase: TestCase, defects: List<Defect>) {
    if (testCase.hasDefects) {
        // Check LLM found defects as well (i.e. didn't commit false negative)
        if (defects.isNotEmpty()) {
            println(
                """
                PASS ${testCase.screenshot.name}: ${defects.size} defects found (as expected)
                ${defects.joinToString("\n") { "\t* ${it.category}: ${it.reasoning}" }}
                """.trimIndent()
            )
        } else {
            println("FAIL ${testCase.screenshot.name} false-negative: No defects found but some were expected")
        }

    } else {
        // Check that LLM didn't raise false positives
        if (defects.isEmpty()) {
            println(
                """
                PASS ${testCase.screenshot.name}: No defects found (as expected)
                """.trimIndent()
            )
        } else {
            println(
                """
                FAIL ${testCase.screenshot.name} false-positive: ${defects.size} defects found but none were expected
                ${defects.joinToString("\n") { "\t* ${it.category}: ${it.reasoning}" }}
                """.trimIndent()
            )
        }
    }
}
