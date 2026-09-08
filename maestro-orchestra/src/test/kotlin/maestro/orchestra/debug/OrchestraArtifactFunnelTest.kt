package maestro.orchestra.debug

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Seam guard, adapted from the one on `ma-4167-screenshot-diff-collector`: every artifact a
 * command produces goes through [maestro.orchestra.Artifacts], never a path the command builds
 * itself. Self-built paths are how artifacts historically fell out of the Cloud bundle one at
 * a time — written somewhere the uploader never reads and recorded nowhere (most recently the
 * assertScreenshot diff, via `resolveSibling`).
 *
 * If this fails, route the new output through `artifacts.file()`; mark a line
 * `// funnel-exempt: read` only when it resolves a path to *read*.
 *
 * A source scan of one file, so it is a tripwire, not a proof — [ArtifactBoundaryTest] covers
 * what a flow actually writes.
 */
class OrchestraArtifactFunnelTest {

    @Test
    fun `orchestra builds no output paths outside the funnel`() {
        val projectDir = System.getenv("PROJECT_DIR") ?: "."
        val orchestra = File(projectDir, "src/main/java/maestro/orchestra/Orchestra.kt")
        assertThat(orchestra.exists()).isTrue()

        val pathConstruction = Regex("""\bFile\s*\(|\bresolveSibling\s*\(|\bPaths\.get\s*\(|\.toFile\s*\(""")
        val source = orchestra.readText()

        val unexempted = pathConstruction.findAll(source)
            .map { source.lineAt(it.range.first) }
            .filterNot { it.contains("// funnel-exempt:") }
            .toList()

        assertThat(unexempted).isEmpty()
    }

    private fun String.lineAt(index: Int): String {
        val start = lastIndexOf('\n', index) + 1
        val end = indexOf('\n', index).let { if (it == -1) length else it }
        return substring(start, end).trim()
    }
}
