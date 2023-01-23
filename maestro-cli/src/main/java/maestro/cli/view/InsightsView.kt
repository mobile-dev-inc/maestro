package maestro.cli.view

import maestro.cli.util.PrintUtils
import maestro.utils.maestro.insight.Insight
import maestro.utils.maestro.insight.Insights

object InsightsView {

    fun printInsights() {
        val insights = Insights.list()

        insights
            .sortedBy { it.level }
            .forEach {
                when (it.level) {
                    Insight.Level.WARNING -> PrintUtils.warn("WARNING: ${it.message}")
                }
                println()
            }
    }

}