package maestro.orchestra.yaml

import maestro.Platform

data class YamlCondition(
    val platform: Platform? = null,
    val visible: YamlElementSelectorUnion? = null,
    val notVisible: YamlElementSelectorUnion? = null,
    val `true`: String? = null,
)
