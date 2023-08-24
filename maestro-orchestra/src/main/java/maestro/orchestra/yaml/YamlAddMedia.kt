package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlAddMedia(
    val file: String? = null,
) {
    companion object {

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(file: String) = YamlRunFlow(
            file = file,
        )
    }
}