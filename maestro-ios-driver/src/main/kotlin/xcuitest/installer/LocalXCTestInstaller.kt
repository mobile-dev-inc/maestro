package xcuitest.installer

import logger.Logger
import maestro.utils.MaestroTimer
import maestro.utils.SocketUtils
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import okio.source
import org.apache.commons.io.FileUtils
import org.rauschig.jarchivelib.ArchiverFactory
import util.XCRunnerCLIUtils
import xcuitest.XCTestClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class LocalXCTestInstaller(
    private val logger: Logger,
    private val deviceId: String,
    private val host: String = "[::1]",
    defaultPort: Int? = null,
) : XCTestInstaller {
    // Set this flag to allow using a test runner started from Xcode
    // When this flag is set, maestro will not install, run, stop or remove the xctest runner.
    // Make sure to launch the test runner from Xcode whenever maestro needs it.
    private val useXcodeTestRunner = !System.getenv("USE_XCODE_TEST_RUNNER").isNullOrEmpty()
    private val tempDir = "${System.getenv("TMPDIR")}/$deviceId"

    private var xcTestProcess: Process? = null

    private val lazyPort by lazy { SocketUtils.nextFreePort(9800, 9900) }
    private val port = defaultPort ?: lazyPort

    override fun uninstall(sourceIntent: SourceIntent) {
        if (useXcodeTestRunner) {
            return
        }

        stop(sourceIntent)

        //logger.info("[Start] Uninstall XCUITest runner on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
        //XCRunnerCLIUtils.uninstall(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId)
        //logger.info("[Done] Uninstall XCUITest runner on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
    }

    private fun stop(sourceIntent: SourceIntent) {
        val intent = sourceIntent.intent
        val source = sourceIntent.source
        //logger.info("[Start] Stop XCUITest runner on $deviceId with intent of $intent from $source")
//        if (xcTestProcess?.isAlive == true) {
//            xcTestProcess?.destroy()
//        }

//        val pid = XCRunnerCLIUtils.pidForApp(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId)
//        if (pid != null) {
//            ProcessBuilder(listOf("kill", pid.toString()))
//                .start()
//                .waitFor()
//        }
        //logger.info("[Done] Stop XCUITest runner on $deviceId with intent of $intent from $source")
    }

    override fun start(sourceIntent: SourceIntent): XCTestClient? {
        if (useXcodeTestRunner) {
            repeat(20) {
                if (ensureOpen()) {
                    return XCTestClient(host, port)
                }
                logger.info("==> Start XCTest runner to continue flow on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
                Thread.sleep(500)
            }
            throw IllegalStateException("XCTest was not started manually on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
        }

        stop(sourceIntent)

        repeat(3) { i ->
            logger.info("[Start] Install XCUITest runner on $deviceId with intent of $sourceIntent.intent from ${sourceIntent.source}")
            startXCTestRunner(sourceIntent)
            logger.info("[Done] Install XCUITest runner on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")

            logger.info("[Start] Ensure XCUITest runner is running on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
            if (ensureOpen()) {
                logger.info("[Done] Ensure XCUITest runner is running on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
                return XCTestClient(host, port)
            } else {
                logger.info("[Failed] Ensure XCUITest runner is running on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
                logger.info("[Retry] Retrying setup() ${i}th time on $deviceId with intent of ${sourceIntent.intent} from ${sourceIntent.source}")
            }
        }
        return null
    }

    override fun isChannelAlive(): Boolean {
        // TODO: remove this isAlive and check because this might
        return try {
            return xcTestDriverStatusCheck().use { it.isSuccessful }
        } catch (exception: IOException) {
            // ignore
            false
        }
    }

    private fun ensureOpen(): Boolean {
        return MaestroTimer.retryUntilTrue(10_000, 200) {
            try {
                XCRunnerCLIUtils.isAppAlive(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId) &&
                    xcTestDriverStatusCheck().use { it.isSuccessful }
            } catch (ignore: IOException) {
                false
            }
        }
    }

    fun xcTestDriverStatusCheck(): Response {
        fun xctestAPIBuilder(pathSegment: String): HttpUrl.Builder {
            return HttpUrl.Builder()
                .scheme("http")
                .host("[::1]")
                .addPathSegment(pathSegment)
                .port(port)
        }

        val url = xctestAPIBuilder("status")
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .build()
        return okHttpClient.newCall(request).execute()
    }

    private fun startXCTestRunner(sourceIntent: SourceIntent) {
        val processOutput = ProcessBuilder(listOf("xcrun", "simctl", "spawn", deviceId, "launchctl", "list"))
            .start()
            .inputStream.source().buffer().readUtf8()
            .trim()

        val intent = sourceIntent.intent
        val source = sourceIntent.source
        logger.info("[Start] Writing xctest run file for $deviceId with intent of $intent from $source")
        val tempDir = File(tempDir).apply { mkdir() }
        val xctestRunFile = File("$tempDir/maestro-driver-ios-config.xctestrun")
        writeFileToDestination(XCTEST_RUN_PATH, xctestRunFile)
        logger.info("[Done] Writing xctest run file for $deviceId with intent of $intent from $source")

        if (isChannelAlive()) {
            logger.info("UI Test runner already running on $deviceId, stopping it with intent of $intent from $source")
            //stop(sourceIntent)
            return
        } else {
            logger.info("Not able to find ui test runner app running, going to install now on $deviceId with intent of $intent from $source")

            logger.info("[Start] Writing maestro-driver-iosUITests-Runner app for $deviceId with intent of $intent from $source")
            extractZipToApp("maestro-driver-iosUITests-Runner", UI_TEST_RUNNER_PATH)
            logger.info("[Done] Writing maestro-driver-iosUITests-Runner app for $deviceId with intent of $intent from $source")

            logger.info("[Start] Writing maestro-driver-ios app for $deviceId with intent of $intent from $source")
            extractZipToApp("maestro-driver-ios", UI_TEST_HOST_PATH)
            logger.info("[Done] Writing maestro-driver-ios app for $deviceId with intent of $intent from $source")
        }

        logger.info("[Start] Running XcUITest with xcode build command for $deviceId with intent of $intent from $source")

        xcTestProcess = XCRunnerCLIUtils.runXcTestWithoutBuild(
            deviceId,
            xctestRunFile.absolutePath,
            port
        )
        logger.info("[Done] Running XcUITest with xcode build command $deviceId with intent of $intent from $source")
    }

    override fun close(sourceIntent: SourceIntent) {
        if (useXcodeTestRunner) {
            return
        }

        val intent = Intent.DRIVER_CLOSE
        logger.info("[Start] Cleaning up the ui test runner files for $deviceId with intent of $intent from ${sourceIntent.source}")
        FileUtils.cleanDirectory(File(tempDir))
        uninstall(sourceIntent)
        logger.info("[Done] Cleaning up the ui test runner files for $deviceId with intent of $intent from ${sourceIntent.source}")
    }

    private fun extractZipToApp(appFileName: String, srcAppPath: String) {
        val appFile = File("$tempDir/Debug-iphonesimulator").apply { mkdir() }
        val appZip = File("$tempDir/$appFileName.zip")

        writeFileToDestination(srcAppPath, appZip)
        ArchiverFactory.createArchiver(appZip).apply {
            extract(appZip, appFile)
        }
    }

    private fun writeFileToDestination(srcPath: String, destFile: File) {
        LocalXCTestInstaller::class.java.getResourceAsStream(srcPath)?.let {
            val bufferedSink = destFile.sink().buffer()
            bufferedSink.writeAll(it.source())
            bufferedSink.flush()
        }
    }

    companion object {
        private const val UI_TEST_RUNNER_PATH = "/maestro-driver-iosUITests-Runner.zip"
        private const val XCTEST_RUN_PATH = "/maestro-driver-ios-config.xctestrun"
        private const val UI_TEST_HOST_PATH = "/maestro-driver-ios.zip"
        private const val UI_TEST_RUNNER_APP_BUNDLE_ID = "dev.mobile.maestro-driver-iosUITests.xctrunner"
    }
}
