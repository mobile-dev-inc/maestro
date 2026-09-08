package maestro.orchestra

import maestro.orchestra.debug.ArtifactsGenerator
import java.io.File

/**
 * How a command produces or reads a file. This is the whole artifact surface a command
 * implementation sees.
 *
 * Two verbs, deliberately not interchangeable — the same split androidx.test draws with
 * `TestStorage.openOutputFile` / `openInputFile`:
 *
 *  - [file] produces. The result belongs to this run and is collected with it.
 *  - [existing] reads back something this run already produced.
 *
 * A command never names a directory, never asks whether a bundle exists, and never records
 * anything as a second step: allocating the file *is* declaring it, so the manifest cannot
 * disagree with what is on disk.
 *
 * What this rules out is the bug that motivated it: `assertScreenshot` derived the failure
 * diff's location from the *reference's* location (`resolveSibling`), putting an output into
 * the workspace — an input tree Cloud discards when the run ends. With only these two verbs
 * there is no path from an input location to an output location, because neither hands out a
 * directory to be relative to.
 */
class Artifacts internal constructor(
    private val generator: ArtifactsGenerator,
) {

    /**
     * A file to write [kind] output into, called [name] — without an extension: the kind's own
     * `ArtifactFormat` supplies it, so no caller spells out ".png" again.
     *
     * Decided here rather than by the caller: which folder (from [kind]), the extension, path
     * validation, attribution to the running step, disambiguation across retries of that step,
     * and the manifest entry.
     */
    fun file(kind: ArtifactKind, name: String): File = generator.allocate(kind, name)

    /**
     * An artifact of [kind] this run already produced under [name], or null if there is none.
     * Read-only: allocates nothing and records nothing.
     */
    fun existing(kind: ArtifactKind, name: String): File? = generator.locate(kind, name)
}
