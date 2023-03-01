package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlClearState(
    val appId: String? = null,
    val permissions: Map<String, String>?,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(appId: String) = YamlClearState(
            appId = appId,
            permissions = null
        )
    }
}
