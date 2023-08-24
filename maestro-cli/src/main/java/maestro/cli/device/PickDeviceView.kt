package maestro.cli.device

import maestro.cli.CliError
import maestro.cli.model.DeviceStartOptions
import maestro.cli.util.DeviceConfigAndroid
import maestro.cli.util.DeviceConfigIos
import maestro.cli.util.PrintUtils
import org.fusesource.jansi.Ansi.ansi

object PickDeviceView {

    fun showRunOnDevice(device: Device) {
        println("Running on ${device.description}")
    }

    fun pickDeviceToStart(devices: List<Device>): Device {
        printIndexedDevices(devices)

        println("No running devices detected. Choose a device to boot and run on.")
        printEnterNumberPrompt()

        return pickIndex(devices)
    }

    fun requestDeviceOptions(): DeviceStartOptions {
        PrintUtils.message("Please specify device platform (android, ios):")
        val platform = readlnOrNull()?.lowercase()?.let {
            when(it) {
                "android" -> Platform.ANDROID
                "ios" -> Platform.IOS
                else -> throw CliError("Unsupported platform $it")
            }
        } ?: throw CliError("Please specify a platform")

        val version = platform.let {
            when (it) {
                Platform.IOS -> {
                    val versions = DeviceConfigIos.versions.joinToString(separator = ", ")
                    PrintUtils.message("Please specify iOS version ($versions): Press ENTER for default (${DeviceConfigIos.defaultVersion})")
                    readlnOrNull()?.toIntOrNull() ?: DeviceConfigIos.defaultVersion
                }

                Platform.ANDROID -> {
                    val versions = DeviceConfigAndroid.versions.joinToString(separator = ", ")
                    PrintUtils.message("Please specify Android version ($versions): Press ENTER for default (${DeviceConfigAndroid.defaultVersion})")
                    readlnOrNull()?.toIntOrNull() ?: DeviceConfigAndroid.defaultVersion
                }

                else -> throw CliError("Unsupported platform")
            }
        }

        return DeviceStartOptions(
            platform = platform,
            osVersion = version,
            forceCreate = false
        )
    }

    fun pickRunningDevice(devices: List<Device>): Device {
        printIndexedDevices(devices)

        println("Multiple running devices detected. Choose a device to run on.")
        printEnterNumberPrompt()

        return pickIndex(devices)
    }

    private fun <T> pickIndex(data: List<T>): T {
        println()
        while (!Thread.interrupted()) {
            val index = readLine()?.toIntOrNull() ?: 0

            if (index < 1 || index > data.size) {
                printEnterNumberPrompt()
                continue
            }

            return data[index - 1]
        }

        error("Interrupted")
    }

    private fun printEnterNumberPrompt() {
        println()
        println("Enter a number from the list above:")
    }

    private fun printIndexedDevices(devices: List<Device>) {
        val devicesByPlatform = devices.groupBy {
            it.platform
        }

        var index = 0

        devicesByPlatform.forEach { (platform, devices) ->
            println(platform.description)
            println()
            devices.forEach { device ->
                println(
                    ansi()
                        .render("[")
                        .fgCyan()
                        .render("${++index}")
                        .fgDefault()
                        .render("] ${device.description}")
                )
            }
            println()
        }
    }

}