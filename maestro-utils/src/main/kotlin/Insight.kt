package maestro.utils

object Insights: InsightsInterface {

    private var insight: Insight = Insight("", Insight.Level.NONE)
    private val listeners = mutableListOf<(Insight) -> Unit>()

    override fun report(insight: Insight) {
        Insights.insight = insight
        listeners.forEach { it.invoke(insight) }
    }

    override fun onInsightsUpdated(callback: (Insight) -> Unit) {
        listeners.add(callback)
    }

    override fun unregisterListener(callback: (Insight) -> Unit) {
        listeners.remove(callback)
    }
}
