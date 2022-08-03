package maestro.orchestra.yaml

import maestro.orchestra.ApplyConfigurationCommand
import maestro.orchestra.InvalidInitFlowFile
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroConfig
import java.io.File

data class YamlConfig(
    val appId: String,
    val initFlow: YamlInitFlowUnion?,
) {

    fun toCommand(flowFile: File): MaestroCommand {
        val config = MaestroConfig(
            initFlow = initFlow(initFlow, flowFile)
        )
        return MaestroCommand(applyConfigurationCommand = ApplyConfigurationCommand(config))
    }

    fun getInitFlowFile(flowFile: File): File? {
        if (initFlow == null) return null
        return when (initFlow) {
            is StringInitFlow -> getInitFlowFile(flowFile, initFlow.path)
            is YamlInitFlow -> null
        }
    }

    companion object {

        private fun initFlow(initFlow: YamlInitFlowUnion?, flowFile: File): List<MaestroCommand>? {
            if (initFlow == null) return null

            return when (initFlow) {
                is StringInitFlow -> stringInitFlow(initFlow, flowFile)
                is YamlInitFlow -> initFlow.commands.map { it.toCommand() }
            }
        }

        private fun stringInitFlow(initFlow: StringInitFlow, flowFile: File): List<MaestroCommand> {
            val initFlowFile = getInitFlowFile(flowFile, initFlow.path)
            return YamlCommandReader.readCommands(initFlowFile)
        }

        private fun getInitFlowFile(flowFile: File, initFlowPath: String): File {
            val initFlowFile = File(initFlowPath)
            val absoluteInitFlowFile = if (initFlowFile.isAbsolute) {
                initFlowFile
            } else {
                flowFile.parentFile.resolve(initFlowFile)
            }
            if (!absoluteInitFlowFile.exists() || absoluteInitFlowFile.isDirectory) {
                throw InvalidInitFlowFile(absoluteInitFlowFile)
            }
            return absoluteInitFlowFile
        }
    }
}