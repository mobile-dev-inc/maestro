package maestro.orchestra.yaml

import maestro.SwipeDirection

data class YamlSwipe(
    val direction: SwipeDirection? = null,
    val start: String? = null,
    val end: String? = null
)