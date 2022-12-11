package maestro.cli.update

import maestro.cli.api.ApiClient
import maestro.cli.api.CliVersion
import maestro.cli.view.red
import java.nio.file.Paths
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object Updates {

    private val BASE_API_URL = "https://api.mobile.dev"
    private val DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory()
    private val EXECUTOR = Executors.newCachedThreadPool {
        DEFAULT_THREAD_FACTORY.newThread(it).apply { isDaemon = true }
    }
    private val OS_NAME = System.getProperty("os.name")
    private val FRESH_INSTALL: Boolean
    private val DEVICE_UUID: String
    private val CLI_VERSION: CliVersion? = getVersion().apply {
        if (this == null) {
            System.err.println("\nWarning: Failed to parse current version".red())
        }
    }

    init {
        val uuidPath = Paths.get(System.getProperty("user.home"), ".mobiledev", "uuid")
        FRESH_INSTALL = if (uuidPath.exists()) {
            false
        } else {
            uuidPath.parent.createDirectories()
            uuidPath.writeText(UUID.randomUUID().toString())
            true
        }
        DEVICE_UUID = uuidPath.readText()
    }

    fun checkForUpdates(): CliVersion? {
        if (CLI_VERSION == null) {
            return null
        }

        val future = CompletableFuture.supplyAsync({
            ApiClient(BASE_API_URL).getLatestCliVersion(
                deviceUuid = DEVICE_UUID,
                osName = OS_NAME,
                currentVersion = CLI_VERSION,
                freshInstall = FRESH_INSTALL,
            )
        }, EXECUTOR)

        val latestCliVersion = try {
            future.get(3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            return null
        }

        return if (latestCliVersion > CLI_VERSION) {
            latestCliVersion
        } else {
            null
        }
    }

    private fun getVersion(): CliVersion? {
        val props = try {
            Updates::class.java.classLoader.getResourceAsStream("version.properties").use {
                Properties().apply { load(it) }
            }
        } catch (e: Exception) {
            return null
        }

        val versionString = props["version"] as? String ?: return null

        return CliVersion.parse(versionString)
    }
}

fun main() {
    println(Updates.checkForUpdates())
}