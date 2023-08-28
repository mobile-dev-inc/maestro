package maestro.orchestra.android

import com.google.common.truth.Truth.assertThat
import dadb.Dadb
import maestro.Maestro
import maestro.drivers.AndroidDriver
import maestro.orchestra.Orchestra
import maestro.orchestra.yaml.YamlCommandReader
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@Disabled
class AndroidMediaStoreTest {

    @Test
    fun `it should add media and its visible in google photos`() {
        // given
        val dadb = Dadb.create("localhost", 5555)
        val maestro = Maestro.android(AndroidDriver(dadb))
        val maestroCommands = YamlCommandReader.readCommands(
            Paths.get("./src/test/resources/media/add_media.yaml")
        )

        // when
        Orchestra(maestro).runFlow(maestroCommands)

        // then
        val exists = dadb.fileExists("/sdcard/Pictures/android.png")
        assertThat(exists).isTrue()
    }
}