package maestro.cli.device

object PickDeviceInteractor {

    fun pickDevice(): Device {
        return pickDeviceInternal()
            .let { pickedDevice ->
                var result = pickedDevice

                if (!result.connected) {
                    result = DeviceService.startDevice(pickedDevice)
                }

                DeviceService.prepareDevice(result)

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
        val availableDevices = DeviceService.listAvailableDevices()

        return PickDeviceView.pickDeviceToStart(availableDevices)
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
