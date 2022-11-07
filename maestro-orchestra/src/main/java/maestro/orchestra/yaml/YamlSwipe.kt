package maestro.orchestra.yaml

import maestro.SwipeDirection

data class YamlSwipe(
    val direction: SwipeDirection? = null,
    val start: String? = null,
    val end: String? = null,
    val duration: Long = DEFAULT_DURATION_IN_MILLIS
) {
    companion object {
        private const val DEFAULT_DURATION_IN_MILLIS = 2000L
    }
}