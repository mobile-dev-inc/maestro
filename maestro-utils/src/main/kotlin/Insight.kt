package maestro.utils

object Insights {

    private val insights = mutableSetOf<Insight>()
    private var onInsightsUpdated: (List<Insight>) -> Unit = {}

    fun report(insight: Insight) {
        insights.add(insight)
        onInsightsUpdated(insights.toList())
    }

    fun list(): Set<Insight> {
        return insights
    }

    fun clear() {
        insights.clear()
    }

    fun onInsightsUpdated(callback: (List<Insight>) -> Unit) {
        onInsightsUpdated = callback
    }
}

data class Insight(val message: String, val level: Level) {
    enum class Level {
        WARNING,
        INSIGHT
    }
}
