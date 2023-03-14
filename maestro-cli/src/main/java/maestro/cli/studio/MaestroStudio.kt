package maestro.cli.studio

import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ApplicationReceivePipeline
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import maestro.Maestro

object MaestroStudio {

    fun setMaestroInstance(maestro: Maestro) {
        DeviceScreenService.maestro = maestro
        ReplService.maestro = maestro
    }

    fun start(port: Int) {
        embeddedServer(Netty, port = port) {
            install(CORS) {
                allowHost("localhost:3000")
                allowHost("studio.mobile.dev", listOf("https"))
                allowHeader(HttpHeaders.ContentType)
            }
            install(StatusPages) {
                exception<HttpException> { call, cause ->
                    call.respond(cause.statusCode, cause.errorMessage)
                }
                exception { _, cause: Throwable ->
                    cause.printStackTrace()
                }
            }
            receivePipeline.intercept(ApplicationReceivePipeline.Before) {
                withContext(Dispatchers.IO) {
                    proceed()
                }
            }
            routing {
                DeviceScreenService.routes(this)
                ReplService.routes(this)
                MockService.routes(this, MockInteractor())
                DevicesService.routes(this)
                this.get("/") {
                    call.respondText("running")
                }
            }
        }.start()
    }
}
