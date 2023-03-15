package maestro.cli.idb

import maestro.cli.device.Device
import maestro.debuglog.DebugLogStore
import maestro.utils.MaestroTimer
import java.net.Socket

object IdbCompanion {
    private val logger = DebugLogStore.loggerFor(IdbCompanion::class.java)

    // TODO: Understand why this is a separate method from strartIdbCompanion
    fun setup(device: Device.Connected) {
        val idbProcessBuilder = ProcessBuilder("idb_companion", "--udid", device.instanceId)
        idbProcessBuilder.start()

        val idbHost = "localhost"
        val idbPort = 10882
        MaestroTimer.retryUntilTrue(timeoutMs = 30000, delayMs = 100) {
            Socket(idbHost, idbPort).use { true }
        }
    }
}
