package maestro.android

import dadb.Dadb
import maestro.Maestro
import maestro.drivers.AndroidDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

internal class AndroidAppFilesTest {

    private val home = System.getenv("HOME")

    private lateinit var dadb: Dadb

    @BeforeEach
    fun setUp() {
        dadb = Dadb.create("localhost", 5555) ?: throw IllegalStateException("Could not find local emulator")
    }

    @Test
    fun pull() {
        val appZipFile = File("$home/Downloads/com.reddit.frontpage.zip")
        AndroidAppFiles.pull(dadb, "com.reddit.frontpage", appZipFile)
    }

    @Test
    fun `it should addMedia`() {
        val driver = AndroidDriver(
            dadb = Dadb.create("localhost", 5555)
        )
        Maestro.android(driver).addMedia()
    }

    @Test
    fun push() {
        val appZipFile = File("$home/Downloads/com.reddit.frontpage.zip")
        AndroidAppFiles.push(dadb, "com.reddit.frontpage", appZipFile)
    }
}
