package conductor.cli.runner

import org.fusesource.jansi.Ansi

class ResultView {

    fun setState(state: UiState) {
        when (state) {
            is UiState.Running -> renderRunningState(state)
            is UiState.Error -> renderErrorState(state)
        }
    }

    private fun renderErrorState(state: UiState.Error) {
        clearScreen()

        println(
            Ansi.ansi()
                .fgRed()
                .render(state.message)
        )
    }

    private fun renderRunningState(state: UiState.Running) {
        clearScreen()

        val statusColumnWidth = 13

        state.commands.forEach {
            println(
                Ansi.ansi()
                    .fg(inferColor(it.status))
                    .render(it.status.name)
                    .render(String(CharArray(statusColumnWidth - it.status.name.length) { ' ' }))
                    .fgDefault()
                    .render(it.command.description())
            )
        }
    }

    private fun clearScreen() {
        println(
            Ansi.ansi()
                .eraseScreen()
                .cursor(0, 0)
        )
    }

    private fun inferColor(status: CommandStatus): Int {
        return when (status) {
            CommandStatus.PENDING -> Ansi.Color.WHITE.value()
            CommandStatus.RUNNING -> Ansi.Color.YELLOW.value()
            CommandStatus.COMPLETED -> Ansi.Color.GREEN.value()
            CommandStatus.FAILED -> Ansi.Color.RED.value()
        }
    }

    sealed class UiState {

        data class Error(val message: String) : UiState()

        data class Running(
            val commands: List<CommandState>,
        ) : UiState()

    }
}