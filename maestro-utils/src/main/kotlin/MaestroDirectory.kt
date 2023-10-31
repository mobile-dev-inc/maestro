package maestro.utils

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

object MaestroDirectory {
    private val oldPath = Paths.get(System.getProperty("user.home"), ".maestro")
    private val LOGGER = LoggerFactory.getLogger(MaestroDirectory::class.java)

    fun getMaestroDirectory(): Path {
        val dir = getMigratedDirectory()
        // Check if we didn't migrate yet
        if (oldPath.exists()) {
            return oldPath
        }
        return dir
    }

    private fun getMigratedDirectory(): Path {
        val osName = System.getProperty("os.name")

        val newDir = when {
            osName.startsWith("Linux") -> getLinuxDirectory()
            else -> oldPath
        }
        return newDir
    }

    fun migrate() {
        try {
            val dir = getMigratedDirectory()
            if (dir != oldPath && oldPath.exists()) {
                LOGGER.warn("We detected that you use the old maestro directory ($oldPath).")
                val migrate = if (System.getenv("CI") != null) {
                    LOGGER.warn("We seem to run in a CI environment, not migrating.")
                    false
                } else {
                    print("Do you want to automatically migrate to the new directory ($dir)? [y/N]: ")
                    val response = readln()
                    when (response) {
                        "Y", "y" -> true
                        else -> false
                    }
                }

                if (migrate) {
                    oldPath.toFile().copyRecursively(dir.toFile(), true)
                    oldPath.toFile().deleteRecursively()
                    LOGGER.info("Successfully migrated data from $oldPath to $dir")
                }
            }
        } catch (e: IOException) {
            LOGGER.error("Failed to migrate old data", e)
        }
    }

    private fun getLinuxDirectory(): Path {
        val xdgDataHome =
            System.getenv("XDG_DATA_HOME")?.let(::Path) ?: (Path(System.getenv("HOME")) / ".local" / "share")
        return xdgDataHome / "maestro"
    }
}
