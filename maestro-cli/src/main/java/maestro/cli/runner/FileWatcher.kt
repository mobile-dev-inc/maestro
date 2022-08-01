package maestro.cli.runner

import com.sun.nio.file.SensitivityWatchEventModifier
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class FileWatcher {

    private val watchService = FileSystems.getDefault().newWatchService()

    private val registrations: MutableMap<WatchKey, MutableSet<FileWatcherRegistration>> = mutableMapOf()

    fun register(
        path: Path,
        block: (path: Path) -> Unit,
    ): FileWatcherRegistration {
        if (path.parent == null || !path.parent.isDirectory()) {
            throw IllegalArgumentException("Invalid path: $path")
        }
        val watchKey = path.parent.register(
            watchService,
            arrayOf(
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY,
            ),
            SensitivityWatchEventModifier.HIGH,
        )
        return FileWatcherRegistration(path.toAbsolutePath(), block).apply {
            registrations.computeIfAbsent(watchKey) { mutableSetOf() }.add(this)
        }
    }

    fun start() {
        registrations.values.flatten().forEach { registration ->
            registration.block(registration.path)
        }

        while (true) {
            val watchKey = watchService.take()
            handleEvent(watchKey)
        }
    }

    private fun handleEvent(watchKey: WatchKey) {
        if (!watchKey.isValid) {
            registrations.remove(watchKey)
            return
        }

        try {
            watchKey.pollEvents().map { it.context() as Path }.forEach { relativePath ->
                val fullPath = (watchKey.watchable() as Path).resolve(relativePath).toAbsolutePath()
                val registrationSet = registrations[watchKey] ?: return
                registrationSet.toList().forEach { registration ->
                    if (registration.path == fullPath) {
                        registration.block(fullPath)
                    }
                }
            }
        } finally {
            if (!watchKey.reset()) {
                registrations.remove(watchKey)
            }
        }
    }

    inner class FileWatcherRegistration(
        val path: Path,
        val block: (path: Path) -> Unit,
    ) {

        fun cancel() {
            registrations.entries.removeIf { (watchKey, registrationSet) ->
                if (registrationSet.remove(this) && registrationSet.isEmpty()) {
                    watchKey.cancel()
                    true
                } else {
                    false
                }
            }
        }
    }
}

fun main() {
    val fileWatcher = FileWatcher()

    var bRegistration: FileWatcher.FileWatcherRegistration? = null
    fileWatcher.register(Paths.get("/Users/leland/work/a")) { aPath ->
        println("Received event A: $aPath")
        bRegistration?.cancel()
        bRegistration = null

        if (aPath.exists()) {
            val bPath = aPath.resolve(aPath.readText().trim())
            println("bPath: $bPath")
            if (bPath.parent.isDirectory()) {
                println("Register B: $bPath")
                bRegistration = fileWatcher.register(bPath) { bPath ->
                    println("Received event B: $bPath")
                }
            }
        }
    }
    fileWatcher.start()
}
