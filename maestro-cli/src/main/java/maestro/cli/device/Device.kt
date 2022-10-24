package maestro.cli.device

data class Device(
    val id: String,
    val description: String,
    val platform: Platform,
    val connected: Boolean,
) {

    enum class Platform(val description: String) {
        ANDROID("Android"),
        IOS("iOS"),
    }

}