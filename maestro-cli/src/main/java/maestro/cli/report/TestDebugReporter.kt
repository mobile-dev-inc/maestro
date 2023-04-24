package maestro.cli.report

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import maestro.MaestroException
import maestro.TreeNode
import maestro.cli.runner.CommandStatus
import maestro.debuglog.DebugLogStore
import maestro.debuglog.warn
import maestro.orchestra.MaestroCommand
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.IdentityHashMap
import kotlin.io.path.absolutePathString

object TestDebugReporter {

    private val logger = DebugLogStore.loggerFor(TestDebugReporter::class.java)

    private val dateFormat = "yyyy-MM-dd_HHmmss"
    private val mapper = ObjectMapper()

    private val parentPath = Paths.get(System.getProperty("user.home"), ".maestro", "tests")
    val path: Path

    init {

        // folder
        val dateFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val folderName = dateFormatter.format(LocalDateTime.now())

        path = Paths.get(System.getProperty("user.home"), ".maestro", "tests", folderName)

        Files.createDirectories(path)

        // json
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    }

    fun saveFlow(flowName: String, data: FlowDebugMetadata) {

        // commands
        val commandMetadata = data.commands
        if (commandMetadata.isNotEmpty()) {
            val file = File(path.absolutePathString(), "commands-(${flowName}).json")
            commandMetadata.map { CommandDebugWrapper(it.key, it.value) }.let {
                mapper.writeValue(file, it)
            }
        }

        // screenshots
        data.screenshots.forEach {
            val status = when (it.status) {
                CommandStatus.COMPLETED -> "✅"
                CommandStatus.FAILED -> "❌"
                else -> "﹖"
            }
            val name = "screenshot-$status-${it.timestamp}-(${flowName}).png"
            val file = File(path.absolutePathString(), name)

            it.screenshot.copyTo(file)
        }
    }

    fun saveLogs() {
        // maestro logs
        DebugLogStore.copyTo(File(path.absolutePathString(), "maestro.log"))
        // todo - device logs
    }

    fun deleteOldFiles(path: Path = parentPath, days: Long = 14) {
        try {
            val currentTime = Instant.now()
            val daysLimit = currentTime.minus(Duration.of(days, ChronoUnit.DAYS))

            Files.walk(path)
                .filter {
                    val fileTime = Files.getAttribute(it, "basic:lastModifiedTime") as FileTime
                    val isOlderThanLimit = fileTime.toInstant().isBefore(daysLimit)
                    Files.isDirectory(it) && isOlderThanLimit
                }
                .sorted(Comparator.reverseOrder())
                .forEach {
                    Files.walk(it)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.delete(it) }
                }
        } catch (e: Exception) {
            logger.warn("Failed to delete older files", e)
        }
    }
}

private data class CommandDebugWrapper(
    val command: MaestroCommand,
    val metadata: CommandDebugMetadata
)

data class CommandDebugMetadata(
    var status: CommandStatus? = null,
    var timestamp: Long? = null,
    var duration: Long? = null,
    var error: Throwable? = null,
    var hierarchy: TreeNode? = null
) {
    fun calculateDuration() {
        if (timestamp != null) {
            duration = System.currentTimeMillis() - timestamp!!
        }
    }
}

data class ScreenshotDebugMetadata(
    val screenshot: File,
    val timestamp: Long,
    val status: CommandStatus
)

data class FlowDebugMetadata(
    val commands: IdentityHashMap<MaestroCommand, CommandDebugMetadata> = IdentityHashMap<MaestroCommand, CommandDebugMetadata>(),
    val screenshots: MutableList<ScreenshotDebugMetadata> = mutableListOf(),
    var exception: MaestroException? = null
)

