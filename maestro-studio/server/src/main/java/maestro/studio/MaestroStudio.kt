package maestro.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticRootFolder
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import maestro.Maestro
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

object MaestroStudio {

    private val screenshotDir = getScreenshotDir()

    fun start(port: Int, maestro: Maestro) {
        embeddedServer(Netty, port = port) {
            routing {
                get("/api/hierarchy") {
                    val hierarchy = jacksonObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(maestro.viewHierarchy().root)
                    call.respondText(hierarchy)
                }
                static("/screenshot") {
                    staticRootFolder = screenshotDir.toFile()
                }
                singlePageApplication {
                    useResources = true
                    filesPath = "web"
                    defaultPage = "index.html"
                }
            }
        }.start()
    }

    private fun getScreenshotDir(): Path {
        val home = System.getProperty("user.home")
        val parent = if (home.isNullOrBlank()) createTempDirectory() else Path(home)
        val screenshotDir = parent.resolve(".maestro/studio/screenshots")
        screenshotDir.createDirectories()
        return screenshotDir
    }
}
