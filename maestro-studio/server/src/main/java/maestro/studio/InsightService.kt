package maestro.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import maestro.studio.BannerMessage.*
import maestro.utils.Insight
import maestro.utils.Insights
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object InsightService {

    private var currentInsights: List<Insight>? = null

    fun routes(routing: Route) {
        registerInsightUpdateCallback()

        routing.get("/api/banner-message") {

            if (currentInsights != null) {
                if (!currentInsights.isNullOrEmpty()) {
                    currentInsights?.forEach {
                        val bannerMessage = BannerMessage(
                            it.message,
                            Level.valueOf(it.level.toString()).toString().lowercase()
                        )
                        val response = jacksonObjectMapper()
                            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(bannerMessage)
                        call.respondText(response)
                    }
                } else {
                    val response = jacksonObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(BannerMessage("", Level.NONE.toString().lowercase()))
                    call.respondText(response)
                }
            } else {
                call.respondText("")
            }
        }
    }

    private fun registerInsightUpdateCallback() {
        Insights.onInsightsUpdated {
            currentInsights = it
        }
    }
}

data class BannerMessage(val message: String, val level: String) {
    enum class Level {
        WARNING,
        NONE,
        INFO
    }
}