package maestro.cli.command.network

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import maestro.cli.util.MaestroFactory
import maestro.cli.util.PrintUtils
import maestro.networkproxy.NetworkProxy
import maestro.networkproxy.yaml.YamlMappingRule
import maestro.networkproxy.yaml.YamlMappingRuleParser
import maestro.networkproxy.yaml.YamlResponse
import okio.buffer
import okio.gzip
import okio.source
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.net.URL
import java.util.concurrent.Callable
import kotlin.concurrent.thread

@Command(
    name = "record",
)
class RecordNetworkCommand : Callable<Int> {

    @CommandLine.ParentCommand
    lateinit var parent: NetworkCommand

    @Parameters
    lateinit var output: File

    private val jsonMapper = jacksonObjectMapper()

    override fun call(): Int {
        val proxy = NetworkProxy(port = 8080)
        proxy.start(emptyList())
        proxy.setListener { request, response ->
            if (request.method.value().isEmpty()) {
                return@setListener
            }

            val url = URL(request.absoluteUrl)

            val outputDirectory = File(
                output,
                "${url.authority}/${url.path}"
            )
            outputDirectory.mkdirs()

            val outputFile = File(outputDirectory, "${request.method.value()}.yaml")

            // TODO reuse
            val isJsonContent = response.headers.getHeader("Content-Type")
                .takeIf { it.isPresent }
                ?.values()
                ?.any { it.contains("application/json") } ?: false

            val bodyFile = File(
                outputDirectory, generateBodyFileName(
                    directory = outputDirectory,
                    method = request.method.value(),
                    isJson = isJsonContent,
                )
            )

            // TODO reuse logic
            val bodyStr = if (response.headers.getHeader("Content-Encoding").containsValue("gzip")) {
                response.bodyStream.source().gzip().buffer().readUtf8()
            } else {
                response.bodyAsString
            }

            val bodyFormatted = if (isJsonContent) {
                // TODO reuse
                try {
                    val jsonObj = jsonMapper.readValue(bodyStr, Any::class.java)
                    jsonMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(jsonObj)
                } catch (e: Exception) {
                    // Ignore invalid JSON
                    bodyStr
                }
            } else {
                bodyStr
            }

            bodyFile.writeText(bodyFormatted)

            val mappingRule = YamlMappingRule(
                path = request.absoluteUrl,
                method = request.method.value(),
                headers = request.headers
                    .all()
                    .associateBy(
                        keySelector = { it.key() },
                        valueTransform = { it.values().joinToString(",") }
                    ),
                response = YamlResponse(
                    status = response.status,
                    headers = response.headers  // TODO reuse logic
                        .all()
                        .associateBy(
                            keySelector = { it.key() },
                            valueTransform = { it.values().joinToString(",") }
                        )
                        .filter {
                            it.key.lowercase() !in setOf(
                                "date", // TODO extract some of those from request as well
                                "last-modified",   // TODO extract some of those from request as well
                                "content-encoding",
                                "content-length",
                            )
                        },
                    bodyFile = bodyFile.name,
                )
            )

            val existingRules = if (outputFile.exists()) {
                YamlMappingRuleParser.readRules(outputFile.toPath())
            } else {
                emptyList()
            }

            YamlMappingRuleParser.saveRules(
                (existingRules + mappingRule).distinct(),
                outputFile.toPath()
            )
        }

        val (maestro, _) = MaestroFactory.createMaestro(parent.app.host, parent.app.port, parent.app.deviceId)

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            maestro.close()
        })

        maestro.use {
            maestro.setProxy(port = 8080)

            PrintUtils.message("Recording network traffic to ${output.absolutePath}")

            while (!Thread.interrupted()) {
                Thread.sleep(100)
            }
        }

        return 0
    }

    private fun generateBodyFileName(
        directory: File,
        method: String,
        isJson: Boolean
    ): String {
        val prefix = "${method}_body"

        val prefixCount = directory
            .listFiles { _, name -> name.startsWith(prefix) }
            ?.count() ?: 0

        return "${prefix}_${prefixCount}.${if (isJson) "json" else "txt"}"
    }

}
