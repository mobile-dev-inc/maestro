package xcuitest.installer

import xcuitest.XCTestClient

interface XCTestInstaller {
    fun start(sourceIntent: SourceIntent): XCTestClient?

    fun uninstall(sourceIntent: SourceIntent)

    fun isChannelAlive(): Boolean

    fun close(sourceIntent: SourceIntent)
}


enum class Intent(val description: String) {
    DRIVER_OPEN("Opening driver"),
    RETRY("Retrying the connection"),
    DRIVER_CLOSE("Closing the driver")
}

data class SourceIntent(val source: String, val intent: Intent)