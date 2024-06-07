package maestro.cli.runner.resultview

object NoopResultView : ResultView {
    override fun setState(state: UiState) { }
}