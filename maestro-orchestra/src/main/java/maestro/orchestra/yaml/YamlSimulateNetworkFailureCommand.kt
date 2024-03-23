package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlSimulateNetworkFailureCommand(
    val state: Boolean,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(state: Boolean): YamlSimulateNetworkFailureCommand {
            return YamlSimulateNetworkFailureCommand(state)
        }
    }
}