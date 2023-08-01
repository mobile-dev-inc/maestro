package maestro.utils

object Insights {

    private var onInsightsUpdated: (Insight) -> Unit = {}
    private var insight: Insight = Insight("", Insight.Level.NONE)

    fun report(insight: Insight) {
        this.insight = insight
        onInsightsUpdated(insight)
    }

    fun onInsightsUpdated(callback: (Insight) -> Unit) {
        onInsightsUpdated = callback
    }
}

data class Insight(val message: String, val level: Level) {
    enum class Level {
        WARNING,
        INFO,
        NONE
    }
}
