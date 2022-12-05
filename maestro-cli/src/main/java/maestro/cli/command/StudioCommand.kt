package maestro.cli.command

import maestro.studio.Server
import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "studio",
)
class StudioCommand : Callable<Int> {

    override fun call(): Int {
        Server.start(9000)
        Thread.currentThread().join()
        return 0
    }
}
