package maestro.xctestdriver

import com.google.common.truth.Truth.assertThat
import maestro.debuglog.IOSDriverLogger
import maestro.ios.MockXCTestInstaller
import maestro.utils.enqueueBadResponses
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import xcuitest.XCTestClient
import xcuitest.XCTestDriverClient
import java.net.InetAddress

class XCTestDriverClientTest {

    @ParameterizedTest
    @MethodSource("enqueueTimes")
    fun `it should fail with proper message in case 3 retries fail`(enqueueTime: Int) {
        // given
        val mockWebServer = MockWebServer()
        enqueueBadResponses(mockWebServer, enqueueTime)
        mockWebServer.start(InetAddress.getByName( "localhost"), 22087)
        val httpUrl = mockWebServer.url("/deviceInfo")

        // when
        val simulator = MockXCTestInstaller.Simulator(
            installationRetryCount = 0,
            shouldInstall = false
        )
        val mockXCTestInstaller = MockXCTestInstaller(simulator)
        val xcTestDriverClient = XCTestDriverClient(
            mockXCTestInstaller,
            IOSDriverLogger(XCTestDriverClient::class.java),
            XCTestClient("localhost", 22087)
        )
        val response = xcTestDriverClient.deviceInfo(httpUrl)

        // then
        assertThat(response.message).contains(
            "A timeout occurred while waiting for a response from the XCUITest server."
        )
        mockXCTestInstaller.assertInstallationRetries(3)
        mockWebServer.shutdown()
    }


    companion object {
        @JvmStatic
        fun enqueueTimes(): List<Int> {
            return listOf(0, 1, 2, 3)
        }
    }
}