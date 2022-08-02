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
import maestro.orchestra.MaestroCommand
import maestro.orchestra.Orchestra

object MaestroCommandRunner {

    fun runCommands(
        maestro: Maestro,
        view: ResultView,
        initCommands: List<MaestroCommand>,
        commands: List<MaestroCommand>,
        skipInitFlow: Boolean,
    ): Result {
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

        if (skipInitFlow) {
            initCommandStatuses.fill(CommandStatus.COMPLETED)
        } else {
            executeCommands(initCommands, initCommandStatuses)
        }

        if (!success) return Result(initFlowSuccess = false, flowSuccess = false)

        executeCommands(commands, commandStatuses)

        return Result(initFlowSuccess = true, flowSuccess = success)
    }

    data class Result(
        val initFlowSuccess: Boolean,
        val flowSuccess: Boolean
    )
}