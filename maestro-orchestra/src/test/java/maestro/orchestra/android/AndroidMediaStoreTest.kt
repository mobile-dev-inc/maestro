package maestro.orchestra.android

import com.google.common.truth.Truth.assertThat
import dadb.Dadb
import maestro.Maestro
import maestro.drivers.AndroidDriver
import maestro.orchestra.Orchestra
import maestro.orchestra.yaml.YamlCommandReader
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Paths

@Disabled
class AndroidMediaStoreTest {

    @ParameterizedTest
    @MethodSource("provideMediaFlows")
    fun `it should add media and its visible in google photos`(mediaMap: Map<String, String>) {
        // given
        val expectedMediaPath = mediaMap.values.first()
        val mediaFlow = mediaMap.keys.first()
        val dadb = Dadb.create("localhost", 5555)
        val maestro = Maestro.android(AndroidDriver(dadb))
        val maestroCommands = YamlCommandReader.readCommands(Paths.get(mediaFlow))

        // when
        Orchestra(maestro).runFlow(maestroCommands)

        // then
        val exists = dadb.fileExists(expectedMediaPath)
        assertThat(exists).isFalse()
    }

    companion object {
        @JvmStatic
        fun provideMediaFlows(): List<Map<String, String>> {
            return listOf(
                mapOf("./src/test/resources/media/add_media_png.yaml" to "/sdcard/Pictures/android.png"),
                mapOf("./src/test/resources/media/add_media_jpeg.yaml" to "/sdcard/Pictures/android_jpeg.jpeg"),
                mapOf("./src/test/resources/media/add_media_jpg.yaml" to "/sdcard/Pictures/android_jpg.jpg"),
                mapOf("./src/test/resources/media/add_media_gif.yaml" to "/sdcard/Pictures/android_gif.gif"),
                mapOf("./src/test/resources/media/add_media_mp4.yaml" to "/sdcard/Movies/sample_video.mp4"),
                mapOf("./src/test/resources/media/add_media_mp3.yaml" to "/sdcard/Audio/drums.mp3"),
            )
        }
    }
}