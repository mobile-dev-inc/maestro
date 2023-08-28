package maestro.orchestra.error

class MediaFileNotFound(
    override val message: String,
    val mediaPath: String
): ValidationError(message)