package util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import maestro.utils.MaestroTimer
import net.harawata.appdirs.AppDirsFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

object XCRunnerCLIUtils {

    private const val APP_NAME = "maestro"
    private const val APP_AUTHOR = "mobile_dev"
    private const val LOG_DIR_DATE_FORMAT = "yyyy-MM-dd_HHmmss"
    private const val MAX_COUNT_XCTEST_LOGS = 5

    private val dateFormatter by lazy { DateTimeFormatter.ofPattern(LOG_DIR_DATE_FORMAT) }
    private val logger = LoggerFactory.getLogger(XCRunnerCLIUtils::class.java)
    private val logDirectory by lazy {
        val parentName = AppDirsFactory.getInstance().getUserLogDir(APP_NAME, null, APP_AUTHOR)
        val logsDirectory = File(parentName, "xctest_runner_logs")
        File(parentName).apply {
            if (!exists()) mkdir()

            if (!logsDirectory.exists()) logsDirectory.mkdir()

            val existing = logsDirectory.listFiles() ?: emptyArray()
            val toDelete = existing.sortedByDescending { it.name }
            val count = toDelete.size
            if (count > MAX_COUNT_XCTEST_LOGS) toDelete.forEach { it.deleteRecursively() }
        }
        logsDirectory
    }

    fun listApps(deviceId: String): Set<String> {
        val process = Runtime.getRuntime().exec(arrayOf("bash", "-c", "xcrun simctl listapps $deviceId | plutil -convert json - -o -"))

        val json = String(process.inputStream.readBytes())

        if (json.isEmpty()) return emptySet()

        val mapper = jacksonObjectMapper()
        val appsMap = mapper.readValue(json, Map::class.java) as Map<String, Any>

        return appsMap.keys
    }

    fun setProxy(host: String, port: Int) {
        ProcessBuilder("networksetup", "-setwebproxy", "Wi-Fi", host, port.toString())
            .redirectErrorStream(true)
            .start()
            .waitFor()
        ProcessBuilder("networksetup", "-setsecurewebproxy", "Wi-Fi", host, port.toString())
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }

    fun resetProxy() {
        ProcessBuilder("networksetup", "-setwebproxystate", "Wi-Fi", "off")
            .redirectErrorStream(true)
            .start()
            .waitFor()
        ProcessBuilder("networksetup", "-setsecurewebproxystate", "Wi-Fi", "off")
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }

    fun uninstall(bundleId: String, deviceId: String) {
        CommandLineUtils.runCommand(
            listOf(
                "xcrun",
                "simctl",
                "uninstall",
                deviceId,
                bundleId
            )
        )
    }

    private fun runningApps(deviceId: String): Map<String, Int?> {
        val process = ProcessBuilder(
            "xcrun",
            "simctl",
            "spawn",
            deviceId,
            "launchctl",
            "list"
        ).start()

        val processOutput = process.inputStream
            .bufferedReader()
            .readLines()

        process.waitFor(3000, TimeUnit.MILLISECONDS)

        return processOutput
            .asSequence()
            .drop(1)
            .toList()
            .map { line -> line.split("\\s+".toRegex()) }
            .filter { parts -> parts.count() <= 3 }
            .associate { parts -> parts[2] to parts[0].toIntOrNull() }
            .mapKeys { (key, _) ->
                // Fixes issue with iOS 14.0 where process names are sometimes prefixed with "UIKitApplication:"
                // and ending with [stuff]
                key
                    .substringBefore("[")
                    .replace("UIKitApplication:", "")
            }
    }

    fun isAppAlive(bundleId: String, deviceId: String): Boolean {
        return runningApps(deviceId)
            .containsKey(bundleId)
    }

    fun pidForApp(bundleId: String, deviceId: String): Int? {
        return runningApps(deviceId)[bundleId]
    }

    fun runXcTestWithoutBuild(deviceId: String, xcTestRunFilePath: String, port: Int, enableXCTestOutputFileLogging: Boolean = false): Process {
        val date = dateFormatter.format(LocalDateTime.now())
        val outputFile = if (enableXCTestOutputFileLogging) {
            File(logDirectory, "xctest_runner_$date.log")
        } else {
            null
        }
        val logOutputDir = Files.createTempDirectory("maestro_xctestrunner_xcodebuild_output")
        return CommandLineUtils.runCommand(
            listOf(
                "xcodebuild",
                "test-without-building",
                "-xctestrun",
                xcTestRunFilePath,
                "-destination",
                "id=$deviceId",
                "-derivedDataPath",
                logOutputDir.absolutePathString()
            ),
            waitForCompletion = false,
            outputFile = outputFile,
            params = mapOf("TEST_RUNNER_PORT" to port.toString())
        )
    }

    fun killProcessesByPattern(pattern: String) {
        try {
            // Execute ps and grep command using ProcessBuilder
            val psGrepProcess = ProcessBuilder("/bin/sh", "-c", "ps aux | grep '$pattern' | grep -v grep | awk '{print \$2}'")
                .start()

            val reader = psGrepProcess.inputStream.bufferedReader()
            val pids = reader.readLines().filter { it.isNotEmpty() }

            // Check if any PIDs were found
            if (pids.isNotEmpty()) {
                pids.forEach { pid ->
                    try {
                        // Kill each process found by PID
                        ProcessBuilder("kill", pid).start().also { it.waitFor() }
                        logger.info("Killed process with PID: $pid")
                    } catch (e: Exception) {
                        logger.error("Error killing process $pid with patter $pattern", e)
                    }
                }
            } else {
                logger.info("No processes found matching pattern: $pattern")
            }
        } catch (e: Exception) {
            logger.error("An error occurred while trying to kill processes by pattern '$pattern': ${e.message}", e)
        }
    }
}
