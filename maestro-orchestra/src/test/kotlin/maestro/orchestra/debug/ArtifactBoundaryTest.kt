package maestro.orchestra.debug

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import maestro.DeviceInfo
import maestro.Maestro
import maestro.MaestroException
import maestro.TreeNode
import maestro.ViewHierarchy
import maestro.device.Platform
import maestro.orchestra.ArtifactManifest
import maestro.orchestra.AssertScreenshotCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.Orchestra
import maestro.orchestra.RetryCommand
import maestro.orchestra.TakeScreenshotCommand
import okio.Sink
import okio.buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

/**
 * The boundary the artifact bundle is supposed to hold, asserted by running a flow and
 * looking at the filesystem.
 *
 * [OrchestraArtifactFunnelTest] scans one file for the *shape* of the mistake; this covers
 * what a flow actually wrote, wherever the writing code lives. `nothing outside the bundle`
 * is the one that matters: the assertScreenshot diff was written outside it entirely, so a
 * manifest-orphan check alone would never have seen it.
 */
class ArtifactBoundaryTest {

    @TempDir
    lateinit var tempDir: Path

    private fun mockMaestro(): Maestro = mockk(relaxed = true) {
        coEvery { viewHierarchy(any()) } returns ViewHierarchy(TreeNode(attributes = mutableMapOf()))
        coEvery { cachedDeviceInfo } returns DeviceInfo(
            platform = Platform.ANDROID,
            widthPixels = 100,
            heightPixels = 200,
            widthGrid = 100,
            heightGrid = 200,
        )
    }

    /** Sized so the comparison keeps the diff rectangle, which is what writes a diff file. */
    private fun png(offset: Int): ByteArray = ByteArrayOutputStream()
        .also { out ->
            val image = BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB)
            image.createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, 100, 200)
                color = Color.BLACK
                fillRect(offset, 0, 60, 120)
                dispose()
            }
            ImageIO.write(image, "png", out)
        }
        .toByteArray()

    private fun maestroWithChangingScreens(): Maestro = mockMaestro().also { maestro ->
        var captures = 0
        coEvery { maestro.takeScreenshot(any<Sink>(), any(), any()) } answers {
            // Never identical to the reference (png(0)): the first capture must already differ,
            // or the assertion passes and there is no diff to look for.
            firstArg<Sink>().buffer().use { it.write(png(40 * (++captures))) }
        }
    }

    /** A failing screenshot assertion inside a retry — the shape from the reported issue. */
    private fun failingAssertInsideRetry(reference: Path): List<MaestroCommand> = listOf(
        MaestroCommand(
            retryCommand = RetryCommand(
                maxRetries = "3",
                config = null,
                commands = listOf(
                    MaestroCommand(
                        assertScreenshotCommand = AssertScreenshotCommand(
                            path = reference.toString(),
                            thresholdPercentage = "100",
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun writeReference(dir: Path): Path {
        dir.createDirectories()
        val reference = dir.resolve("home_baseline.png")
        Files.write(reference, png(0))
        return reference
    }

    private fun filesUnder(root: Path): Set<String> =
        Files.walk(root).use { paths ->
            paths.filter(Files::isRegularFile).map { root.relativize(it).joinToString("/") }.toList().toSet()
        }

    @Test
    fun `a flow writes nothing outside its bundle`() {
        // Stands in for the customer's repo: a committed reference, outside the run bundle.
        val workspace = tempDir.resolve("workspace")
        val reference = writeReference(workspace.resolve("logs/screenshots"))
        val before = filesUnder(workspace)

        val bundle = tempDir.resolve("bundle").also { it.createDirectories() }
        val orchestra = Orchestra(maestro = maestroWithChangingScreens(), artifactsDir = bundle)

        assertThrows<MaestroException.AssertionFailure> {
            runBlocking { orchestra.runFlow(failingAssertInsideRetry(reference)) }
        }

        // The workspace is an input tree. On Cloud it is deleted with the machine, so anything a
        // flow leaves here is lost — which is exactly how the diff went missing.
        assertThat(filesUnder(workspace)).isEqualTo(before)
    }

    @Test
    fun `every file under the bundle appears in the manifest`() {
        val reference = writeReference(tempDir.resolve("workspace/logs/screenshots"))
        val bundle = tempDir.resolve("bundle").also { it.createDirectories() }
        val orchestra = Orchestra(maestro = maestroWithChangingScreens(), artifactsDir = bundle)

        assertThrows<MaestroException.AssertionFailure> {
            runBlocking { orchestra.runFlow(failingAssertInsideRetry(reference)) }
        }

        // The manifest carries a `$schema` URL the model deliberately does not field.
        val manifest: ArtifactManifest = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(bundle.resolve(BundleLayout.MANIFEST_JSON).toFile())
        val declared = manifest.entries.map { it.relativePath }.toSet()

        val undeclared = filesUnder(bundle)
            .filterNot { it == BundleLayout.MANIFEST_JSON }
            // A collection entry declares its folder; members live under it.
            .filterNot { path -> declared.any { path == it || path.startsWith("$it/") } }

        assertThat(undeclared).isEmpty()
    }

    @Test
    fun `the failure diff lands in the bundle, one per retry attempt`() {
        val reference = writeReference(tempDir.resolve("workspace/logs/screenshots"))
        val bundle = tempDir.resolve("bundle").also { it.createDirectories() }
        val orchestra = Orchestra(maestro = maestroWithChangingScreens(), artifactsDir = bundle)

        assertThrows<MaestroException.AssertionFailure> {
            runBlocking { orchestra.runFlow(failingAssertInsideRetry(reference)) }
        }

        val diffs = bundle.resolve(BundleLayout.SCREENSHOT_DIFF_DIR).listDirectoryEntries()
        // 1 initial attempt + 3 retries, each keeping its own diff rather than overwriting.
        assertThat(diffs).hasSize(4)
        assertThat(diffs.map { it.fileName.toString() }.toSet()).hasSize(4)
    }

    @Test
    fun `takeScreenshot still lands exactly where the flow asked, inside the bundle`() {
        val bundle = tempDir.resolve("bundle").also { it.createDirectories() }
        val orchestra = Orchestra(maestro = maestroWithChangingScreens(), artifactsDir = bundle)

        runBlocking {
            orchestra.runFlow(listOf(MaestroCommand(takeScreenshotCommand = TakeScreenshotCommand(path = "shots/home"))))
        }

        assertThat(bundle.resolve("${BundleLayout.TAKE_SCREENSHOT_DIR}/shots/home.png").toFile().exists()).isTrue()
    }
}
