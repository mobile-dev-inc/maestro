package util

class IOSDeviceIdentifierResolver {

    fun getUDID(deviceId: String): String {
        // Return the deviceId as-is (it should already be the UDID)
        return deviceId
    }

    fun listDevices(): List<String> {
        return runCatching {
            val output = CommandLineUtils.runCommandAndReturnOutput(
                listOf("idevice_id", "-l")
            )
            output.lines().filter { it.isNotBlank() }
        }.getOrElse { emptyList() }
    }
}
