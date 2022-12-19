package maestro.studio

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import maestro.Maestro
import maestro.orchestra.MaestroCommand
import maestro.orchestra.Orchestra
import maestro.orchestra.yaml.YamlCommandReader
import maestro.orchestra.yaml.YamlFluentCommand
import java.io.IOException
import java.nio.file.Paths
import java.util.UUID
import kotlin.concurrent.thread

private data class RunCommandRequest(
    val ids: List<UUID>?,
    val yaml: String?,
)

private data class DeleteCommandsRequest(
    val ids: List<UUID>,
)

private data class ReplEntry(
    val id: UUID,
    val yaml: String,
    val commands: List<MaestroCommand>,
    var status: ReplCommandStatus,
)

object ReplService {

    private val executionLock = Object()
    private val entries = mutableListOf<ReplEntry>()

    fun routes(routing: Routing, maestro: Maestro) {
        routing.get("/api/repl") {
            call.respondRepl()
        }
        routing.post("/api/repl/command") {
            val request = call.parseBody<RunCommandRequest>()
            when {
                request.ids != null -> {
                    thread {
                        runEntries(maestro, request.ids)
                    }
                }
                request.yaml != null -> {
                    val newEntries = createEntries(request.yaml)
                    thread {
                        runEntries(maestro, newEntries.map { it.id })
                    }
                }
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Must specify key \"ids\" or \"yaml\"")
                }
            }
            call.respondRepl()
        }
        routing.delete("/api/repl/command") {
            val request = call.parseBody<DeleteCommandsRequest>()
            deleteEntries(request.ids)
            call.respondRepl()
        }
    }

    private suspend fun ApplicationCall.respondRepl() {
        val replCommands = synchronized(entries) {
            entries.map { entry ->
                ReplCommand(entry.id, entry.yaml, entry.status)
            }
        }
        val repl = Repl(replCommands)
        val response = jacksonObjectMapper().writeValueAsString(repl)
        respondText(response)
    }

    private suspend inline fun <reified T> ApplicationCall.parseBody(): T {
        return try {
            receiveStream().use { body ->
                jacksonObjectMapper().readValue(body, T::class.java)
            }
        } catch (e: IOException) {
            throw HttpException(HttpStatusCode.BadRequest, "Failed to parse request body")
        }
    }

    private fun deleteEntries(ids: List<UUID>) {
        synchronized(entries) {
            entries.removeIf { it.id in ids }
        }
    }

    private fun createEntries(yaml: String): List<ReplEntry> {
        val newEntries = try {
            readNodes(yaml).map { node ->
                val yamlCommand = YamlCommandReader.MAPPER.convertValue(node, YamlFluentCommand::class.java)
                val yamlString = YamlCommandReader.MAPPER.writeValueAsString(node)
                val commands = yamlCommand.toCommands(Paths.get(""), "com.example.app")
                ReplEntry(UUID.randomUUID(), yamlString, commands, ReplCommandStatus.PENDING)
            }
        } catch (e: Exception) {
            throw HttpException(HttpStatusCode.BadRequest, "Failed to parse yaml: ${e.message}")
        }
        synchronized(entries) {
            entries.addAll(newEntries)
        }
        return newEntries
    }

    private fun runEntries(maestro: Maestro, ids: List<UUID>) {
        synchronized(executionLock) {
            val entriesToRun = entries.filter { it.id in ids }
            try {
                entriesToRun.forEach { it.status = ReplCommandStatus.PENDING }
                for (entry in entriesToRun) {
                    val failure = executeCommands(maestro, entry.commands)
                    if (failure == null) {
                        entry.status = ReplCommandStatus.SUCCESS
                    } else {
                        entry.status = ReplCommandStatus.ERROR
                        return
                    }
                }
            } finally {
                entriesToRun.filter { it.status == ReplCommandStatus.PENDING }.forEach { it.status = ReplCommandStatus.CANCELED }
            }
        }
    }

    private fun executeCommands(maestro: Maestro, commands: List<MaestroCommand>): Throwable? {
        var failure: Throwable? = null
        val result = Orchestra(maestro, onCommandFailed = { _, _, throwable ->
            failure = throwable
            Orchestra.ErrorResolution.FAIL
        }).executeCommands(commands)
        return if (result) null else (failure ?: RuntimeException("Command execution failed"))
    }

    private fun readNodes(yaml: String): List<JsonNode> {
        return when (val tree = YamlCommandReader.MAPPER.readTree(yaml)) {
            is ArrayNode -> tree.toList()
            else -> listOf(tree)
        }
    }

}