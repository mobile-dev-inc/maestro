package maestro.studio

import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.routing.static
import org.http4k.server.Undertow
import org.http4k.server.asServer

class Server {
}

fun main() {
    val api = routes(
        "/hello" bind GET to {Response(OK).body("HELLO!")}
    )
    val app = routes(
        "/api" bind api,
        singlePageApp(ResourceLoader.Classpath("/web")),
    )
    app.asServer(Undertow(9000)).start()
}