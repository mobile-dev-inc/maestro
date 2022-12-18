package maestro.studio

import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import maestro.Maestro

object MaestroStudio {

    fun start(port: Int, maestro: Maestro) {
        embeddedServer(Netty, port = port) {
            routing {
                DeviceScreenService.routes(this, maestro)
                singlePageApplication {
                    useResources = true
                    filesPath = "web"
                    defaultPage = "index.html"
                }
            }
        }.start()
    }
}
