package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import okio.Sink

class NoOpReporter : TestSuiteReporter {
    override fun report(summary: TestExecutionSummary, out: Sink) {
        // No operation performed here
    }
}
