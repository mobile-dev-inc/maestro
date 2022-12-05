package maestro.studio

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer

class Server private constructor(private val server: Http4kServer) {

    fun stop() {
        server.stop()
    }

    companion object {

        fun start(port: Int): Server {
            val api = routes(
                "/hello" bind GET to {Response(OK).body("HELLO!")}
            )
            val app = routes(
                "/api" bind api,
                singlePageApp(ResourceLoader.Classpath("/web")),
            )
            val server = app.asServer(Undertow(port))
            server.start()
            return Server(server)
        }
    }
}
