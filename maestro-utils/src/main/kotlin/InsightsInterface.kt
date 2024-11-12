package maestro.utils

interface InsightsInterface {

    fun report(insight: Insight)

    fun onInsightsUpdated(callback: (Insight) -> Unit)

    fun unregisterListener(callback: (Insight) -> Unit)
}

object NoopInsights: InsightsInterface {

    override fun report(insight: Insight) {
        /* no-op */
    }

    override fun onInsightsUpdated(callback: (Insight) -> Unit) {
        /* no-op */
    }

    override fun unregisterListener(callback: (Insight) -> Unit) {
        /* no-op */
    }

}


data class Insight(
    val message: String,
    val level: Level
) {
    enum class Level {
        WARNING,
        INFO,
        NONE
    }
}