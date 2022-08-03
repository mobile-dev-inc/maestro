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
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroConfig
import maestro.orchestra.NoInputException
import maestro.orchestra.SyntaxError
import java.io.File

object YamlCommandReader {

    private val YAML = YAMLFactory(ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    })

    // If it exists, automatically resolves the initFlow file and inlines the commands into the config
    fun readCommands(flowFile: File): List<MaestroCommand> = mapParsingErrors {
        val parser = YAML.createParser(flowFile)
        val config = parser.readValueAs(YamlConfig::class.java)
        val commands = parser.readValueAs<List<YamlFluentCommand>>(
            object : TypeReference<List<YamlFluentCommand>>() {}
        ).map { it.toCommand() }
        listOfNotNull(config.toCommand(flowFile), *commands.toTypedArray())
    }

    // Files to watch for changes. Includes any referenced files.
    fun getWatchFiles(flowFile: File): List<File> = mapParsingErrors{
        val parser = YAML.createParser(flowFile)
        val config = parser.readValueAs(YamlConfig::class.java)
        val initFlowFile = config.getInitFlowFile(flowFile)
        listOfNotNull(
            flowFile,
            initFlowFile,
        ).filter { it.parentFile.isDirectory }
    }

    fun getConfig(commands: List<MaestroCommand>): MaestroConfig? {
        return commands.firstNotNullOfOrNull { it.applyConfigurationCommand }?.config
    }

    private fun <T> mapParsingErrors(block: () -> T): T {
        try {
            return block()
        } catch (e: JsonMappingException) {
            val message = e.message ?: throw SyntaxError("Invalid syntax")

            when {
                message.contains("Unrecognized field") -> throw SyntaxError(message)
                message.contains("Cannot construct instance") -> throw SyntaxError(message)
                message.contains("Cannot deserialize") -> throw SyntaxError(message)
                message.contains("No content to map") -> throw NoInputException
                else -> throw SyntaxError(message)
            }
        }
    }
}
