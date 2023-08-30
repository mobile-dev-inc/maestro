package maestro.cli.util

internal object DeviceConfigIos {

    val device: String = "iPhone-11"
    val versions = listOf(15, 16)
    val runtimes = mapOf(
        15 to "iOS-15-5",
        16 to "iOS-16-2"
    )

    val defaultVersion = 15

    fun generateDeviceName(version: Int) = "Maestro_iPhone11_$version"
}

data class DeviceConfigAndroid(
    val deviceName: String,
    val device: String,
    val tag: String,
    val systemImage: String,
    val abi: String
) {
    companion object {
        val versions = listOf(33, 31, 30, 29, 28)
        val defaultVersion = 30

        fun createConfig(version: Int, architecture: MACOS_ARCHITECTURE): DeviceConfigAndroid {
            val name = "Maestro_Pixel6_$version"
            val device = "pixel_6"
            val tag = "google_apis"
            val systemImage = when (architecture) {
                MACOS_ARCHITECTURE.x86_64 -> "x86_64"
                MACOS_ARCHITECTURE.ARM46 -> "arm64-v8a"
                else -> throw IllegalStateException("Unsupported architecture $architecture")
            }.let {
                "system-images;android-$version;google_apis;$it"
            }
            val abi = when (architecture) {
                MACOS_ARCHITECTURE.x86_64 -> "x86_64"
                MACOS_ARCHITECTURE.ARM46 -> "arm64-v8a"
                else -> throw IllegalStateException("Unsupported architecture $architecture")
            }

            return DeviceConfigAndroid(
                deviceName = name,
                device = device,
                tag = tag,
                systemImage = systemImage,
                abi = abi
            )
        }
    }
}