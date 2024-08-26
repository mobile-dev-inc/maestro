package maestro.orchestra

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import maestro.Platform
import maestro.js.JsEngine
import maestro.orchestra.util.Env.evaluateScripts

// Note: The appId config is only a yaml concept for now. It'll be a larger migration to get to a point
// where appId is part of MaestroConfig (and factored out of MaestroCommands - eg: LaunchAppCommand).
data class MaestroConfig(
    val appId: MaestroAppId? = null,
    val name: String? = null,
    val tags: List<String>? = emptyList(),
    val ext: Map<String, Any?> = emptyMap(),
    val onFlowStart: MaestroOnFlowStart? = null,
    val onFlowComplete: MaestroOnFlowComplete? = null,
) {

    fun evaluateScripts(jsEngine: JsEngine): MaestroConfig {
        return copy(
            appId = appId?.evaluateScripts(jsEngine),
            name = name?.evaluateScripts(jsEngine),
            onFlowComplete = onFlowComplete?.evaluateScripts(jsEngine),
            onFlowStart = onFlowStart?.evaluateScripts(jsEngine),
        )
    }

}

@JsonDeserialize(using = MaestroAppId.Deserializer::class)
data class MaestroAppId(
    val android: String?,
    val ios: String?,
    val web: String?,
) {
    constructor(appId: String) : this(appId, appId, appId)

    fun forPlatform(platform: Platform) = when (platform) {
        Platform.ANDROID -> android ?: error("No appId specified for android.")
        Platform.IOS -> ios ?: error("No appId specified for ios.")
        Platform.WEB -> web ?: error("No appId specified for web.")
    }

    fun evaluateScripts(jsEngine: JsEngine): MaestroAppId {
        return copy(
            android = android?.evaluateScripts(jsEngine),
            ios = ios?.evaluateScripts(jsEngine),
            web = web?.evaluateScripts(jsEngine),
        )
    }

    class Deserializer : JsonDeserializer<MaestroAppId>() {

        override fun deserialize(parser: JsonParser, context: DeserializationContext): MaestroAppId {
            val node: JsonNode = parser.codec.readTree(parser)
            return when {
                node.isTextual -> MaestroAppId(node.asText())
                node.isObject -> {
                    val android = node.get("android")?.asText()
                    val ios = node.get("ios")?.asText()
                    val web = node.get("web")?.asText()
                    MaestroAppId(android, ios, web)
                }
                else -> throw IllegalArgumentException("Unexpected JSON format for MaestroAppId: $node")
            }
        }
    }
}

data class MaestroOnFlowComplete(val commands: List<MaestroCommand>) {
    fun evaluateScripts(jsEngine: JsEngine): MaestroOnFlowComplete {
        return this
    }
}

data class MaestroOnFlowStart(val commands: List<MaestroCommand>) {
    fun evaluateScripts(jsEngine: JsEngine): MaestroOnFlowStart {
        return this
    }
}
