package maestro.orchestra.debug

import maestro.orchestra.ArtifactKind
import java.io.File

/**
 * Where a command puts anything it produces, and reads back anything it already produced.
 *
 * This is the whole answer to "my command needs to write a file — where does it go?". A command
 * names a *kind* and a *name*; everything else is decided here, because everything else is
 * something a caller would otherwise have to remember:
 *
 *  - which folder, and which extension, from the kind's own `ArtifactFormat`
 *  - whether the name needs validating as flow-supplied — see [FLOW_NAMED]
 *  - registration, so the run's manifest — and therefore the Cloud uploader — knows the file
 *
 * That last one is the point. A file written by any other route is not registered, and whatever
 * reads the manifest never sees it.
 *
 * The shape is androidx.test's `TestStorage.openOutputFile` / `openInputFile`: the caller names
 * a thing, the framework owns where it lands.
 */
class Artifacts internal constructor(
    private val writer: BundleWriter,
) {

    /**
     * A file to write [kind] output into, called [name] — *without* an extension.
     *
     * Never null: with no bundle the file is CWD-relative and unregistered, as command output has
     * always been, so no caller needs a fallback of its own.
     */
    fun file(kind: ArtifactKind, name: String): File {
        val label = kind.commandLabel()
        // Only a name a flow can influence can be blank, "." or ".." — and the check has to run
        // before the extension is appended, or "." becomes "..png" and stops looking like one.
        if (kind in FLOW_NAMED) ArtifactRegistry.validateCommandPath(name, label)

        val fileName = "$name${ArtifactRegistry.extensionFor(kind)}"
        val registry = writer.registry ?: return File(fileName)
        return registry.allocateCommandOutput(kind, fileName, label, writer.currentSequenceNumber)
    }

    /** An artifact of [kind] this run already produced under [name], or null. Registers nothing. */
    fun existing(kind: ArtifactKind, name: String): File? = writer.registry?.locate(kind, name)

    private companion object {
        /** Kinds whose name comes from the flow, so it can be empty, "." or "..". */
        private val FLOW_NAMED = setOf(ArtifactKind.TAKE_SCREENSHOT, ArtifactKind.START_SCREEN_RECORDING)

        /** The command a kind belongs to, for the "invalid path for X" message. */
        private fun ArtifactKind.commandLabel(): String = when (this) {
            ArtifactKind.TAKE_SCREENSHOT -> "takeScreenshot"
            ArtifactKind.START_SCREEN_RECORDING -> "startRecording"
            else -> name
        }
    }
}
