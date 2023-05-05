package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlDefineSelectors(
    val selectors: Map<String, YamlElementSelectorUnion>
) {
    companion object {

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(selectors: Map<String, YamlElementSelectorUnion>) =
            YamlDefineSelectors(selectors = selectors)
    }
}
