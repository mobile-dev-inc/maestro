package xcuitest.starter

import java.io.File
import org.slf4j.LoggerFactory
import util.XCRunnerCLIUtils
import xcuitest.UI_TEST_RUNNER_APP_BUNDLE_ID
import xcuitest.XCTestClient
import xcuitest.installer.LocalXCTestInstaller

private val logger = LoggerFactory.getLogger(LocalXCTestInstaller::class.java)

class LocalXCTestStarter(
    private val deviceId: String,
    private val host: String = "[::1]",
    private val enableXCTestOutputFileLogging: Boolean,
    private val defaultPort: Int,
) : XCTestStarter {

    override fun start(xcTestRunFile: File): XCTestClient {
        logger.info("[Start] Running XcUITest with `xcodebuild test-without-building`")
        XCRunnerCLIUtils.runXcTestWithoutBuild(
            deviceId = deviceId,
            xcTestRunFilePath = xcTestRunFile.absolutePath,
            port = defaultPort,
            enableXCTestOutputFileLogging = enableXCTestOutputFileLogging,
        )
        logger.info("[Done] Running XcUITest with `xcodebuild test-without-building`")
        return XCTestClient(host, defaultPort)
    }

    override fun stop() {
        killXCTestRunnerProcess()
    }

    private fun killXCTestRunnerProcess() {
        logger.trace("Will attempt to stop all alive XCTest Runner processes before uninstalling")

        val pid = XCRunnerCLIUtils.pidForApp(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId)
        logger.trace("Killing XCTest Runner process with the `kill` command")
        ProcessBuilder(listOf("kill", pid.toString()))
            .start()
            .waitFor()

        logger.trace("All XCTest Runner processes were stopped")
    }

    override fun isRunning(): Boolean {
        val appAlive = XCRunnerCLIUtils.isAppAlive(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId)
        return appAlive
    }


//    private fun ensureOpen(): Boolean {
//        val timeout = 120_000L
//        logger.info("ensureOpen(): Will spend $timeout ms waiting for the channel to become alive")
//        val result = MaestroTimer.retryUntilTrue(timeout, 200, onException = {
//            logger.error("ensureOpen() failed with exception: $it")
//        }) { isChannelAlive() }
//        logger.info("ensureOpen() finished, is channel alive?: $result")
//        return result
//    }
//
//    private fun xcTestDriverStatusCheck(): Boolean {
//        logger.info("[Start] Perform XCUITest driver status check on $deviceId")
//        fun xctestAPIBuilder(pathSegment: String): HttpUrl.Builder {
//            return HttpUrl.Builder()
//                .scheme("http")
//                .host("[::1]")
//                .addPathSegment(pathSegment)
//                .port(defaultPort)
//        }
//
//        val url = xctestAPIBuilder("status")
//            .build()
//
//        val request = Request.Builder()
//            .get()
//            .url(url)
//            .build()
//
//        val okHttpClient = OkHttpClient.Builder()
//            .connectTimeout(40, TimeUnit.SECONDS)
//            .readTimeout(100, TimeUnit.SECONDS)
//            .build()
//
//        val checkSuccessful = try {
//            okHttpClient.newCall(request).execute().use {
//                logger.info("[Done] Perform XCUITest driver status check on $deviceId")
//                it.isSuccessful
//            }
//        } catch (ignore: IOException) {
//            logger.info("[Failed] Perform XCUITest driver status check on $deviceId, exception: $ignore")
//            false
//        }
//
//        return checkSuccessful
//    }

}