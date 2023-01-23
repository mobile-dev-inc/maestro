package maestro.utils.maestro.insight

object Insights {

    private val insights = mutableSetOf<Insight>()

    fun report(insight: Insight) {
        insights.add(insight)
    }

    fun list(): Set<Insight> {
        return insights
    }

    fun clear() {
        insights.clear()
    }

}