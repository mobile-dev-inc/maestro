package maestro

import maestro.mockserver.MockEvent

data class HeadersAndValueMatches(
    val headerName: String,
    val headerValue: String,
)

data class OutgoingRequestRules(
    val url: String,
)

object AssertOutgoingRequestService {

    fun assert(events: List<MockEvent>, rules: OutgoingRequestRules): List<MockEvent> {
        val eventsMatching = events.filter { it.path == rules.url }
        println("from ${events.size} events, ${eventsMatching.size} match the url ${rules.url}")
        return eventsMatching
    }

}