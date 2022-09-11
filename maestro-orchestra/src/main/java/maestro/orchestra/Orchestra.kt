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

package maestro.orchestra

import maestro.Maestro
import maestro.MaestroException
import maestro.MaestroTimer
import maestro.orchestra.yaml.YamlCommandReader
import java.io.File
import java.nio.file.Files

class Orchestra(
    private val maestro: Maestro,
    private val stateDir: File? = null,
    private val lookupTimeoutMs: Long = 15000L,
    optionalLookupTimeoutMs: Long = 3000L,
    private val onFlowStart: (List<MaestroCommand>) -> Unit = {},
    private val onCommandStart: (Int, MaestroCommand) -> Unit = { _, _ -> },
    private val onCommandComplete: (Int, MaestroCommand) -> Unit = { _, _ -> },
    private val onCommandFailed: (Int, MaestroCommand, Throwable) -> Unit = { _, _, e -> throw e },
) {
    private val context = OrchestraContext(maestro, optionalLookupTimeoutMs, lookupTimeoutMs)

    /**
     * If initState is provided, initialize app disk state with the provided OrchestraAppState and skip
     * any initFlow execution. Otherwise, initialize app state with initFlow if defined.
     */
    fun runFlow(
        commands: List<MaestroCommand>,
        initState: OrchestraAppState? = null,
    ): Boolean {
        val config = YamlCommandReader.getConfig(commands)
        val state = initState ?: config?.initFlow?.let {
            runInitFlow(it) ?: return false
        }

        if (state != null) {
            maestro.clearAppState(state.appId)
            maestro.pushAppState(state.appId, state.file)
        }

        onFlowStart(commands)
        return executeCommands(commands)
    }

    /**
     * Run the initFlow and return the resulting app OrchestraAppState which can be used to initialize
     * app disk state when past into Orchestra.runFlow.
     */
    fun runInitFlow(
        initFlow: MaestroInitFlow,
    ): OrchestraAppState? {
        val success = runFlow(
            initFlow.commands,
            initState = null,
        )
        if (!success) return null

        maestro.stopApp(initFlow.appId)

        val stateFile = if (stateDir == null) {
            Files.createTempFile(null, ".state")
        } else {
            Files.createTempFile(stateDir.toPath(), null, ".state")
        }
        maestro.pullAppState(initFlow.appId, stateFile.toFile())

        return OrchestraAppState(
            appId = initFlow.appId,
            file = stateFile.toFile(),
        )
    }

    private fun executeCommands(
        commands: List<MaestroCommand>,
    ): Boolean {
        commands
            .forEachIndexed { index, command ->
                onCommandStart(index, command)
                try {
                    executeCommand(command.command)
                    onCommandComplete(index, command)
                } catch (e: Throwable) {
                    onCommandFailed(index, command, e)
                    return false
                }
            }
        return true
    }

    private fun executeCommand(command: Command?) {
        return when (command) {
            is TapOnElementCommand -> {
                tapOnElement(command, command.retryIfNoChange ?: true, command.waitUntilVisible ?: true)
            }
            is TapOnPointCommand -> command.execute(context)
            is BackPressCommand -> command.execute(context)
            is ScrollCommand -> command.execute(context)
            is SwipeCommand -> command.execute(context)
            is AssertCommand -> assertCommand(command)
            is InputTextCommand -> command.execute(context)
            is LaunchAppCommand -> command.execute(context)
            is OpenLinkCommand -> command.execute(context)
            is PressKeyCommand -> command.execute(context)
            is EraseTextCommand -> command.execute(context)
            is ApplyConfigurationCommand, null -> { /* no-op */ }
        }
    }

    private fun assertCommand(command: AssertCommand) {
        command.visible?.let { assertVisible(it) }
        command.notVisible?.let { assertNotVisible(it) }
    }

    private fun assertNotVisible(selector: ElementSelector, timeoutMs: Long = lookupTimeoutMs) {
        val result = MaestroTimer.withTimeout(timeoutMs) {
            try {
                context.findElement(selector, timeoutMs = 2000L)

                // If we got to that point, the element is still visible.
                // Returning null to keep waiting.
                null
            } catch (ignored: MaestroException.ElementNotFound) {
                // Element was not visible, as we expected
                true
            }
        }

        if (result != true) {
            // If we got to this point, it means that element is actually visible
            throw MaestroException.AssertionFailure(
                "${selector.description()} is visible",
                maestro.viewHierarchy().root,
            )
        }
    }

    private fun assertVisible(selector: ElementSelector) {
        context.findElement(selector) // Throws if element is not found
    }

    private fun tapOnElement(
        command: TapOnElementCommand,
        retryIfNoChange: Boolean,
        waitUntilVisible: Boolean,
    ) {
        try {
            val element = context.findElement(command.selector)
            maestro.tap(
                element,
                retryIfNoChange,
                waitUntilVisible,
                command.longPress ?: false,
            )
        } catch (e: MaestroException.ElementNotFound) {

            if (!command.selector.optional) {
                throw e
            }
        }
    }

    companion object {
        val REGEX_OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
    }
}

data class OrchestraAppState(
    val appId: String,
    val file: File,
)
