package maestro.orchestra.yaml

import maestro.ScrollDirection

data class YamlScrollUntilVisible(
    val direction: ScrollDirection ?= null,
    val element: YamlElementSelectorUnion? = null,
    val timeout: Long? = null,
)