package maestro.cli.command.network

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.http.RequestMethod
import maestro.cli.util.MaestroFactory
import maestro.networkproxy.NetworkProxy
import okio.buffer
import okio.gzip
import okio.source
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import picocli.CommandLine
import picocli.CommandLine.Option
import java.util.concurrent.Callable
import kotlin.concurrent.thread

@CommandLine.Command(
    name = "observe",
)
class ObserveNetworkCommand : Callable<Int> {

    @CommandLine.ParentCommand
    lateinit var parent: NetworkCommand

    @Option(names = ["-q", "--query"])
    var query: String? = null

    @Option(names = ["-j", "--json"])
    var jsonOnly: Boolean = false

    private val jsonMapper = jacksonObjectMapper()

    override fun call(): Int {
        val pattern = query?.toRegex(RegexOption.IGNORE_CASE)

        val proxy = NetworkProxy(port = 8080)
        proxy.start(emptyList())
        proxy.setListener { request, response ->
            if (request.method.value().isEmpty()) {
                return@setListener
            }

            val isJsonContent = response.headers.getHeader("Content-Type")
                .takeIf { it.isPresent }
                ?.values()
                ?.any { it.contains("application/json") } ?: false

            if (jsonOnly && !isJsonContent) {
                return@setListener
            }

            val url = request.absoluteUrl

            var bodyStr = if (response.headers.getHeader("Content-Encoding").containsValue("gzip")) {
                response.bodyStream.source().gzip().buffer().readUtf8()
            } else {
                response.bodyAsString
            }

            if (pattern != null) {
                if (!pattern.containsMatchIn(url) && !pattern.containsMatchIn(bodyStr)) {
                    return@setListener
                }
            }

            print(
                ansi()
                    .fg(methodColor(request.method))
                    .render(request.method.value())
                    .fgDefault()
                    .render(" (${response.status})")
                    .render(" $url")
            )
            println()

            if (query != null) {
                if (isJsonContent) {
                    try {
                        val jsonObj = jsonMapper.readValue(bodyStr, Any::class.java)
                        bodyStr = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj)
                    } catch (e: Exception) {
                        // Ignore invalid JSON
                    }
                }

                print(
                    ansi().render(bodyStr)
                )
                println()
            }
        }

        val (maestro, _) = MaestroFactory.createMaestro(parent.app.host, parent.app.port, parent.app.deviceId)
        maestro.use {
            Runtime.getRuntime().addShutdownHook(thread(start = false) {
                maestro.close()
            })

            maestro.setProxy(port = 8080)

            while (!Thread.interrupted()) {
                Thread.sleep(100)
            }
        }

        return 0
    }

    private fun methodColor(method: RequestMethod): Ansi.Color {
        return when (method.value()) {
            "GET" -> Ansi.Color.MAGENTA
            "POST" -> Ansi.Color.GREEN
            "PUT" -> Ansi.Color.CYAN
            "DELETE" -> Ansi.Color.YELLOW
            else -> Ansi.Color.DEFAULT
        }
    }
}