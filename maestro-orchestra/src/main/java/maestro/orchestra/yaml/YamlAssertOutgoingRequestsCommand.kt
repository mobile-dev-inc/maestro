package maestro.orchestra.yaml

data class YamlAssertOutgoingRequestsCommand(
    val url: String? = null,
    val headersPresent: List<String> = emptyList(),
    val headersAndValues: Map<String, String> = emptyMap(),
    val httpMethodIs: String? = null,
    val requestBodyContains: String? = null,
)
