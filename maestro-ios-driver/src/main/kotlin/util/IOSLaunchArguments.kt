package util

object IOSLaunchArguments {

    private val List<String>.isPair: Boolean
        get() = size == 2

    fun List<String>.toIOSLaunchArguments(): List<String> {
        if (isEmpty()) return emptyList()

        val iOSLaunchArguments = mutableListOf<String>()
        forEach {
            val arguments = it.split("=")
            if (arguments.isPair) {
                iOSLaunchArguments += handlePairedArguments(arguments).split("=")
            } else {
                iOSLaunchArguments += it
            }
        }
        return iOSLaunchArguments
    }

    private fun handlePairedArguments(arguments: List<String>): String {
        return if (!arguments[0].startsWith("-")) {
            "-${arguments[0]}=${arguments[1]}"
        } else {
            "${arguments[0]}=${arguments[1]}"
        }
    }

}