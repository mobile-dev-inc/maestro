package maestro.utils

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol

class MetricsEventListener(
    private val registry: Metrics,
    private val clientName: String,
) : EventListener() {
    private val sample = registry.startTimer()

    override fun callStart(call: Call) {
        registry.counter(
            "http.client.requests",
            mapOf(
                "client" to clientName,
                "method" to call.request().method,
                "url" to call.request().url.host
            )
        ).increment()
    }

    override fun callEnd(call: Call) {
        sample.stop(
            registry.timer(
                "http.client.request.duration",
                mapOf(
                    "client" to clientName,
                    "method" to call.request().method,
                    "url" to call.request().url.host
                )
            )
        )
    }

    override fun callFailed(call: Call, e: IOException) {
        registry.counter(
            "http.client.errors",
            mapOf(
                "client" to clientName,
                "method" to call.request().method,
                "url" to call.request().url.host,
                "exception" to e.javaClass.simpleName
            )
        ).increment()
    }

    class Factory(
        private val registry: Metrics,
        private val clientName: String,
    ) : EventListener.Factory {
        override fun create(call: Call): EventListener =
            MetricsEventListener(registry, clientName)
    }
}

// utility object to build http clients with metrics
object HttpClient {
    fun build(
        name: String,
        connectTimeout: Duration = 10.seconds,
        readTimeout: Duration = 10.seconds,
        writeTimeout: Duration = 10.seconds,
        interceptors: List<Interceptor> = emptyList(),
        networkInterceptors: List<Interceptor> = emptyList(),
        protocols: List<Protocol> = listOf(Protocol.HTTP_2),
        metrics: Metrics = MetricsProvider.getInstance()
    ): OkHttpClient {
        var b = OkHttpClient.Builder()
            .eventListenerFactory(MetricsEventListener.Factory(metrics, name))
            .connectTimeout(connectTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .protocols(protocols)

        b = networkInterceptors.map { b.addNetworkInterceptor(it) }.last()
        b = interceptors.map { b.addInterceptor(it) }.last()

        return b.build()
    }
}
