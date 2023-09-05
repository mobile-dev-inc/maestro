package maestro.orchestra.workspace

import maestro.isMediaFile
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension

class MediaFileVisitor: SimpleFileVisitor<Path>() {

    private val mediaFiles = mutableListOf<Path>()

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.extension.isMediaFile()) mediaFiles.add(file)

        return FileVisitResult.CONTINUE
    }

    fun getMediaFiles() =  mediaFiles.toList()
}