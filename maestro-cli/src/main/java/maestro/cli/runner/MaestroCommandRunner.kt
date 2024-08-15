/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package maestro.cli.runner

import maestro.Maestro
import maestro.MaestroException
import maestro.cli.device.Device
import maestro.cli.report.AIOutput
import maestro.cli.report.CommandDebugMetadata
import maestro.cli.report.FlowAIOutput
import maestro.cli.report.FlowDebugOutput
import maestro.cli.runner.resultview.ResultView
import maestro.cli.runner.resultview.UiState
import maestro.orchestra.ApplyConfigurationCommand
import maestro.orchestra.CompositeCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.Orchestra
import maestro.orchestra.OrchestraAppState
import maestro.orchestra.yaml.YamlCommandReader
import maestro.utils.Insight
import okio.Buffer
import org.slf4j.LoggerFactory
import java.io.File
import java.util.IdentityHashMap

object MaestroCommandRunner {

    private val logger = LoggerFactory.getLogger(MaestroCommandRunner::class.java)

    fun runCommands(
        maestro: Maestro,
        device: Device?,
        view: ResultView,
        commands: List<MaestroCommand>,
        debugOutput: FlowDebugOutput,
        aiOutput: FlowAIOutput,
    ): Result {
        val config = YamlCommandReader.getConfig(commands)
        val initFlow = config?.initFlow
        val onFlowComplete = config?.onFlowComplete
        val onFlowStart = config?.onFlowStart

        val commandStatuses = IdentityHashMap<MaestroCommand, CommandStatus>()
        val commandMetadata = IdentityHashMap<MaestroCommand, Orchestra.CommandMetadata>()

        fun takeDebugScreenshot(status: CommandStatus): File? {
            val containsFailed = debugOutput.screenshots.any { it.status == CommandStatus.FAILED }

            // Avoids duplicate failed images from parent commands
            if (containsFailed && status == CommandStatus.FAILED) {
                return null
            }

            val result = kotlin.runCatching {
                val out = File
                    .createTempFile("screenshot-${System.currentTimeMillis()}", ".png")
                    .also { it.deleteOnExit() } // save to another dir before exiting
                maestro.takeScreenshot(out, false)
                debugOutput.screenshots.add(
                    FlowDebugOutput.Screenshot(
                        screenshot = out,
                        timestamp = System.currentTimeMillis(),
                        status = status
                    )
                )
                out
            }

            return result.getOrNull()
        }

        fun writeAIscreenshot(buffer: Buffer): File {
            val out = File
                .createTempFile("ai-screenshot-${System.currentTimeMillis()}", ".png")
                .also { it.deleteOnExit() }
            out.outputStream().use { it.write(buffer.readByteArray()) }
            return out
        }

        fun refreshUi() {
            view.setState(
                UiState.Running(
                    device = device,
                    initCommands = toCommandStates(
                        initFlow?.commands ?: emptyList(),
                        commandStatuses,
                        commandMetadata
                    ),
                    onFlowStartCommands = toCommandStates(
                        onFlowStart?.commands ?: emptyList(),
                        commandStatuses,
                        commandMetadata
                    ),
                    onFlowCompleteCommands = toCommandStates(
                        onFlowComplete?.commands ?: emptyList(),
                        commandStatuses,
                        commandMetadata
                    ),
                    commands = toCommandStates(
                        commands,
                        commandStatuses,
                        commandMetadata
                    )
                )
            )
        }

        refreshUi()

        val orchestra = Orchestra(
            maestro,
            onCommandStart = { _, command ->
                logger.info("${command.description()} RUNNING")
                commandStatuses[command] = CommandStatus.RUNNING
                debugOutput.commands[command] = CommandDebugMetadata(
                    timestamp = System.currentTimeMillis(),
                    status = CommandStatus.RUNNING
                )

                refreshUi()
            },
            onCommandComplete = { _, command ->
                logger.info("${command.description()} COMPLETED")
                commandStatuses[command] = CommandStatus.COMPLETED
                debugOutput.commands[command]?.apply {
                    status = CommandStatus.COMPLETED
                    calculateDuration()
                }
                refreshUi()
            },
            onCommandFailed = { _, command, e ->
                debugOutput.commands[command]?.apply {
                    status = CommandStatus.FAILED
                    calculateDuration()
                    error = e
                }

                takeDebugScreenshot(CommandStatus.FAILED)

                if (e !is MaestroException) {
                    throw e
                } else {
                    debugOutput.exception = e
                }

                logger.info("${command.description()} FAILED")
                commandStatuses[command] = CommandStatus.FAILED
                refreshUi()
                Orchestra.ErrorResolution.FAIL
            },
            onCommandSkipped = { _, command ->
                logger.info("${command.description()} SKIPPED")
                commandStatuses[command] = CommandStatus.SKIPPED
                debugOutput.commands[command]?.apply {
                    status = CommandStatus.SKIPPED
                }
                refreshUi()
            },
            onCommandReset = { command ->
                logger.info("${command.description()} PENDING")
                commandStatuses[command] = CommandStatus.PENDING
                debugOutput.commands[command]?.apply {
                    status = CommandStatus.PENDING
                }
                refreshUi()
            },
            onCommandMetadataUpdate = { command, metadata ->
                logger.info("${command.description()} metadata $metadata")
                commandMetadata[command] = metadata
                refreshUi()
            },
            onCommandGeneratedOutput = { command, defects, screenshot ->
                logger.info("${command.description()} OUTPUTTED")
                val screenshotPath = writeAIscreenshot(screenshot)
                aiOutput.outputs.add(
                    AIOutput(
                        screenshotPath = screenshotPath,
                        defects = defects
                    )
                )
            }
        )

        val flowSuccess = orchestra.runFlow(commands)

        return Result(flowSuccess = flowSuccess, cachedAppState = null)
    }

    private fun toCommandStates(
        commands: List<MaestroCommand>,
        commandStatuses: MutableMap<MaestroCommand, CommandStatus>,
        commandMetadata: IdentityHashMap<MaestroCommand, Orchestra.CommandMetadata>,
    ): List<CommandState> {
        return commands
            // Don't render configuration commands
            .filter { it.asCommand() !is ApplyConfigurationCommand }
            .mapIndexed { _, command ->
                CommandState(
                    command = commandMetadata[command]?.evaluatedCommand ?: command,
                    subOnStartCommands = (command.asCommand() as? CompositeCommand)
                        ?.config()
                        ?.onFlowStart
                        ?.let { toCommandStates(it.commands, commandStatuses, commandMetadata) },
                    subOnCompleteCommands = (command.asCommand() as? CompositeCommand)
                        ?.config()
                        ?.onFlowComplete
                        ?.let { toCommandStates(it.commands, commandStatuses, commandMetadata) },
                    status = commandStatuses[command] ?: CommandStatus.PENDING,
                    numberOfRuns = commandMetadata[command]?.numberOfRuns,
                    subCommands = (command.asCommand() as? CompositeCommand)
                        ?.subCommands()
                        ?.let { toCommandStates(it, commandStatuses, commandMetadata) },
                    logMessages = commandMetadata[command]?.logMessages ?: emptyList(),
                    insight = commandMetadata[command]?.insight ?: Insight("", Insight.Level.NONE)
                )
            }
    }

    data class Result(
        val flowSuccess: Boolean,
        val cachedAppState: OrchestraAppState?
    )
}
