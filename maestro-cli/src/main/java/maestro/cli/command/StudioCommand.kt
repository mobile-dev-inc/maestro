package maestro.cli.command

import maestro.cli.view.blue
import maestro.cli.view.bold
import maestro.cli.view.box
import maestro.studio.MaestroStudio
import picocli.CommandLine
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "studio",
)
class StudioCommand : Callable<Int> {

    override fun call(): Int {
        val port = getFreePort()
        MaestroStudio.start(port)
        val studioUrl = "http://localhost:${port}"
        val message = ("Maestro Studio".bold() + " is running at " + studioUrl.blue()).box()
        println()
        println(message)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(studioUrl))
        }
        Thread.currentThread().join()
        return 0
    }

    private fun getFreePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }
}
