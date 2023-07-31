package maestro.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import maestro.utils.Insight
import maestro.utils.Insights
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object InsightService {

    private var insight: Insight? = null

    fun routes(routing: Route) {
        routing.get("/api/banner-message") {
            Insights.onInsightsUpdated {
                insight = it.first { insight -> insight.visibility == Insight.Visibility.VISIBLE }
            }

            if (insight != null) {
                val response = jacksonObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(insight)
                call.respondText(response)
            } else {
                call.respondText("")
            }
        }
    }
}