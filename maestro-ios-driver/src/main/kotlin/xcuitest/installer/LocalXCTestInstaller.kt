package xcuitest.installer

import java.io.File
import okio.buffer
import okio.sink
import okio.source
import org.apache.commons.io.FileUtils
import org.rauschig.jarchivelib.ArchiverFactory
import org.slf4j.LoggerFactory
import util.XCRunnerCLIUtils
import xcuitest.UI_TEST_RUNNER_APP_BUNDLE_ID

class LocalXCTestInstaller(
    private val deviceId: String,
) : XCTestInstaller {

    private val logger = LoggerFactory.getLogger(LocalXCTestInstaller::class.java)

    private val tempDir = "${System.getenv("TMPDIR")}/$deviceId"

    override fun uninstall() {
        logger.info("[Start] Uninstalling XCTest Runner from device $deviceId")
        FileUtils.cleanDirectory(File(tempDir))
        XCRunnerCLIUtils.uninstall(UI_TEST_RUNNER_APP_BUNDLE_ID, deviceId)
        logger.info("[Done] Uninstalling XCTest Runner from device $deviceId")
    }

    override fun install(): File {
        logger.info("[Start] Install XCUITest runner on $deviceId")
        return installXCTestRunner().also {
            logger.info("[Done] Install XCUITest runner on $deviceId")
        }
    }

    private fun installXCTestRunner(): File {
        val processOutput = ProcessBuilder(listOf("xcrun", "simctl", "spawn", deviceId, "launchctl", "list"))
            .start()
            .inputStream.source().buffer().readUtf8()
            .trim()

        logger.info("[Start] Writing xctest run file")
        val tempDir = File(tempDir).apply { mkdir() }
        val xctestRunFile = File("$tempDir/maestro-driver-ios-config.xctestrun")
        writeFileToDestination(XCTEST_RUN_PATH, xctestRunFile)
        logger.info("[Done] Writing xctest run file")

        if (processOutput.contains(UI_TEST_RUNNER_APP_BUNDLE_ID)) {
            logger.info("UI Test runner already running, stopping it")
            uninstall()
        } else {
            logger.info("Not able to find ui test runner app running, going to install now")

            logger.info("[Start] Writing maestro-driver-iosUITests-Runner app")
            extractZipToApp("maestro-driver-iosUITests-Runner", UI_TEST_RUNNER_PATH)
            logger.info("[Done] Writing maestro-driver-iosUITests-Runner app")

            logger.info("[Start] Writing maestro-driver-ios app")
            extractZipToApp("maestro-driver-ios", UI_TEST_HOST_PATH)
            logger.info("[Done] Writing maestro-driver-ios app")
        }

        return xctestRunFile
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
    }
}
