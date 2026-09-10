package xcuitest.api

data class DoubleTouchRequest(
    val x: Float,
    val y: Float,
    val interval: Double?,
    val duration: Double? = null,
)
