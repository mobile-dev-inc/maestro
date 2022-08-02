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
import java.io.File

object YamlCommandReader {

    private val mapper by lazy {
        ObjectMapper(YAMLFactory())
            .apply {
                registerModule(KotlinModule.Builder().build())
            }
    }

    // Files to watch for changes. Includes any referenced files.
    fun getWatchFiles(flowFile: File): List<File> {
        return listOfNotNull(
            flowFile,
            getInitFlowFile(flowFile)
        ).filter { it.parentFile.isDirectory }
    }

    fun readCommands(flowFile: File): Pair<List<MaestroCommand>, List<MaestroCommand>> {
        return try {
            val commands = flowFile.source().use {
                readCommandsUnsafe(it)
            }
            val initFlowCommands = getInitCommands(flowFile, commands)
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
        flowFile: File,
        commands: List<MaestroCommand>,
    ): List<MaestroCommand> {
        val config = getConfig(commands) ?: return emptyList()
        val initFlow = config["initFlow"] ?: return emptyList()
        return if (initFlow is String) {
            val initFlowFile = getInitFlowFile(flowFile, initFlow)
            if (!initFlowFile.exists() || initFlowFile.isDirectory) return emptyList()
            readCommands(initFlowFile).second
        } else {
            val initFlowBytes = mapper.writeValueAsBytes(initFlow)
            val source = ByteArrayInputStream(initFlowBytes).source()
            readCommandsUnsafe(source)
        }
    }

    private fun getInitFlowFile(flowFile: File): File? {
        val commands = flowFile.source().use {
            readCommandsUnsafe(it)
        }
        val config = getConfig(commands) ?: return null
        val initFlow = config["initFlow"]
        if (initFlow !is String) return null
        return getInitFlowFile(flowFile, initFlow)
    }

    private fun getInitFlowFile(flowFile: File, initFlow: String): File {
        val initFlowFile = File(initFlow)
        if (initFlowFile.isAbsolute) return initFlowFile
        return flowFile.parentFile.resolve(initFlowFile)
    }

    private fun getConfig(commands: List<MaestroCommand>): Map<String, *>? {
        return commands.firstNotNullOfOrNull { it.applyConfigurationCommand }?.config
    }
}
