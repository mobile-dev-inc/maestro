package maestro.utils


// singleton to provide a metric manager across maestro code since there's so many singleton objects and passing it around would be a massive change
object MetricsProvider {
    private var metrics: Metrics = NoOpMetrics()

    fun setMetrics(metrics: Metrics) {
        this.metrics = metrics
    }

    fun getInstance(): Metrics {
        return metrics
    }
}

interface Metrics {
    fun <T> measured(name: String, labels: Map<String, String?> = emptyMap(), block: () -> T): T

    // get a metrics object that adds a certain prefix to all metrics
    fun withPrefix(prefix: String): Metrics

    // get a metrics object that adds labels to all metrics
    fun withLabels(labels: Map<String, String>): Metrics
}

class NoOpMetrics : Metrics {
    override fun <T> measured(name: String, labels: Map<String, String?>, block: () -> T): T {
        return block()
    }

    override fun withPrefix(prefix: String): Metrics {
        return this
    }

    override fun withLabels(labels: Map<String, String>): Metrics {
        return this
    }
}
