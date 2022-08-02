package maestro.cli.runner

import maestro.Maestro
import maestro.orchestra.NoInputException
import maestro.orchestra.SyntaxError
import maestro.orchestra.yaml.YamlCommandReader
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import kotlin.concurrent.thread

object TestRunner {

    fun run(
        maestro: Maestro,
        flowFile: File,
        continuous: Boolean,
    ): Int {
        AnsiConsole.systemInstall()
        println(Ansi.ansi().eraseScreen())

        val view = ResultView()

        fun runFlow(): Int {
            val result = runCatching(view) {
                MaestroCommandRunner.run(maestro, view, flowFile)
            }
            return if (result == true) 0 else 1
        }

        if (!continuous) {
            return runFlow()
        }

        val fileWatcher = FileWatcher()

        var ongoingTest: Thread? = null
        do {
            if (ongoingTest != null) {
                ongoingTest.interrupt()
                ongoingTest.join()
            }

            ongoingTest = thread {
                runFlow()
            }

            val watchFiles = runCatching(view) {
                YamlCommandReader.getWatchFiles(flowFile)
            } ?: listOf(flowFile)

            fileWatcher.waitForChange(watchFiles)
        } while (true)
    }

    private fun <T> runCatching(
        view: ResultView,
        block: () -> T,
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            val message = when (e) {
                is SyntaxError -> "Syntax error"
                is NoInputException -> "No commands in the file"
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