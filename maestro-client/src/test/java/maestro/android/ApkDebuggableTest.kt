package maestro.android

import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore("Local testing only")
internal class ApkDebuggableTest {

    private val home = System.getenv("HOME")

    @Test
    fun enable() {
        ApkDebuggable.enable(
            apkFile = File("$home/Downloads/reddit.apk"),
            apkOutFile = File("$home/Downloads/reddit-debuggable.apk"),
        )
    }
}
