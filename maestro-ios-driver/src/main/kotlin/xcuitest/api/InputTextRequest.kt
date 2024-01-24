package xcuitest.api

data class InputTextRequest(
    val text: String,
    val typingFrequency: Int?,
    val appIds: Set<String>
)
