package maestro.cli.debuglog

import maestro.cli.App
import maestro.utils.FileUtils
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

object DebugLogStore {

    private const val APP_NAME = "maestro"
    private const val APP_AUTHOR = "mobile_dev"
    private const val LOG_DIR_DATE_FORMAT = "yyyy-MM-dd_HHmmss"
    private const val KEEP_LOG_COUNT = 5

    private val logDirectory: File

    init {
        val dateFormatter = DateTimeFormatter.ofPattern(LOG_DIR_DATE_FORMAT)
        val date = dateFormatter.format(LocalDateTime.now())
        val baseDir = File(AppDirsFactory.getInstance().getUserLogDir(APP_NAME, null, APP_AUTHOR))
        logDirectory = File(baseDir, date)
        logDirectory.mkdirs()
        removeOldLogs(baseDir)
        println("Debug logs are stored in $logDirectory")

        logSystemInfo()
    }

    fun logOutputOf(processBuilder: ProcessBuilder) {
        val command = processBuilder.command().first() ?: "unknown"
        val logFile = logFile(command)
        val redirect = ProcessBuilder.Redirect.to(logFile)
        processBuilder
            .redirectOutput(redirect)
            .redirectError(redirect)
    }

    fun finalizeRun() {
        val output = File(logDirectory.parent, "${logDirectory.name}.zip")
        FileUtils.zipDir(logDirectory.toPath(), output.toPath())
        logDirectory.deleteRecursively()
    }

    private fun logFile(named: String): File {
        return File(logDirectory, "$named.log")
    }

    private fun removeOldLogs(baseDir: File) {
        if (!baseDir.isDirectory) { return }

        val existing = baseDir.listFiles() ?: return
        val toDelete = existing.sortedByDescending { it.name }
            .drop(KEEP_LOG_COUNT)
            .toList()

        toDelete.forEach { it.deleteRecursively() }
    }

    private fun logSystemInfo() {
        val logData = """
            Maestro version: ${appVersion()}
            OS: ${System.getProperty("os.name")}
            OS version: ${System.getProperty("os.version")}
            Architecture: ${System.getProperty("os.arch")}
            """.trimIndent() + "\n"

        logFile("system_info").writeText(logData)
    }

    private fun appVersion(): String {
        val props = App::class.java.classLoader.getResourceAsStream("version.properties").use {
            Properties().apply { load(it) }
        }

        return props["version"].toString()
    }
}
