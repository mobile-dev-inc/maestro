package maestro.cli.device

import dadb.Dadb
import maestro.cli.CliError
import maestro.cli.model.DeviceCreateException.*
import maestro.cli.util.EnvUtils
import maestro.utils.MaestroTimer
import okio.buffer
import okio.source
import org.slf4j.LoggerFactory
import util.LocalSimulatorUtils
import util.LocalSimulatorUtils.SimctlError
import util.SimctlList
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object DeviceService {
    val logger = LoggerFactory.getLogger(DeviceService::class.java)
    fun startDevice(device: Device.AvailableForLaunch): Device.Connected {
        when (device.platform) {
            Platform.IOS -> {
                try {
                    LocalSimulatorUtils.launchSimulator(device.modelId)
                    LocalSimulatorUtils.awaitLaunch(device.modelId)
                } catch (e: SimctlError) {
                    logger.error("Failed to launch simulator", e)
                    throw CliError(e.message)
                }

                return Device.Connected(
                    instanceId = device.modelId,
                    description = device.description,
                    platform = device.platform,
                )
            }

            Platform.ANDROID -> {
                val emulatorBinary = requireEmulatorBinary()

                ProcessBuilder(
                    emulatorBinary.absolutePath,
                    "-avd",
                    device.modelId,
                    "-netdelay",
                    "none",
                    "-netspeed",
                    "full"
                ).start()

                val dadb = MaestroTimer.withTimeout(30000) {
                    try {
                        Dadb.discover()
                    } catch (ignored: Exception) {
                        Thread.sleep(100)
                        null
                    }
                } ?: throw CliError("Unable to start device: ${device.modelId}")

                return Device.Connected(
                    instanceId = dadb.toString(),
                    description = device.description,
                    platform = device.platform,
                )
            }

            Platform.WEB -> {
                return Device.Connected(
                    instanceId = "",
                    description = "Selenium Chromium driver",
                    platform = device.platform,
                )
            }
        }
    }

    fun listConnectedDevices(): List<Device.Connected> {
        return listDevices()
            .filterIsInstance(Device.Connected::class.java)
    }

    fun listAvailableForLaunchDevices(): List<Device.AvailableForLaunch> {
        return listDevices()
            .filterIsInstance(Device.AvailableForLaunch::class.java)
    }

    private fun listDevices(): List<Device> {
        return listAndroidDevices() + listIOSDevices() + listWebDevices()
    }

    private fun listWebDevices(): List<Device> {
        return listOf(
            Device.AvailableForLaunch(
                platform = Platform.WEB,
                description = "Chromium Desktop Browser (Experimental)",
                modelId = "chromium"
            )
        )
    }

    private fun listAndroidDevices(): List<Device> {
        val connected = Dadb.list()
            .map {
                Device.Connected(
                    instanceId = it.toString(),
                    description = it.toString(),
                    platform = Platform.ANDROID,
                )
            }

        // Note that there is a possibility that AVD is actually already connected and is present in
        // connectedDevices.
        val avds = try {
            val emulatorBinary = requireEmulatorBinary()
            ProcessBuilder(emulatorBinary.absolutePath, "-list-avds")
                .start()
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines
                        .map {
                            Device.AvailableForLaunch(
                                modelId = it,
                                description = it,
                                platform = Platform.ANDROID,
                            )
                        }
                        .toList()
                }
        } catch (ignored: Exception) {
            emptyList()
        }

        return connected + avds
    }

    private fun listIOSDevices(): List<Device> {
        val simctlList = try {
            LocalSimulatorUtils.list()
        } catch (ignored: Exception) {
            return emptyList()
        }

        val runtimeNameByIdentifier = simctlList
            .runtimes
            .associate { it.identifier to it.name }

        return simctlList
            .devices
            .flatMap { runtime ->
                runtime.value
                    .filter { it.isAvailable }
                    .map { device(runtimeNameByIdentifier, runtime, it) }
            }
    }

    private fun device(
        runtimeNameByIdentifier: Map<String, String>,
        runtime: Map.Entry<String, List<SimctlList.Device>>,
        device: SimctlList.Device
    ): Device {
        val runtimeName = runtimeNameByIdentifier[runtime.key] ?: "Unknown runtime"
        val description = "${device.name} - $runtimeName - ${device.udid}"

        return if (device.state == "Booted") {
            Device.Connected(
                instanceId = device.udid,
                description = description,
                platform = Platform.IOS,
            )
        } else {
            Device.AvailableForLaunch(
                modelId = device.udid,
                description = description,
                platform = Platform.IOS,
            )
        }
    }


    /**
     * Creates an iOS simulator
     *
     * @param deviceName Any name
     * @param device Simulator type as specified by Apple i.e. iPhone-11
     * @param os OS runtime name as specified by Apple i.e. iOS-16-2
     */
    fun createIosDevice(deviceName: String, device: String, os: String): UUID {
        val command = listOf(
            "xcrun",
            "simctl",
            "create",
            deviceName,
            "com.apple.CoreSimulator.SimDeviceType.$device",
            "com.apple.CoreSimulator.SimRuntime.$os"
        )

        val process = ProcessBuilder(*command.toTypedArray()).start()

        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            throw TimeoutException()
        }

        if (process.exitValue() != 0) {
            val processOutput = process.errorStream
                .source()
                .buffer()
                .readUtf8()

            if (processOutput.contains("Invalid runtime")) {
                throw InvalidRuntimeException()
            } else if (processOutput.contains("Invalid device type")) {
                throw InvalidDeviceTypeException()
            } else throw UnableToCreateException(processOutput)

        } else {

            val output = String(process.inputStream.readBytes()).trim()

            return try {
                UUID.fromString(output)
            } catch (ignore: IllegalArgumentException) {
                throw UnableToCreateException("Unable to create device. No UUID was generated")
            }

        }
    }

    /**
     * Creates an Android emulator
     *
     * @param deviceName Any device name
     * @param device Device type as specified by the Android SDK i.e. "pixel_6"
     * @param systemImage Full system package i.e "system-images;android-28;google_apis;x86_64"
     * @param tag google apis or playstore tag i.e. google_apis or google_apis_playstore
     * @param abi x86_64, x86 etc..
     */
    fun createAndroidDevice(deviceName: String, device: String, systemImage: String, tag: String, abi: String): String {
        val avd = requireAvdManagerBinary()
        val command = listOf(
            avd.absolutePath,
            "create", "avd",
            "--name", deviceName,
            "--package", systemImage,
            "--tag", tag,
            "--abi", abi,
            "--device", device,
            "--force"
        )

        val process = ProcessBuilder(*command.toTypedArray()).start()

        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            throw TimeoutException()
        }

        if (process.exitValue() != 0) {
            val processOutput = process.errorStream
                .source()
                .buffer()
                .readUtf8()

            throw UnableToCreateException(processOutput)
        }

        return deviceName
    }


    /**
     * @return true is Android system image is already installed
     */
    fun isAndroidSystemImageInstalled(image: String): Boolean {
        try {
            val command = listOf(
                requireSdkManagerBinary().absolutePath,
                "--list_installed"
            )

            val process = ProcessBuilder(*command.toTypedArray()).start()
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                throw TimeoutException()
            }

            if (process.exitValue() == 0) {
                val output = String(process.inputStream.readBytes()).trim()

                return output.contains(image)
            }

        } catch (e: Exception) {
            logger.error("Unable to detect if SDK package is installed", e)
        }

        return false
    }

    fun installAndroidSystemImage(image: String): Boolean {
        try {
            val command = listOf(
                requireSdkManagerBinary().absolutePath,
                image
            )

            val process = ProcessBuilder(*command.toTypedArray())
                .inheritIO()
                .start()
            if (!process.waitFor(120, TimeUnit.MINUTES)) {
                throw TimeoutException()
            }

            if (process.exitValue() == 0) {
                val output = String(process.inputStream.readBytes()).trim()

                return output.contains(image)
            }

        } catch (e: Exception) {
            logger.error("Unable to install if SDK package is installed", e)
        }

        return false
    }

    fun getAndroidSystemImageInstallCommand(pkg: String): String {
        return listOf(
            requireSdkManagerBinary().absolutePath,
            "\"$pkg\""
        ).joinToString(separator = " ")
    }

    private fun requireEmulatorBinary(): File {
        val androidHome = EnvUtils.androidHome()
            ?: throw CliError("Could not detect Android home environment variable is not set. Ensure that either ANDROID_HOME or ANDROID_SDK_ROOT is set.")
        val firstChoice = File(androidHome, "emulator/emulator")
        val secondChoice = File(androidHome, "tools/emulator")
        return firstChoice.takeIf { it.exists() } ?: secondChoice.takeIf { it.exists() }
        ?: throw CliError("Could not find emulator binary at either of the following paths:\n$firstChoice\n$secondChoice")
    }

    private fun requireAvdManagerBinary(): File {
        val androidHome = EnvUtils.androidHome()
            ?: throw CliError("Could not detect Android home environment variable is not set. Ensure that either ANDROID_HOME or ANDROID_SDK_ROOT is set.")
        val firstChoice = File(androidHome, "cmdline-tools/latest/bin/avdmanager")
        val secondChoice = File(androidHome, "tools/bin/avdmanager")
        return firstChoice.takeIf { it.exists() } ?: secondChoice.takeIf { it.exists() }
        ?: throw CliError("Could not find avdmanager binary at either of the following paths:\n$firstChoice\n$secondChoice")
    }

    private fun requireSdkManagerBinary(): File {
        val androidHome = EnvUtils.androidHome()
            ?: throw CliError("Could not detect Android home environment variable is not set. Ensure that either ANDROID_HOME or ANDROID_SDK_ROOT is set.")
        val firstChoice = File(androidHome, "cmdline-tools/latest/bin/sdkmanager")
        val secondChoice = File(androidHome, "tools/bin/sdkmanager")
        return firstChoice.takeIf { it.exists() } ?: secondChoice.takeIf { it.exists() }
        ?: throw CliError("Could not find avdmanager binary at either of the following paths:\n$firstChoice\n$secondChoice")
    }
}
