package maestro.cli.model

sealed class DeviceCreateException(message: String? = null) : IllegalStateException(message) {

    class InvalidRuntimeException : DeviceCreateException()

    class InvalidDeviceTypeException: DeviceCreateException()

    class UnableToCreateException(override val message: String): DeviceCreateException(message)
}