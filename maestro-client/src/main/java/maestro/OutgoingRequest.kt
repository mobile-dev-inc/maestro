package maestro

import maestro.mockserver.MockEvent
import maestro.utils.StringUtils.toRegexSafe

data class OutgoingRequestRules(
    val path: String,
    val headersPresent: List<String> = emptyList(),
    val headersAndValues: Map<String, String> = emptyMap(),
    val httpMethodIs: String? = null,
    val requestBodyContains: String? = null,
)

object AssertOutgoingRequestService {

    private val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)

    fun assert(events: List<MockEvent>, rules: OutgoingRequestRules): List<MockEvent> {
        val eventsFilteredByUrl = events.filter { e -> e.path == rules.path || e.path.matches(rules.path.toRegexSafe(REGEX_OPTIONS)) }

        val eventsFilteredByHttpMethod = rules.httpMethodIs?.let { httpMethod ->
            eventsFilteredByUrl.filter { e -> e.method == httpMethod }
        } ?: eventsFilteredByUrl

        val eventsFilteredByHeader = rules.headersPresent.fold(eventsFilteredByHttpMethod) { eventsList, header ->
            eventsList.filter { e -> e.headers?.containsKey(header.lowercase()) == true }
        }

        val eventsFilteredByHeadersAndValues = rules.headersAndValues.entries.fold(eventsFilteredByHeader) { eventsList, (header, value) ->
            eventsList.filter { e -> e.headers?.get(header.lowercase()) == value }
        }

        val eventsMatching = rules.requestBodyContains?.let { requestBody ->
            eventsFilteredByHeadersAndValues.filter { e -> e.bodyAsString?.contains(requestBody) == true }
        } ?: eventsFilteredByHeadersAndValues

        return eventsMatching
    }

}