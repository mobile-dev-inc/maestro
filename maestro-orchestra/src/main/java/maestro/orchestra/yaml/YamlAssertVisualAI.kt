package maestro.orchestra.yaml

private const val DEFAULT_DIFF_THRESHOLD = 95

data class YamlAssertVisualAI(
    val assertion: String? = null,
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
