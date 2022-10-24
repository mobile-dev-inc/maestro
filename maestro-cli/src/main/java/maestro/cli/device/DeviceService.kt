package maestro.cli.device

import com.github.michaelbull.result.expect
import dadb.Dadb
import io.grpc.ManagedChannelBuilder
import ios.idb.IdbIOSDevice
import maestro.MaestroTimer
import maestro.cli.CliError
import maestro.cli.device.ios.Simctl
import maestro.cli.util.EnvUtils
import java.io.File

object DeviceService {

    fun startDevice(device: Device): Device {
        when (device.platform) {
            Device.Platform.IOS -> {
                Simctl.launchSimulator(device.id)
                Simctl.awaitLaunch(device.id)
                return device
            }
            Device.Platform.ANDROID -> {
                val androidHome = EnvUtils.androidHome()
                    ?: throw CliError("ANDROID_HOME environment variable is not set. Unable to find 'ANDROID_HOME/tools/emulator' executable.")

                ProcessBuilder(
                    File(androidHome, "tools/emulator").absolutePath,
                    "@${device.id}",
                ).start()

                val dadb = MaestroTimer.withTimeout(30000) {
                    try {
                        Dadb.discover()
                    } catch (ignored: Exception) {
                        Thread.sleep(100)
                        null
                    }
                } ?: throw CliError("Unable to start device: ${device.id}")

                return device.copy(id = dadb.toString())
            }
        }
    }

    fun prepareDevice(device: Device) {
        if (device.platform == Device.Platform.IOS) {
            killIdbCompanion()
            startIdbCompanion(device)
        }
    }

    private fun startIdbCompanion(device: Device) {
        ProcessBuilder("idb_companion", "--udid", device.id)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        val iosDevice = IdbIOSDevice(
            ManagedChannelBuilder.forAddress("localhost", 10882)
                .usePlaintext()
                .build()
        )
        try {
            // Try to connect to the device repeatedly
            MaestroTimer.withTimeout(10000) {
                try {
                    iosDevice.deviceInfo().expect {}
                } catch (ignored: Exception) {
                    // Ignore
                    null
                }
            } ?: error("idb_companion did not start in time")
        } finally {
            iosDevice.close()
        }
    }

    private fun killIdbCompanion() {
        ProcessBuilder("killall", "idb_companion")
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .waitFor()
    }

    fun listConnectedDevices(): List<Device> {
        return listAvailableDevices()
            .filter { it.connected }
    }

    fun listAvailableDevices(): List<Device> {
        return listAndroidDevices() + listIOSDevices()
    }

    private fun listAndroidDevices(): List<Device> {
        val connectedDevices = Dadb.list()
            .map {
                Device(
                    id = it.toString(),
                    description = it.toString(),
                    platform = Device.Platform.ANDROID,
                    connected = true
                )
            }

        // Note that there is a possibility that AVD is actually already connected and is present in
        // connectedDevices.
        val avds = try {
            ProcessBuilder("emulator", "-list-avds")
                .start()
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines
                        .map {
                            Device(
                                id = it,
                                description = it,
                                platform = Device.Platform.ANDROID,
                                connected = false
                            )
                        }
                        .toList()
                }
        } catch (ignored: Exception) {
            emptyList()
        }

        return connectedDevices + avds
    }

    private fun listIOSDevices(): List<Device> {
        val simctlList = try {
            Simctl.list()
        } catch (ignored: Exception) {
            return emptyList()
        }

        val runtimes = simctlList
            .runtimes

        return runtimes
            .flatMap { runtime ->
                runtime.supportedDeviceTypes
                    .flatMap { supportedDeviceType ->
                        simctlList.devices
                            .values
                            .flatten()
                            .filter {
                                it.isAvailable &&
                                    it.deviceTypeIdentifier == supportedDeviceType.identifier
                            }
                    }
                    .map { device ->
                        Device(
                            id = device.udid,
                            description = "${device.name} - ${runtime.name} - ${device.udid}",
                            platform = Device.Platform.IOS,
                            connected = device.state == "Booted"
                        )
                    }
            }
    }

}
