package conductor.cli.continuous

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.DefaultWindowManager
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import com.sun.nio.file.SensitivityWatchEventModifier
import conductor.Conductor
import conductor.cli.continuous.ContinuousTestRunner.ResultView.UiState
import conductor.orchestra.CommandReader
import conductor.orchestra.ConductorCommand
import conductor.orchestra.Orchestra
import conductor.orchestra.yaml.YamlCommandReader
import okio.source
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread
import kotlin.io.path.name

class ContinuousTestRunner(
    private val conductor: Conductor,
    private val testFile: File,
) {

    fun run() {
        val commandReader = resolveCommandReader(testFile)

        conductor.use { conductor ->
            ResultView().withView { view ->
                val executor = Executors.newSingleThreadExecutor()
                var future: Future<*>? = null

                watchTestFile {
                    cancelFuture(future)

                    val commands = try {
                        testFile.source().use {
                            commandReader.readCommands(it)
                        }
                    } catch (e: Exception) {
                        val message = when {
                            e is CommandReader.SyntaxError -> "Syntax error"
                            e is CommandReader.NoInputException -> "No commands in the file"
                            e.message != null -> e.message!!
                            else -> "Failed to read commands"
                        }

                        view.setState(
                            UiState.Error(
                                message = message
                            )
                        )
                        return@watchTestFile
                    }

                    future = executor.submit {
                        runCommands(
                            conductor,
                            commands,
                            view
                        )
                    }
                }
            }
        }
    }

    private fun cancelFuture(future: Future<*>?) {
        try {
            future?.cancel(true)
            future?.get()
        } catch (ignored: Exception) {
            // Do nothing
        }
    }

    private fun runCommands(
        conductor: Conductor,
        commands: List<ConductorCommand>,
        view: ResultView,
    ) {
        val indexToStatus = Array(commands.size) { CommandStatus.PENDING }

        fun refreshUi() {
            view.setState(
                UiState.Running(
                    commands = commands
                        .mapIndexed { idx, command ->
                            CommandState(
                                command = command,
                                status = indexToStatus[idx]
                            )
                        },
                )
            )
        }

        refreshUi()

        Orchestra(
            conductor,
            onCommandStart = { idx, _ ->
                indexToStatus[idx] = CommandStatus.RUNNING
                refreshUi()
            },
            onCommandComplete = { idx, _ ->
                indexToStatus[idx] = CommandStatus.COMPLETED
                refreshUi()
            },
            onCommandFailed = { idx, _, _ ->
                indexToStatus[idx] = CommandStatus.FAILED
                refreshUi()
            },
        ).executeCommands(commands)
    }

    private fun watchTestFile(block: () -> Unit) {
        val watchService = FileSystems.getDefault().newWatchService()

        val pathKey = testFile
            .parentFile
            .toPath()
            .register(
                watchService,
                arrayOf(
                    StandardWatchEventKinds.ENTRY_MODIFY,
                ),
                SensitivityWatchEventModifier.HIGH
            )

        try {
            block()

            while (!Thread.interrupted()) {
                val watchKey = watchService.take()

                watchKey.pollEvents().forEach {
                    val modifiedPath = it.context() as Path
                    if (modifiedPath.name == testFile.name) {
                        block()
                    }
                }

                if (!watchKey.reset()) {
                    watchKey.cancel()
                    watchService.close()
                    break
                }
            }
        } catch (ignored: InterruptedException) {
            // Do nothing
        }

        pathKey.cancel()
    }

    private fun resolveCommandReader(file: File): CommandReader {
        if (file.extension == "yaml") {
            return YamlCommandReader()
        }

        throw IllegalArgumentException(
            "Test file extension is not supported: ${file.extension}"
        )
    }

    private data class CommandState(
        val status: CommandStatus,
        val command: ConductorCommand,
    )

    private enum class CommandStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
    }

    private class ResultView {

        private val panel: Panel = Panel()
        private lateinit var gui: MultiWindowTextGUI

        fun withView(block: (ResultView) -> Unit) {
            val terminal: Terminal = DefaultTerminalFactory().createTerminal()
            val screen = TerminalScreen(terminal)
            screen.startScreen()

            panel.layoutManager = LinearLayout(Direction.VERTICAL)

            panel.addComponent(Label("Starting up"))

            val window = BasicWindow()
            window.component = panel

            gui = MultiWindowTextGUI(screen, DefaultWindowManager(), EmptySpace(TextColor.ANSI.BLACK))

            val eventThread = thread {
                block(this)
            }

            gui.addWindowAndWait(window)

            eventThread.interrupt()
        }

        fun setState(state: UiState) {
            panel.removeAllComponents()

            when (state) {
                is UiState.Running -> renderRunningState(state)
                is UiState.Error -> renderErrorState(state)
            }

            gui.updateScreen()
        }

        private fun renderErrorState(state: UiState.Error) {
            panel.addComponent(
                Label(
                    state.message
                ).setForegroundColor(TextColor.ANSI.RED)
            )
        }

        private fun renderRunningState(state: UiState.Running) {
            panel.addComponent(
                Panel(
                    GridLayout(2)
                ).apply {
                    state.commands.forEach {
                        addComponent(
                            Label("${it.status}")
                                .setForegroundColor(inferColor(it.status))
                        )
                        addComponent(
                            Label(
                                it.command.description()
                            )
                        )
                    }
                }
            )
        }

        private fun inferColor(status: CommandStatus): TextColor {
            return when (status) {
                CommandStatus.PENDING -> TextColor.ANSI.BLACK
                CommandStatus.RUNNING -> TextColor.ANSI.YELLOW_BRIGHT
                CommandStatus.COMPLETED -> TextColor.ANSI.GREEN
                CommandStatus.FAILED -> TextColor.ANSI.RED
            }
        }

        sealed class UiState {

            data class Error(val message: String) : UiState()

            data class Running(
                val commands: List<CommandState>,
            ) : UiState()

        }
    }

}
