package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator
import java.lang.UnsupportedOperationException
import maestro.orchestra.MaestroAppId

data class YamlAppId(
    val android: String?,
    val ios: String?,
    val web: String?,
){
    fun asAppId() = MaestroAppId(android, ios, web)

    companion object {

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(appId: Any): YamlAppId {
            val appIdString = when (appId) {
                is String -> appId
                is Map<*, *> -> {
                    val android = appId.getOrDefault("android", null) as String?
                    val ios = appId.getOrDefault("ios", null) as String?
                    val web = appId.getOrDefault("web", null) as String?
                    if (
                        android == null &&
                        ios == null &&
                        web == null
                    ) {
                        throw UnsupportedOperationException("Cannot deserialize appId with no specified platform.")
                    }
                    return YamlAppId(android, ios, web)
                }
                else -> throw UnsupportedOperationException("Cannot deserialize appId with data type ${appId.javaClass}")
            }
            return YamlAppId(appIdString, appIdString, appIdString)
        }
    }
}
