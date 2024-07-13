import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import maestro.cli.util.EnvUtils
import org.slf4j.LoggerFactory
import java.io.File

object Analytics {

    private val logger = LoggerFactory.getLogger(Analytics::class.java)
    private val analyticsStateFile: File

    init {
        analyticsStateFile =
    }

    val enabled: Boolean
        get() {
            // if ~/.maestro/uuid already exists, assume permission was granted (for backward compatibility)


            EnvUtils.xdgStateHome().resolve("analytics.json")

            return true
        }

    /**
     * Uploads analytics if there was a version upda
     */
    public fun maybeUploadAnalyticsAsync() {

    }

    private fun analyticsStatefile(): File {
        return EnvUtils.xdgStateHome().resolve("analytics.json")

    }

}


@JsonIgnoreProperties(ignoreUnknown = true)
data class AnalyticsState(
    val uuid: String,
    val enabled: Boolean,
)
