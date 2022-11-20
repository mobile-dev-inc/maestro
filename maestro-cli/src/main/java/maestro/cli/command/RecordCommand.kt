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

import maestro.cli.App
import maestro.cli.CliError
import maestro.cli.api.ApiClient
import maestro.cli.report.ReportFormat
import maestro.cli.runner.ResultView
import maestro.cli.runner.TestRunner
import maestro.cli.util.MaestroFactory
import okio.sink
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "record",
)
class RecordCommand : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: App? = null

    @CommandLine.Parameters
    private lateinit var flowFile: File

    @Option(names = ["-e", "--env"])
    private var env: Map<String, String> = emptyMap()

    @Option(names = ["--format"])
    private var format: ReportFormat = ReportFormat.NOOP

    @Option(names = ["--output"])
    private var output: File? = null

    @CommandLine.Spec
    lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun call(): Int {
        if (!flowFile.exists()) {
            throw CommandLine.ParameterException(
                commandSpec.commandLine(),
                "File not found: $flowFile"
            )
        }

        if (parent?.platform != null) {
            throw CliError("--platform option was deprecated. You can remove it to run your test.")
        }

        val (maestro, device) = MaestroFactory.createMaestro(parent?.host, parent?.port, parent?.deviceId)

        if (flowFile.isDirectory) {
            throw CommandLine.ParameterException(
                commandSpec.commandLine(),
                "Only single Flows are supported by \"maestro record\". $flowFile is a directory.",
            )
        }

        val resultView = ResultView()
        val screenRecording = kotlin.io.path.createTempFile(suffix = ".mp4").toFile()
        val exitCode = screenRecording.sink().use { out ->
            maestro.startScreenRecording(out).use {
                val exitCode = TestRunner.runSingle(maestro, device, flowFile, env, resultView)
                Thread.sleep(3000)
                exitCode
            }
        }
        val frames = resultView.getFrames()
        val videoUrl = ApiClient("").render(screenRecording, frames)

        println(videoUrl)

        return exitCode
    }
}
