package maestro

import com.google.common.truth.Truth.assertThat
import maestro.mockserver.MockEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class AssertOutgoingRequestServiceTest {

    companion object {
        val sessionId: UUID = UUID.randomUUID()
    }

    @Test
    fun `test assert with url rule`() {
        val events = events()

        val rules = OutgoingRequestRules(url = ".*api.company.com\\/[^\\/]+\\/endpoint")
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(2)
        assertThat(result.first().path).isEqualTo("http://api.company.com/bla/endpoint")
        assertThat(result[1].path).isEqualTo("https://api.company.com/foo/endpoint")
    }

    @Test
    fun `test assert with http method rule`() {
        val events = events()

        val rules = OutgoingRequestRules(assertHttpMethod = "GET")
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(2)

        assertThat(result.first().path).isEqualTo("https://api.company.com/foo/endpoint")
        assertThat(result[1].path).isEqualTo("another-path")
    }

    @Test
    fun `test assert with header is present rule`() {
        val events = events()

        val rules = OutgoingRequestRules(assertHeaderIsPresent = "cOnTent-tyPE")
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(1)

        assertThat(result.first().path).isEqualTo("https://api.company.com/foo/endpoint")
    }

    @Test
    fun `test assert with header and value rule`() {
        val events = events()

        val rules = OutgoingRequestRules(
            assertHeadersAndValues = listOf(
                HeadersAndValueMatches("content-type", "application-json"),
                HeadersAndValueMatches("cache-control", "no")
            )
        )
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(1)

        assertThat(result.first().path).isEqualTo("https://api.company.com/foo/endpoint")
    }

    @Test
    fun `test assert with body contains rule`() {
        val events = events()

        val rules = OutgoingRequestRules(
            assertRequestBodyContains = "\"name\":\"felipe\""
        )
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(1)

        assertThat(result.first().statusCode).isEqualTo(400)
    }

    @Test
    fun `test assert with all rules`() {
        val events = events()

        val rules = OutgoingRequestRules(
            url = ".*api.company.com\\/[^\\/]+\\/endpoint",
            assertHttpMethod = "GET",
            assertHeaderIsPresent = "cOnTent-tyPE",
            assertHeadersAndValues = listOf(
                HeadersAndValueMatches("content-type", "application-json"),
                HeadersAndValueMatches("cache-control", "no")
            )
        )
        val result = AssertOutgoingRequestService.assert(events, rules)
        assertThat(result.size).isEqualTo(1)
    }

    private fun events(): List<MockEvent> {
        return listOf(
            MockEvent(
                timestamp = "2021-05-18T12:00:00.000Z",
                path = "http://api.company.com/bla/endpoint",
                matched = true,
                response = "",
                statusCode = 201,
                sessionId = sessionId,
                projectId = UUID.randomUUID(),
                method = "POTS",
                headers = mapOf(
                    "cache-control" to "no",
                )
            ),

            MockEvent(
                timestamp = "2021-05-18T12:00:00.000Z",
                path = "https://api.company.com/foo/endpoint",
                matched = true,
                response = "",
                statusCode = 200,
                sessionId = sessionId,
                projectId = UUID.randomUUID(),
                method = "GET",
                headers = mapOf(
                    "content-type" to "application-json",
                    "cache-control" to "no",
                )
            ),

            MockEvent(
                timestamp = "2021-05-18T12:00:00.000Z",
                path = "another-path",
                matched = true,
                response = "",
                statusCode = 400,
                sessionId = UUID.randomUUID(),
                projectId = UUID.randomUUID(),
                method = "GET",
                bodyAsString = "{\"foo\":{\"name\":\"felipe\"}}",
                headers = mapOf(
                    "content-length" to "10",
                )
            )
        )
    }
}