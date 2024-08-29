package maestro.cli.model

import maestro.MaestroException
import maestro.cli.report.CommandDebugMetadata
import maestro.cli.report.ScreenshotDebugMetadata
import maestro.orchestra.MaestroCommand
import java.util.*
import kotlin.time.Duration

data class TestExecutionSummary(
    val passed: Boolean,
    val suites: List<SuiteResult>,
    val passedCount: Int? = null,
    val totalTests: Int? = null,
) {

    data class SuiteResult(
        val passed: Boolean,
        val flows: List<FlowResult>,
        val duration: Duration? = null,
        val deviceName: String? = null,
    )

    data class FlowResult(
        val name: String,
        val fileName: String?,
        val status: FlowStatus,
        val failure: Failure? = null,
        val duration: Duration? = null,
    )

    data class Failure(
        val message: String,
        val commands: IdentityHashMap<MaestroCommand, CommandDebugMetadata>? = null,
        val screenshots: MutableList<ScreenshotDebugMetadata>? = null,
        var exception: MaestroException? = null
    )
}
