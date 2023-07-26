package maestro.test

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.google.common.net.HttpHeaders
import com.google.common.truth.Truth.assertThat
import maestro.js.GraalJsEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraalJsEngineTest : JsEngineTest() {

    @BeforeEach
    fun setUp() {
        engine = GraalJsEngine()
    }

    @Test
    fun `Allow redefinitions of variables`() {
        engine.evaluateScript("const foo = null")
        engine.evaluateScript("const foo = null")
    }

    @Test
    fun `You can't share variables between scopes`() {
        engine.evaluateScript("const foo = 'foo'")
        val result = engine.evaluateScript("foo").toString()
        assertThat(result).contains("undefined")
    }

    @Test
    fun `Backslash and newline are supported`() {
        engine.setCopiedText("\\\n")
        engine.putEnv("FOO", "\\\n")

        val result = engine.evaluateScript("maestro.copiedText + FOO").toString()

        assertThat(result).isEqualTo("\\\n\\\n")
    }

    @Test
    fun `parseInt returns an int representation`() {
        val result = engine.evaluateScript("parseInt('1')").toString()
        assertThat(result).isEqualTo("1")
    }

    @Test
    fun `HTTP - Make GET request and check response body and headers `(wiremockInfo: WireMockRuntimeInfo) {
        // Given
        val port = wiremockInfo.httpPort
        val body =
            """
                {
                    "message": "GET Endpoint"
                }
            """.trimIndent()

        val testHeader = "testHeader"
        val response = WireMock.aResponse().withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withHeader(testHeader, "first")
            .withHeader(testHeader, "second")
            .withBody(body);

        WireMock.stubFor(
            WireMock.get("/json").willReturn(response)
        )

        val script = """
            var response = http.get('http://localhost:$port/json');
            
            //check body
            var message = json(response.body).message;
            
            // check headers
            var contentType = response.headers['content-type'];
            var testHeader = response.headers['testheader'];
            String(message + String(" ") + contentType[0] + String(" ") + testHeader[0] + String(" ") + testHeader[1]);
        """.trimIndent()

        // When
        val result = engine.evaluateScript(script)

        // Then
        assertThat(result.toString()).isEqualTo("GET Endpoint application/json first second")
    }
}