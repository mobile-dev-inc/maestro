package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlInstallApp(
    val apkPath: String? = null,
    val label: String? = null,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(apkPath: String) = YamlInstallApp(
            apkPath = apkPath,
        )
    }
}
