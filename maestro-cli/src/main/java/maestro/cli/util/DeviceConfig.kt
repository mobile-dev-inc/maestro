package maestro.cli.util

internal object IosDeviceConfig {

    val device: String = "iPhone-11"
    val versions = listOf(15, 16)
    val runtimes = mapOf(
        15 to "iOS-15-5",
        16 to "iOS-16-2"
    )

    fun generateDeviceName(version: Int) = "Maestro_iPhone_11_$version"
}

internal object AndroidDeviceConfig {
    val device = "pixel_6"
    val tag = "google_apis"
    val versions = listOf(33, 31, 30, 29, 28)
    val systemImages = mapOf(
        28 to "system-images;android-28;google_apis;x86_64",
        29 to "system-images;android-29;google_apis;x86_64",
        30 to "system-images;android-30;google_apis;x86_64",
        31 to "system-images;android-31;google_apis;x86_64",
        33 to "system-images;android-33;google_apis;x86_64"
    )
    val abi = "x86_64"

    fun generateDeviceName(version: Int) = "Maestro_Pixel6_$version"
}