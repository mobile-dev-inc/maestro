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

package maestro.cli.command

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import maestro.cli.App
import maestro.cli.util.MaestroFactory
import picocli.CommandLine

@CommandLine.Command(
    name = "hierarchy",
)
class PrintHierarchyCommand : Runnable {

    @CommandLine.ParentCommand
    private val parent: App? = null

    @CommandLine.Spec
    lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun run() {
        MaestroFactory.createMaestro(parent?.platform, parent?.host, parent?.port).use {
            println("Printing hierarchy from ${it.deviceName()}")

            val hierarchy = jacksonObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(it.viewHierarchy())

            println(hierarchy)
        }
    }
}
