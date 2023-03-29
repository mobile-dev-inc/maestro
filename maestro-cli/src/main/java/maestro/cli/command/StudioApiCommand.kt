package maestro.cli.command

import maestro.cli.App
import maestro.cli.CliError
import maestro.cli.DisableAnsiMixin
import maestro.cli.device.DeviceService
import maestro.cli.session.MaestroSessionManager
import maestro.cli.studio.MaestroStudio
import picocli.CommandLine
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "studio-api",
    hidden = true,
    description = ["Launch Maestro Studio API"],
)
class StudioApiCommand : Callable<Int> {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.ParentCommand
    private val parent: App? = null

    private fun deferPickDevice(): Boolean {
        val connectedDevices = DeviceService.listConnectedDevices()
        return connectedDevices.isEmpty() || connectedDevices.size > 1
    }

    private fun launchApi(session: MaestroSessionManager.MaestroSession? = null) {
        val port = getFreePort()
        MaestroStudio.start(port)
        if (session?.maestro != null) {
            MaestroStudio.setMaestroInstance(session.maestro)
        }

        println()
        println("Maestro Studio API running on port $port")
        Thread.currentThread().join()
    }

    override fun call(): Int {
        if (parent?.platform != null) {
            throw CliError("--platform option was deprecated. You can remove it to run your test.")
        }

        if (deferPickDevice()) {
            // launch API  without a running session
            launchApi()
        } else {
            // create a session normally and launch API
            MaestroSessionManager.newSession(parent?.host, parent?.port, parent?.deviceId, true) { session ->
                launchApi(session)
            }
        }

        return 0
    }

    private fun getFreePort(): Int {
        (9999..11000).forEach { port ->
            try {
                ServerSocket(port).use { return it.localPort }
            } catch (ignore: Exception) {}
        }
        ServerSocket(0).use { return it.localPort }
    }
}
