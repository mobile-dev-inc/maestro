package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import java.io.File

interface TestSuiteReporter {
    val fileExtension: String?

    /**
     * Writes the report for [summary] to [out] in the format specified by the implementation.
     */
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
