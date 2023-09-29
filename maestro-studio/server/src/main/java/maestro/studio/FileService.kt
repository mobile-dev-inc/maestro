package maestro.studio

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.*
import java.util.*
import kotlin.streams.toList

data class FileResponse(
    val content: String,
)

data class FileDirectoryResponse(
    val fileTree: List<FileItem>
)

data class FileItem(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val children: List<FileItem>
)

data class SaveFileRequest(
    val absolutePath: String,
    val content: String
)

object FileService {

    fun routes(routing: Routing, workspaceDirectory: File?) {
        routing.get("/api/read-file/{path}") {
            val encodedFilePath = call.parameters["path"] ?: throw IllegalArgumentException("Path parameter is missing")
            val filePath = String(Base64.getDecoder().decode(encodedFilePath))
            val file = File(filePath)
            if (!file.exists() || file.isDirectory) {
                throw IllegalArgumentException("Path (${file.path}) does not point to a file")
            }

            val fileContent = file.readText()
            call.respond(FileResponse(fileContent).json())
        }

        routing.get("/api/read-directory") {
            if (workspaceDirectory == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val fullFileTree = listDirectory(workspaceDirectory.toPath())
            call.respond(FileDirectoryResponse(fullFileTree).json())
        }

        routing.post("/api/save-file") {
            val request = call.parseBody<SaveFileRequest>()
            val filePath = String(Base64.getDecoder().decode(request.absolutePath))
            File(filePath).writeText(request.content)
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun listDirectory(directory: Path): List<FileItem> {
    val encoder = Base64.getEncoder()
    return Files.list(directory)
        .map {
            val children = if (Files.isDirectory(it)) {
                listDirectory(it)
            } else emptyList()
            FileItem(it.fileName.toString(), encoder.encodeToString(it.toAbsolutePath().toString().toByteArray()), Files.isDirectory(it), children)
        }
        .toList()
}