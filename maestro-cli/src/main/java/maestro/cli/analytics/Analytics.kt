import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import maestro.cli.util.CiUtils
import maestro.cli.util.EnvUtils
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The new analytics system for Maestro CLI.
 *  - Sends data to /maestro/cli-analytics endpoint.
 *  - Uses configuration from $XDG_CONFIG_HOME/maestro/analytics.json.
 */
object Analytics {
    private val logger = LoggerFactory.getLogger(Analytics::class.java)
    private val analyticsStatePath: Path = EnvUtils.xdgStateHome().resolve("analytics.json")
    private val legacyUuidPath: Path = EnvUtils.legacyMaestroHome().resolve("uuid")

    private val JSON = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val hasRunBefore: Boolean
        get() = legacyUuidPath.exists() || analyticsStatePath.exists()

    private val enabled: Boolean
        get() {
            // Previous versions of Maestro (<1.36.0) used ~/.maestro/uuid to store uuid.
            // If ~/.maestro/uuid already exists, assume permission was granted (for backward compatibility)
            if (legacyUuidPath.exists()) {
                // Migrate to new location
                val uuid = legacyUuidPath.readText()
                saveAnalyticsState(granted = true, uuid = uuid)
                legacyUuidPath.deleteExisting()
                return true
            }

            return analyticsState?.enabled ?: false
        }

    private val analyticsState: AnalyticsState?
        get() {
            if (analyticsStatePath.exists()) {
                return JSON.readValue<AnalyticsState>(analyticsStatePath.readText())
            }

            return null
        }

    fun maybeAskToEnableAnalytics() {
        if (hasRunBefore) return

        while (!Thread.interrupted()) {
            println("Maestro CLI would like to collect anonymous usage data to improve the product.")
            print("Enable analytics? [Y/n] ")

            val str = readlnOrNull()?.lowercase()
            logger.info("User response to analytics enable prompt: $str")
            val granted = str?.isBlank() == true || str == "y" || str == "yes"
            saveAnalyticsState(granted)
            return
        }

        error("Interrupted")
    }

    private fun saveAnalyticsState(granted: Boolean, uuid: String? = null) {
        Thread.dumpStack()

        val state = AnalyticsState(
            uuid = uuid ?: generateUUID(),
            enabled = granted,
            lastUploadedForCLI = null,
        )
        val stateJson = jacksonObjectMapper().writeValueAsString(state)
        analyticsStatePath.parent.createDirectories()
        analyticsStatePath.writeText(stateJson)
        logger.info("Saved analytics to $analyticsStatePath, value: $state")
    }

    private fun generateUUID(): String {
        return CiUtils.getCiProvider() ?: UUID.randomUUID().toString()
    }

    /**
     * Uploads analytics if there was a version update.
     */
    fun maybeUploadAnalyticsAsync() {
        if (!hasRunBefore) {
            logger.info("First run, not uploading")
            return
        }

        if (!enabled) {
            logger.info("Analytics disabled, not uploading")
            return
        }

        val state = analyticsState ?: return
        if (!state.enabled) return

        val report = AnalyticsReport(
            uuid = state.uuid,
            version = EnvUtils.CLI_VERSION?.toString() ?: "Unknown",
            os = EnvUtils.OS_NAME,
            osArch = EnvUtils.OS_ARCH,
            osVersion = EnvUtils.OS_VERSION,
            javaVersion = EnvUtils.getJavaVersion().toString(),
            xcodeVersion = EnvUtils.getXcodeVersion(),
            flutterVersion = EnvUtils.getFlutterVersionAndChannel().first,
            flutterChannel = EnvUtils.getFlutterVersionAndChannel().second,
        )

        logger.info("Will upload analytics report")
        logger.info(report.toString())

        // Update CLI state with last uploaded time/version
    }

//        val latestCliVersion = ApiClient(BASE_API_URL).getLatestCliVersion(
//            freshInstall = FRESH_INSTALL,
//        )
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class AnalyticsState(
    val uuid: String,
    val enabled: Boolean,
    val lastUploadedForCLI: String?,
)

// analytics data:
// .header("X-UUID", Updates.DEVICE_UUID)
// .header("X-VERSION", EnvUtils.getVersion().toString())
// .header("X-OS", EnvUtils.OS_NAME)
// .header("X-OSARCH", EnvUtils.OS_ARCH)
// .header("X-OSVERSION", EnvUtils.OS_VERSION)
// .header("X-JAVA", EnvUtils.getJavaVersion().toString())
// .header("X-XCODE", EnvUtils.getXcodeVersion() ?: "Undefined")
// .header("X-FLUTTER", EnvUtils.getFlutterVersionAndChannel().first ?: "Undefined")
// .header("X-FLUTTER-CHANNEL", EnvUtils.getFlutterVersionAndChannel().second ?: "Undefined")

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnalyticsReport(
    @JsonProperty("uuid") val uuid: String,
    @JsonProperty("cli_version") val version: String,
    @JsonProperty("os") val os: String,
    @JsonProperty("os_arch") val osArch: String,
    @JsonProperty("os_version") val osVersion: String,
    @JsonProperty("java_version") val javaVersion: String?,
    @JsonProperty("xcode_version") val xcodeVersion: String?,
    @JsonProperty("flutter_version") val flutterVersion: String?,
    @JsonProperty("flutter_channel") val flutterChannel: String?,
    // TODO(bartek): List of Android versions of created Android emulators (alternative: list of downlaoded system-images)
    // TODO(bartek): List of installed iOS Simulator runtimes
)
