package maestro.orchestra.yaml

data class YamlAssertOutgoingRequestsCommand(
    val url: String? = null,
    val assertHeaderIsPresent: String? = null,
    val assertHeadersAndValues: Map<String, String> = emptyMap(),
    val assertHttpMethod: String? = null,
    val assertRequestBodyContains: String? = null,
)
