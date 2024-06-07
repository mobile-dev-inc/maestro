package maestro.cli.runner

import maestro.Maestro
import maestro.cli.CliError
import maestro.cli.device.Device
import maestro.cli.model.FlowStatus
import maestro.cli.model.TestExecutionSummary
import maestro.cli.report.TestSuiteReporter
import maestro.cli.runner.resultview.ResultView
import maestro.cli.util.PrintUtils
import maestro.cli.view.TestSuiteStatusView
import maestro.cli.view.TestSuiteStatusView.TestSuiteViewModel
import maestro.orchestra.workspace.WorkspaceExecutionPlanner
import okio.Sink
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class TestSuiteInteractor(
    private val maestro: Maestro,
    private val device: Device? = null,
    private val reporter: TestSuiteReporter,
) {

    private val logger = LoggerFactory.getLogger(TestSuiteInteractor::class.java)

    fun runTestSuite(
        executionPlan: WorkspaceExecutionPlanner.ExecutionPlan,
        reportOut: Sink?,
        view: ResultView,
        env: Map<String, String>,
        debugOutputPath: Path
    ): TestExecutionSummary {
        if (executionPlan.flowsToRun.isEmpty() && executionPlan.sequence?.flows?.isEmpty() == true) {
            throw CliError("No flows returned from the tag filter used")
        }

        val flowResults = mutableListOf<TestExecutionSummary.FlowResult>()

        PrintUtils.message("Waiting for flows to complete...")
        println()

        var passed = true

        // first run sequence of flows if present
        val flowSequence = executionPlan.sequence
        for (flow in flowSequence?.flows ?: emptyList()) {
            val result = TestRunner.runSingle(maestro, device, flow.toFile(),env, view, debugOutputPath)
            flowResults.add(result)

            if (result.status == FlowStatus.ERROR) {
                passed = false
                if (executionPlan.sequence?.continueOnFailure != true) {
                    PrintUtils.message("Flow ${result.name} failed and continueOnFailure is set to false, aborting running sequential Flows")
                    println()
                    break
                }
            }
        }

        // proceed to run all other Flows
        executionPlan.flowsToRun.forEach { flow ->
            val result = TestRunner.runSingle(maestro, device, flow.toFile(), env, view, debugOutputPath)

            if (result.status == FlowStatus.ERROR) {
                passed = false
            }
            flowResults.add(result)
        }


        val suiteDuration = flowResults.sumOf { it.duration?.inWholeSeconds ?: 0 }.seconds

        TestSuiteStatusView.showSuiteResult(
            TestSuiteViewModel(
                status = if (passed) FlowStatus.SUCCESS else FlowStatus.ERROR,
                duration = suiteDuration,
                flows = flowResults
                    .map {
                        TestSuiteViewModel.FlowResult(
                            name = it.name,
                            status = it.status,
                            duration = it.duration,
                        )
                    },
            )
        )

        val summary = TestExecutionSummary(
            passed = passed,
            deviceName = device?.description,
            suites = listOf(
                TestExecutionSummary.SuiteResult(
                    passed = passed,
                    flows = flowResults,
                    duration = suiteDuration
                )
            )
        )

        if (reportOut != null) {
            reporter.report(
                summary,
                reportOut,
            )
        }

        return summary
    }

}
