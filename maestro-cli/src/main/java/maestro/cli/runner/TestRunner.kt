package maestro.cli.runner

import com.github.michaelbull.result.*
import maestro.Maestro
import maestro.cli.device.Device
import maestro.cli.model.FlowStatus
import maestro.cli.model.TestExecutionSummary
import maestro.cli.report.FlowDebugMetadata
import maestro.cli.report.TestDebugReporter
import maestro.cli.runner.resultview.AnsiResultView
import maestro.cli.runner.resultview.ResultView
import maestro.cli.runner.resultview.UiState
import maestro.cli.util.PrintUtils
import maestro.cli.view.ErrorViewUtils
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroInitFlow
import maestro.orchestra.OrchestraAppState
import maestro.orchestra.util.Env.withEnv
import maestro.orchestra.yaml.YamlCommandReader
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

object TestRunner {

    private val logger = LoggerFactory.getLogger(TestRunner::class.java)

    fun runSingle(
        maestro: Maestro,
        device: Device?,
        flowFile: File,
        env: Map<String, String>,
        resultView: ResultView,
        debugOutputPath: Path
    ): TestExecutionSummary.FlowResult {

        // debug
        val debug = FlowDebugMetadata()

        val result: Result<MaestroCommandRunner.Result, Exception>
        val time = measureTimeMillis {
            result = runCatching(resultView, maestro) {
                val commands = YamlCommandReader.readCommands(flowFile.toPath())
                    .withEnv(env)
                MaestroCommandRunner.runCommands(
                    maestro,
                    device,
                    resultView,
                    commands,
                    debug
                )
            }
        }.milliseconds

        TestDebugReporter.saveFlow(flowFile.name, debug, debugOutputPath)
        if (debug.exception != null) PrintUtils.err("${debug.exception?.message}")

        val flowStatus = if (result.get()?.flowSuccess == true) FlowStatus.SUCCESS else FlowStatus.ERROR

        return TestExecutionSummary.FlowResult(
            name = flowFile.nameWithoutExtension,
            fileName = flowFile.nameWithoutExtension,
            status = flowStatus,
            failure = if (flowStatus == FlowStatus.ERROR) {
                TestExecutionSummary.Failure(
                    message = result.getError()?.message ?: debug.exception?.message ?: "Unknown error",
                )
            } else null,
            duration = time,
        )
    }

    fun runContinuous(
        maestro: Maestro,
        device: Device?,
        flowFile: File,
        env: Map<String, String>,
    ): Nothing {
        val resultView = AnsiResultView("> Press [ENTER] to restart the Flow\n\n")

        val fileWatcher = FileWatcher()

        var previousCommands: List<MaestroCommand>? = null
        var previousInitFlow: MaestroInitFlow? = null
        var previousResult: MaestroCommandRunner.Result? = null

        var ongoingTest: Thread? = null
        do {
            val watchFiles = runCatching(resultView, maestro) {
                ongoingTest?.apply {
                    interrupt()
                    join()
                }

                val commands = YamlCommandReader.readCommands(flowFile.toPath())
                    .withEnv(env)
                val initFlow = getInitFlow(commands)

                // Restart the flow if anything has changed
                if (commands != previousCommands || initFlow != previousInitFlow) {
                    ongoingTest = thread {
                        // If previous init flow was successful and there were no changes to the init flow,
                        // then reuse cached app state (and skip the init commands)
                        val cachedAppState: OrchestraAppState? = if (initFlow == previousInitFlow) {
                            previousResult?.cachedAppState
                        } else {
                            null
                        }

                        previousCommands = commands
                        previousInitFlow = initFlow

                        previousResult = runCatching(resultView, maestro) {
                            MaestroCommandRunner.runCommands(
                                maestro,
                                device,
                                resultView,
                                commands,
                                FlowDebugMetadata()
                            )
                        }.get()
                    }
                }

                YamlCommandReader.getWatchFiles(flowFile.toPath())
            }
                .onFailure {
                    previousCommands = null
                }
                .getOr(listOf(flowFile.toPath()))

            if (CliWatcher.waitForFileChangeOrEnter(fileWatcher, watchFiles) == CliWatcher.SignalType.ENTER) {
                // On ENTER force re-run of flow even if commands have not changed
                previousCommands = null
            }
        } while (true)
    }

    private fun getInitFlow(commands: List<MaestroCommand>): MaestroInitFlow? {
        return YamlCommandReader.getConfig(commands)?.initFlow
    }

    private fun <T> runCatching(
        view: ResultView,
        maestro: Maestro,
        block: () -> T,
    ): Result<T, Exception> {
        return try {
            Ok(block())
        } catch (e: Exception) {
            logger.error("Failed to run flow", e)
            val message = ErrorViewUtils.exceptionToMessage(e)

            if (!maestro.isShutDown()) {
                view.setState(
                    UiState.Error(
                        message = message
                    )
                )
            }
            return Err(e)
        }
    }
}
