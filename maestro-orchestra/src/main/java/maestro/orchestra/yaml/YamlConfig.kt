package maestro.orchestra.yaml

data class YamlConfig(
    val appId: String,
    val initFlow: YamlInitFlowUnion?,
)