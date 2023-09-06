package maestro.orchestra.workspace

import maestro.orchestra.error.MediaFileNotFound
import maestro.orchestra.yaml.YamlCommandReader
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

class MediaCommandsValidator(private val input: Path) {

    private val mediaFileVisitor by lazy { MediaFileVisitor() }
    private val inputWorkspaceMediaFiles by lazy {
        Files.walkFileTree(input, mediaFileVisitor)
        mediaFileVisitor.getMediaFiles()
    }

    fun validate(flow: Path) {
        YamlCommandReader.readCommands(flow)
            .mapNotNull { it.addMediaCommand }
            .forEach {
                it.mediaPaths.forEach { pathString ->
                    val resolveMediaPath = resolvePath(flow, pathString)
                    inputWorkspaceMediaFiles.find {
                        val resolveInputMedia = resolvePath(flow, pathString)
                        Files.isSameFile(resolveInputMedia, resolveMediaPath)
                    } ?: throw MediaFileNotFound("Media file $pathString in flow file: ${flow.fileName} do not exist in workspace", resolveMediaPath)
                }
            }
    }

    private fun resolvePath(flow: Path, pathString: String): Path {
        val path = flow.fileSystem.getPath(pathString)
        val resolvedPath = if (path.isAbsolute) {
            path
        } else {
            flow.resolveSibling(path).toAbsolutePath()
        }
        return resolvedPath
    }
}