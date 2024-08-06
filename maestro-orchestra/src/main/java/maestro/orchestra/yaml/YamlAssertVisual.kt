package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

private const val DEFAULT_DIFF_THRESHOLD = 95

data class YamlAssertVisual(
    val baseline: String,
    val thresholdPercentage: Int = DEFAULT_DIFF_THRESHOLD,
    val optional: Boolean = false,
    val label: String? = null,
) {

    companion object {

        // TODO(bartek): This might be needed if single value is passed in YAML
        // @JvmStatic
        // @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        // fun parse(appId: String): YamlLaunchApp {
        //     return YamlLaunchApp(
        //         appId = appId,
        //         clearState = null,
        //         clearKeychain = null,
        //         stopApp = null,
        //         permissions = null,
        //         arguments = null,
        //     )
        // }
    }
}
