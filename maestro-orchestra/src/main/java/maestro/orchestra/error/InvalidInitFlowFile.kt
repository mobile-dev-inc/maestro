package maestro.orchestra.error

import java.nio.file.Path

@Deprecated("Obsolete API. See #1921")
class InvalidInitFlowFile(
    override val message: String,
    val initFlowPath: Path
) : RuntimeException()
