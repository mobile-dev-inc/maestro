package maestro.studio

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import maestro.utils.Insights
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object InsightService {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun routes(routing: Route) {
        routing.get("/api/insights") {
            val deferredInsight = CompletableDeferred<String?>()
            scope.launch(Dispatchers.IO) {
                val insight = fetchInsights()
                deferredInsight.complete(insight)
            }
            val insight = withTimeoutOrNull(3_000) {
                deferredInsight.await()
            }

            if (insight.isNullOrEmpty()) {
                call.respondText("")
            } else {
                call.respondText(insight)
            }
        }
    }

    private suspend fun fetchInsights(): String {
        return suspendCoroutine { continuation ->
            Insights.onInsightsUpdated { insights ->
                continuation.resume(insights.map { insight -> insight.message }.first())
            }
        }
    }
}