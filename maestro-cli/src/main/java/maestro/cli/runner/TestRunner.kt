package maestro.cli.runner

import maestro.Maestro
import maestro.orchestra.InvalidInitFlowFile
import maestro.orchestra.NoInputException
import maestro.orchestra.SyntaxError
import maestro.orchestra.yaml.YamlCommandReader
import java.io.File
import kotlin.concurrent.thread

object TestRunner {

    fun runSingle(
        maestro: Maestro,
        flowFile: File,
    ): Int {
        val view = ResultView()
        return runFlow(maestro, view, flowFile)
    }

    fun runContinuous(
        maestro: Maestro,
        flowFile: File,
    ): Nothing {
        val view = ResultView()

        val fileWatcher = FileWatcher()

        var ongoingTest: Thread? = null
        do {
            if (ongoingTest != null) {
                ongoingTest.interrupt()
                ongoingTest.join()
            }

            ongoingTest = thread {
                runFlow(maestro, view, flowFile)
            }

            val watchFiles = runCatching(view) {
                YamlCommandReader.getWatchFiles(flowFile)
            } ?: listOf(flowFile)

            fileWatcher.waitForChange(watchFiles)
        } while (true)
    }

    private fun runFlow(
        maestro: Maestro,
        view: ResultView,
        flowFile: File,
    ): Int {
        val result = runCatching(view) {
            MaestroCommandRunner.run(maestro, view, flowFile)
        }
        return if (result == true) 0 else 1
    }

    private fun <T> runCatching(
        view: ResultView,
        block: () -> T,
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            val message = when (e) {
                is SyntaxError -> "Could not parse Flow file:\n\n${e.message}"
                is NoInputException -> "No commands found in Flow file"
                is InvalidInitFlowFile -> "initFlow file is invalid: ${e.initFlowFile}"
                else -> e.stackTraceToString()
            }

            view.setState(
                ResultView.UiState.Error(
                    message = message
                )
            )
            return null
        }
    }
}