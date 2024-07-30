package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import java.io.File

interface TestSuiteReporter {
    val fileExtension: String?

    fun report(
        summary: TestExecutionSummary,
        out: File,
    )

    companion object {
        val NOOP: TestSuiteReporter = object : TestSuiteReporter {
            override val fileExtension: String? = null

            override fun report(summary: TestExecutionSummary, out: File) {
                // no-op
            }
        }
    }
}
