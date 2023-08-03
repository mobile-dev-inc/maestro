package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonAnySetter
import maestro.MaestroException
import maestro.orchestra.ApplyConfigurationCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroConfig
import maestro.orchestra.MaestroOnFlowComplete
import maestro.orchestra.MaestroOnFlowStart
import java.nio.file.Path

data class YamlConfig(
    val name: String?,
    val appId: String,
    val initFlow: YamlInitFlowUnion?,
    val tags: List<String>? = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val onFlowStart: YamlOnFlowStart?,
    val onFlowComplete: YamlOnFlowComplete?,
) {

    private val ext = mutableMapOf<String, Any?>()

    @JsonAnySetter
    fun setOtherField(key: String, other: Any?) {
        ext[key] = other
    }

    fun toCommand(flowPath: Path): MaestroCommand {
        if (initFlow != null) {
            throw MaestroException.DeprecatedCommand("initFlow command is deprecated, please use onFlowStart instead")
        }

        val config = MaestroConfig(
            appId = appId,
            name = name,
            tags = tags,
            ext = ext.toMap(),
            onFlowStart = onFlowStart(flowPath),
            onFlowComplete = onFlowComplete(flowPath)
        )
        return MaestroCommand(ApplyConfigurationCommand(config))
    }

    fun getWatchFiles(flowPath: Path): List<Path> {
        return emptyList()
    }

    private fun onFlowComplete(flowPath: Path): MaestroOnFlowComplete? {
        if (onFlowComplete == null) return null

        return MaestroOnFlowComplete(onFlowComplete.commands.flatMap { it.toCommands(flowPath, appId) })
    }

    private fun onFlowStart(flowPath: Path): MaestroOnFlowStart? {
        if (onFlowStart == null) return null

        return MaestroOnFlowStart(onFlowStart.commands.flatMap { it.toCommands(flowPath, appId) })
    }
}