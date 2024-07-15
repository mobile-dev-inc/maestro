package maestro.cli.update

import maestro.cli.api.ApiClient
import maestro.cli.api.CliVersion
import maestro.cli.util.CiUtils
import maestro.cli.util.EnvUtils.CLI_VERSION
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object Updates {
    const val BASE_API_URL = "https://api.mobile.dev"

    private val DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory()
    private val EXECUTOR = Executors.newCachedThreadPool {
        DEFAULT_THREAD_FACTORY.newThread(it).apply { isDaemon = true }
    }

    private var future: CompletableFuture<CliVersion?>? = null

    private fun generateUUID(): String {
        return CiUtils.getCiProvider() ?: UUID.randomUUID().toString()
    }

    fun fetchUpdatesAsync() {
        getFuture()
    }

    fun checkForUpdates(): CliVersion? {
        // Disable update check, when MAESTRO_DISABLE_UPDATE_CHECK is set to "true" e.g. when installed by a package manager. e.g. nix
        if (System.getenv("MAESTRO_DISABLE_UPDATE_CHECK")?.toBoolean() == true) {
            return null
        }
        return try {
            getFuture().get(3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            return null
        }
    }

    private fun fetchUpdates(): CliVersion? {
        if (CLI_VERSION == null) {
            return null
        }

        val latestCliVersion = ApiClient(BASE_API_URL).getLatestCliVersion()

        return if (latestCliVersion > CLI_VERSION) {
            latestCliVersion
        } else {
            null
        }
    }

    @Synchronized
    private fun getFuture(): CompletableFuture<CliVersion?> {
        var future = this.future
        if (future == null) {
            future = CompletableFuture.supplyAsync(this::fetchUpdates, EXECUTOR)!!
            this.future = future
        }
        return future
    }
}
