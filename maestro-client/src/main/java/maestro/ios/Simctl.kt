package maestro.ios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import maestro.MaestroTimer
import ios.commands.CommandLineUtils
import okio.buffer
import okio.source
import java.util.concurrent.TimeUnit

object Simctl {
    fun listApps(): Set<String> {
        val process = ProcessBuilder("bash", "-c", "xcrun simctl listapps booted | plutil -convert json - -o -").start()

        val json = String(process.inputStream.readBytes())

        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val appsMap: Map<String, Any> = gson.fromJson(json, type)

        return appsMap.keys
    }

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
            val process = ProcessBuilder(
                "bash",
                "-c",
                "xcrun simctl spawn booted launchctl print system | grep $bundleId | awk '/$bundleId/ {print \$3}'"
            ).start()

            val processOutput = process.inputStream.source().buffer().readUtf8().trim()
            process.waitFor(3000, TimeUnit.MILLISECONDS)

            processOutput.contains(bundleId)
        }
    }

    fun runXcTestWithoutBuild(deviceId: String, xcTestRunFilePath: String) {
//        CommandLineUtils.runCommand(
//            "xcodebuild test-without-building -xctestrun $xcTestRunFilePath -destination id=$deviceId",
//            waitForCompletion = false
//        )
    }

    fun uninstall(bundleId: String) {
        CommandLineUtils.runCommand("xcrun simctl uninstall booted $bundleId")
    }
}
