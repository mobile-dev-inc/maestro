package maestro.ios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import maestro.MaestroTimer
import maestro.utils.CommandLineUtils
import okio.buffer
import okio.source

object Simctl {

    fun list(): SimctlList {
        val command = listOf("xcrun", "simctl", "list", "-j")

        val process = ProcessBuilder(command).start()
        val json = String(process.inputStream.readBytes())

        return jacksonObjectMapper().readValue(json)
    }

    fun awaitLaunch(deviceId: String) {
        MaestroTimer.withTimeout(30000) {
            if (list()
                    .devices
                    .values
                    .flatMap { it }
                    .find { it.udid == deviceId }
                    ?.state == "Booted"
            ) true else null
        } ?: throw RuntimeException("Device $deviceId did not boot in time")
    }

    fun launchSimulator(deviceId: String) {
        CommandLineUtils.runCommand("xcrun simctl boot $deviceId")

        var exceptionToThrow: Exception? = null

        // Up to 10 iterations => max wait time of 1 second
        repeat(10) {
            try {
                CommandLineUtils.runCommand("open -a /Applications/Xcode.app/Contents/Developer/Applications/Simulator.app --args -CurrentDeviceUDID $deviceId")
                return
            } catch (e: Exception) {
                exceptionToThrow = e
                Thread.sleep(100)
            }
        }

        exceptionToThrow?.let { throw it }
    }

    fun ensureAppAlive(bundleId: String) {
        MaestroTimer.retryUntilTrue(timeoutMs = 4000, delayMs = 300) {
            val processOutput = ProcessBuilder(
                "bash",
                "-c",
                "xcrun simctl spawn booted launchctl print system | grep $bundleId | awk '/$bundleId/ {print \$3}'"
            ).start().inputStream.source().buffer().readUtf8().trim()

            processOutput.contains(bundleId)
        }
    }

    fun runXcTestWithoutBuild(deviceId: String, xcTestRunFilePath: String) {
        CommandLineUtils.runCommand(
            "xcodebuild test-without-building -xctestrun $xcTestRunFilePath -destination id=$deviceId",
            waitForCompletion = false
        )
    }
}
