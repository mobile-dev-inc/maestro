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

package maestro.orchestra.yaml

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import maestro.orchestra.MaestroCommand
import maestro.orchestra.NoInputException
import maestro.orchestra.SyntaxError
import okio.Source
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

object YamlCommandReader {

    private val mapper by lazy {
        ObjectMapper(YAMLFactory())
            .apply {
                registerModule(KotlinModule.Builder().build())
            }
    }

    fun readCommands(source: Source): Pair<List<MaestroCommand>, List<MaestroCommand>> {
        return try {
            val commands = readCommandsUnsafe(source)
            val initFlowCommands = getInitCommands(commands)
            initFlowCommands to commands
        } catch (e: MismatchedInputException) {
            val message = e.message ?: throw e

            when {
                message.contains("Unrecognized field") -> throw SyntaxError
                message.contains("Cannot construct instance") -> throw SyntaxError
                message.contains("Cannot deserialize") -> throw SyntaxError
                message.contains("No content to map") -> throw NoInputException
                else -> throw e
            }
        }
    }

    private fun readCommandsUnsafe(source: Source) = mapper.readValue(
        source.buffer().inputStream(),
        object : TypeReference<List<YamlFluentCommand>>() {}
    ).map { it.toCommand() }

    private fun getInitCommands(
        commands: List<MaestroCommand>,
    ): List<MaestroCommand> {
        val config = commands.firstNotNullOfOrNull { it.applyConfigurationCommand }?.config ?: return emptyList()
        val initFlow = config["initFlow"] ?: return emptyList()
        val initFlowBytes = mapper.writeValueAsBytes(initFlow)
        val source = ByteArrayInputStream(initFlowBytes).source()
        return readCommandsUnsafe(source)
    }
}
