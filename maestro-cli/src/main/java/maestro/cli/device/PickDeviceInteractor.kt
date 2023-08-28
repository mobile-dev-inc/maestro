package maestro.cli.device

import maestro.cli.CliError
import maestro.cli.util.PrintUtils

object PickDeviceInteractor {

    fun pickDevice(deviceId: String? = null): Device.Connected {
        if (deviceId != null) {
            return DeviceService.listConnectedDevices()
                .find {
                    it.instanceId == deviceId
                } ?: throw CliError("Device with id $deviceId is not connected")
        }

        return pickDeviceInternal()
            .let { pickedDevice ->
                var result: Device = pickedDevice

                if (result is Device.AvailableForLaunch) {
                    when (result.platform) {
                        Platform.ANDROID -> PrintUtils.message("Launching Android emulator...")
                        Platform.IOS -> PrintUtils.message("Launching iOS simulator...")
                        Platform.WEB -> PrintUtils.message("Launching ${result.description}")
                    }

                    result = DeviceService.startDevice(result)
                }

                if (result !is Device.Connected) {
                    error("Device $result is not connected")
                }

                result
            }
    }

    private fun pickDeviceInternal(): Device {
        val connectedDevices = DeviceService.listConnectedDevices()

        if (connectedDevices.size == 1) {
            val device = connectedDevices[0]

            PickDeviceView.showRunOnDevice(device)

            return device
        }

        if (connectedDevices.isEmpty()) {
            return startDevice()
        }

        return pickRunningDevice(connectedDevices)
    }

    private fun startDevice(): Device {
        PrintUtils.message("No devices found. Would you like to start a new one? y/n")
        val proceed = readlnOrNull()?.lowercase()?.trim()
        if (proceed == "no" || proceed == "n") {
            throw CliError("Please either start a device manually or via running maestro start-device to proceed running your flows")
        }

        val options = PickDeviceView.requestDeviceOptions()
        if (options.platform == Platform.WEB) {
            return Device.AvailableForLaunch(
                platform = Platform.WEB,
                description = "Chromium Desktop Browser (Experimental)",
                modelId = "chromium"
            )
        }

        return DeviceCreateUtil.getOrCreateDevice(options.platform, options.osVersion, options.forceCreate)
    }

    private fun pickRunningDevice(devices: List<Device>): Device {
        return PickDeviceView.pickRunningDevice(devices)
    }

}

fun main() {
    println(PickDeviceInteractor.pickDevice())

    println("Ready")
    while (!Thread.interrupted()) {
        Thread.sleep(1000)
    }
}
