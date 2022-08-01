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
import maestro.orchestra.CommandReader
import maestro.orchestra.MaestroCommand
import maestro.orchestra.Orchestra
import okio.source
import java.io.File

class MaestroCommandRunner(
    private val maestro: Maestro,
    private val view: ResultView,
    private val commandReader: CommandReader,
) {

    fun run(testFile: File): Boolean {
        val (initCommands, commands) = try {
            testFile.source().use {
                commandReader.readCommands(it)
            }
        } catch (e: Exception) {
            val message = when {
                e is CommandReader.SyntaxError -> "Syntax error"
                e is CommandReader.NoInputException -> "No commands in the file"
                e.message != null -> e.message!!
                else -> "Failed to read commands"
            }

            view.setState(
                ResultView.UiState.Error(
                    message = message
                )
            )
            return false
        }

        return runCommands(initCommands, commands)
    }

    private fun runCommands(
        initCommands: List<MaestroCommand>,
        commands: List<MaestroCommand>,
    ): Boolean {
        val initCommandStatuses = Array(initCommands.size) { CommandStatus.PENDING }
        val commandStatuses = Array(commands.size) { CommandStatus.PENDING }

        fun refreshUi() {
            view.setState(
                ResultView.UiState.Running(
                    initCommands = initCommands
                        .mapIndexed { idx, command ->
                            CommandState(
                                command = command,
                                status = initCommandStatuses[idx]
                            )
                        },
                    commands = commands
                        .mapIndexed { idx, command ->
                            CommandState(
                                command = command,
                                status = commandStatuses[idx]
                            )
                        },
                )
            )
        }

        refreshUi()

        var success = true

        fun executeCommands(commands: List<MaestroCommand>, statuses: Array<CommandStatus>) {
            Orchestra(
                maestro,
                onCommandStart = { idx, _ ->
                    statuses[idx] = CommandStatus.RUNNING
                    refreshUi()
                },
                onCommandComplete = { idx, _ ->
                    statuses[idx] = CommandStatus.COMPLETED
                    refreshUi()
                },
                onCommandFailed = { idx, _, _ ->
                    statuses[idx] = CommandStatus.FAILED
                    refreshUi()
                    success = false
                },
            ).executeCommands(commands)
        }

        executeCommands(initCommands, initCommandStatuses)

        if (!success) return false

        executeCommands(commands, commandStatuses)

        return success
    }

}