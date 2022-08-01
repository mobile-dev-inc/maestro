package maestro.android

import maestro.utils.TemporaryDirectory
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.AxmlWriter
import pxb.android.axml.NodeVisitor
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

// https://developer.android.com/reference/android/R.attr#debuggable
private const val ANDROID_R_DEBUGGABLE = 16842767

object ApkDebuggable {

    fun enable(apkFile: File, apkOutFile: File) {
        val androidHome = requireAndroidHome()
        val debugKeystore = requireDebugKeystore()
        val buildToolsDir = AndroidBuildToolsDirectory.findBuildToolsDir(androidHome)
        val originalManifestBytes = getOriginalManifestBytes(apkFile)
        val debuggableManifestBytes = createDebuggableManifestBytes(originalManifestBytes)
        TemporaryDirectory.use { workDir ->
            val apkPath = workDir.resolve("tmp.apk")
            val manifestPath = workDir.resolve("AndroidManifest.xml")

            apkFile.copyTo(apkPath.toFile())
            manifestPath.writeBytes(debuggableManifestBytes)

            runShell(workDir, "zip $apkPath AndroidManifest.xml")
            runShell(workDir, "$buildToolsDir/zipalign -p -f 4 $apkPath $apkPath.aligned")
            runShell(workDir, "$buildToolsDir/apksigner sign --ks $debugKeystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android $apkPath.aligned")
            runShell(workDir, "mv $apkPath.aligned $apkOutFile")
        }
    }

    private fun getOriginalManifestBytes(apkFile: File): ByteArray {
        FileSystems.newFileSystem(apkFile.toPath(), null).use { fs ->
            val manifestPath = fs.getPath("AndroidManifest.xml")
            return Files.readAllBytes(manifestPath)
        }
    }

    private fun createDebuggableManifestBytes(originalManifestBytes: ByteArray): ByteArray {
        val reader = AxmlReader(originalManifestBytes)
        val writer = AxmlWriter()
        reader.accept(object : AxmlVisitor(writer) {

            override fun ns(prefix: String?, uri: String?, ln: Int) {
                if (uri != null) {
                    super.ns(prefix, uri, ln)
                }
            }

            override fun child(ns: String?, name: String?): NodeVisitor {
                return object : NodeVisitor(super.child(ns, name)) {

                    override fun child(ns: String?, name: String?): NodeVisitor {
                        if (name != "application") return super.child(ns, name)

                        return object : NodeVisitor(super.child(ns, name)) {

                            override fun end() {
                                super.attr(
                                    "http://schemas.android.com/apk/res/android",
                                    "debuggable",
                                    ANDROID_R_DEBUGGABLE,
                                    TYPE_INT_BOOLEAN,
                                    -1, // true
                                )
                                super.end()
                            }
                        }
                    }
                }
            }
        })
        return writer.toByteArray()
    }

    private fun requireAndroidHome(): File {
        val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
        if (androidSdkRoot != null && androidSdkRoot.isNotBlank()) return File(androidSdkRoot)
        val androidHome = System.getenv("ANDROID_HOME_ROOT")
        if (androidHome == null || androidHome.isBlank()) {
            throw IllegalStateException("\$ANDROID_SDK_ROOT or \$ANDROID_HOME must be set")
        }
        return File(androidHome)
    }

    private fun requireDebugKeystore(): File {
        val home = System.getenv("HOME")
        if (home == null || home.isBlank()) throw IllegalStateException("\$HOME must be set")
        val debugKeystore = File("$home/.android/debug.keystore")
        if (!debugKeystore.exists()) {
            throw IllegalStateException("Could not find debug keystore at $debugKeystore")
        }
        return debugKeystore
    }

    private fun runShell(cwd: Path, script: String, ignoreError: Boolean = false) {
        val process = ProcessBuilder()
            .command(listOf("bash", "-c", "cd $cwd && $script"))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (!ignoreError && process.waitFor() != 0) {
            throw IllegalStateException("Command failed: $script\n$output")
        }
    }
}
