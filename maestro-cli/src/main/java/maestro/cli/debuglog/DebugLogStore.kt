package maestro.cli.debuglog

import maestro.cli.App
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

object DebugLogStore {

    private val logDirectory = createLogDirectory()

    fun logOutputOf(processBuilder: ProcessBuilder) {
        val command = processBuilder.command().first() ?: "unknown"
        val logFile = logFile(command)
        val redirect = ProcessBuilder.Redirect.to(logFile)
        processBuilder
            .redirectOutput(redirect)
            .redirectError(redirect)
    }

    private fun logFile(named: String): File {
        return File(logDirectory, "$named.log")
    }

    private fun createLogDirectory(): File {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val date = dateFormatter.format(LocalDateTime.now())

        val baseDir = File(AppDirsFactory.getInstance().getUserLogDir("maestro", appVersion(), "mobiledev"))
        val logDirectory = File(baseDir, date)
        logDirectory.mkdirs()
        removeOldLogs(baseDir)
        println("Debug logs are stored in $logDirectory")
        return logDirectory
    }

    private fun removeOldLogs(baseDir: File) {
        if (!baseDir.isDirectory) { return }

        val existing = baseDir.listFiles() ?: return
        val toDelete = existing.sortedByDescending { it.name }
            .drop(KEEP_LOG_COUNT)
            .toList()

        toDelete.forEach { it.deleteRecursively() }
    }

    private fun appVersion(): String {
        val props = App::class.java.classLoader.getResourceAsStream("version.properties").use {
            Properties().apply { load(it) }
        }

        return props["version"].toString()
    }

    private const val KEEP_LOG_COUNT = 5
}
