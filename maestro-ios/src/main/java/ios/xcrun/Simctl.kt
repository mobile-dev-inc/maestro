package ios.xcrun

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import maestro.utils.MaestroTimer
import util.CommandLineUtils
import util.CommandLineUtils.runCommand
import java.io.File

object Simctl {

    data class SimctlError(override val message: String): Throwable(message)

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
                    .flatten()
                    .find { it.udid == deviceId }
                    ?.state == "Booted"
            ) true else null
        } ?: throw SimctlError("Device $deviceId did not boot in time")
    }

    fun awaitShutdown(deviceId: String) {
        MaestroTimer.withTimeout(30000) {
            if (list()
                    .devices
                    .values
                    .flatten()
                    .find { it.udid == deviceId }
                    ?.state != "Booted"
            ) true else null
        } ?: throw SimctlError("Device $deviceId did not boot in time")
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

    fun reboot(
        deviceId: String,
    ) {
        CommandLineUtils.runCommand(
            listOf(
                "xcrun",
                "simctl",
                "shutdown",
                deviceId
            ),
            waitForCompletion = true
        )
        awaitShutdown(deviceId)

        CommandLineUtils.runCommand(
            listOf(
                "xcrun",
                "simctl",
                "boot",
                deviceId
            ),
            waitForCompletion = true
        )
        awaitLaunch(deviceId)
    }

    fun addTrustedCertificate(
        deviceId: String,
        certificate: File,
    ) {
        CommandLineUtils.runCommand(
            listOf(
                "xcrun",
                "simctl",
                "keychain",
                deviceId,
                "add-root-cert",
                certificate.absolutePath,
            ),
            waitForCompletion = true
        )

        reboot(deviceId)
    }

    private enum class Permission {
        ALLOW,
        REVOKE,
        RESET
    }

    private val allPermissions = listOf(
        "calendar",
        "camera",
        "contacts",
        "faceid",
        "health",
        "homekit",
        "location",
        "medialibrary",
        "microphone",
        "motion",
        "notifications",
        "photos",
        "reminders",
        "siri",
        "speech",
        "userTracking",
    )

    private fun setPermissionsTo(deviceId: String, bundleId: String, permissions: Map<String, Permission>) {
        val setPermissions = permissions
            .mapValues { (_, value) ->
                when (value) {
                    Permission.ALLOW -> "YES"
                    Permission.REVOKE -> "NO"
                    Permission.RESET -> "unset"
                }
            }
            .map { (key, value) -> "$key=$value" }
            .joinToString { "," }

        runCommand(
            listOf(
                "applesimutils",
                "--byId",
                deviceId,
                "--bundle",
                bundleId,
                "--setPermissions",
                setPermissions
            )
        )
    }

    fun resetPermissions(deviceId: String, bundleId: String) {
        val applesimutilsInstalled = ProcessBuilder("which", "applesimutils").start().exitValue()
        if (applesimutilsInstalled != 0) {
            throw IllegalStateException("applesimutils is not installed. See https://github.com/wix/AppleSimulatorUtils for instructions")
        }

        setPermissionsTo(deviceId, bundleId, allPermissions.associateWith { Permission.RESET })
    }
}
