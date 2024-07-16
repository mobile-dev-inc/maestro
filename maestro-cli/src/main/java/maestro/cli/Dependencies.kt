package maestro.cli

import maestro.cli.util.Unpacker.binaryDependency
import maestro.cli.util.Unpacker.unpack
import org.slf4j.LoggerFactory

object Dependencies {
    private val logger = LoggerFactory.getLogger(Dependencies::class.java)

    val APPLESIMUTILS = binaryDependency("applesimutils")

    fun install() {
        logger.info("install()")
        unpack(
            "deps/applesimutils",
            APPLESIMUTILS
        )
        logger.info("install() COMPLETED")
    }

}
