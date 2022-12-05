package maestro.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import maestro.Maestro
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Undertow
import org.http4k.server.asServer

object MaestroStudio {

    fun start(port: Int, maestro: Maestro) {
        val api = routes(
            "/hierarchy" bind GET to {
                val hierarchy = jacksonObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(maestro.viewHierarchy().root)
                Response(OK).body(hierarchy)
            }
        )
        val app = routes(
            "/api" bind api,
            singlePageApp(ResourceLoader.Classpath("/web")),
        )
        val server = app.asServer(Undertow(port))
        server.start()
    }
}
