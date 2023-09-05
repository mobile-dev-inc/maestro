package maestro.orchestra.workspace

import maestro.orchestra.error.MediaFileNotFound
import maestro.orchestra.yaml.YamlCommandReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

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
                it.mediaPaths.forEach { path ->
                    if (input.isRegularFile()) {
                        validatePathExist(path, flow)
                    } else {
                        if (!File(path).exists()) {
                            throw MediaFileNotFound("Media file $path in flow file: ${flow.fileName} do not exist", path)
                        } else {
                            inputWorkspaceMediaFiles.find { inputMediaPath ->
                                Files.isSameFile(inputMediaPath, Paths.get(path))
                            } ?: throw MediaFileNotFound("Media file $path in flow file: ${flow.fileName} do not exist in workspace", path)
                        }
                    }
                }
            }
    }

    private fun validatePathExist(path: String, flow: Path) {
        if (!File(path).exists()) {
            throw MediaFileNotFound("Media file $path in flow file: ${flow.fileName} do not exist", path)
        }
    }


}