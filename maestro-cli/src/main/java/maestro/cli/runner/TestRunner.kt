package maestro.cli.runner

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.onFailure
import maestro.Maestro
import maestro.cli.device.Device
import maestro.cli.report.FlowAIOutput
import maestro.cli.report.FlowDebugOutput
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

object TestRunner {

    private val logger = LoggerFactory.getLogger(TestRunner::class.java)

    fun runSingle(
        maestro: Maestro,
        device: Device?,
        flowFile: File,
        env: Map<String, String>,
        resultView: ResultView,
        debugOutputPath: Path
    ): Int {
        val debugOutput = FlowDebugOutput()
        var aiOutput = FlowAIOutput(
            flowName = flowFile.nameWithoutExtension,
            flowFile = flowFile,
        )

        val result = runCatching(resultView, maestro) {
            val commands = YamlCommandReader.readCommands(flowFile.toPath())
                .withEnv(env)

            YamlCommandReader.getConfig(commands)?.name?.let {
                aiOutput = aiOutput.copy(flowName = it)
            }

            MaestroCommandRunner.runCommands(
                maestro,
                device,
                resultView,
                commands,
                debugOutput,
                aiOutput,
            )
        }

        TestDebugReporter.saveFlow(
            flowName = flowFile.name,
            debugOutput = debugOutput,
            path = debugOutputPath,
        )
        TestDebugReporter.saveSuggestions(
            outputs = listOf(aiOutput),
            path = debugOutputPath,
        )

        if (debugOutput.exception != null) PrintUtils.err("${debugOutput.exception?.message}")

        return if (result.get()?.flowSuccess == true) 0 else 1
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
                                FlowDebugOutput(),
                                // TODO: bartekpacia - make AI outputs work in continuous mode
                                FlowAIOutput(
                                    flowName = "TODO",
                                    flowFile = flowFile,
                                ),
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
