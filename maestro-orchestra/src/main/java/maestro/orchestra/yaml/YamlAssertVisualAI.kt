package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlAssertVisualAI(
    val assertion: String? = null,
    val optional: Boolean = false,
    val label: String? = null,
) {

    companion object {

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(assertion: String): YamlAssertVisualAI {
            return YamlAssertVisualAI(
                assertion = assertion,
                optional = true,
            )
        }
    }
}
