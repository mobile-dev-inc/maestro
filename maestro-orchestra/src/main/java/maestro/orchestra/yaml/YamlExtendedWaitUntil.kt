package maestro.orchestra.yaml

data class YamlExtendedWaitUntil(
    val visible: YamlElementSelectorUnion? = null,
    val timeout: Long? = null,
)
