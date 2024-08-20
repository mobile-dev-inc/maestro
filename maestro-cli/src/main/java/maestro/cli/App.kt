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

package maestro.cli

import maestro.cli.analytics.Analytics
import maestro.cli.command.*
import maestro.cli.command.DownloadSamplesCommand
import maestro.cli.command.LogoutCommand
import maestro.cli.report.FlowAIOutput
import maestro.cli.report.HtmlAITestSuiteReporter
import maestro.cli.report.SingleScreenFlowAIOutput
import maestro.cli.update.Updates
import maestro.cli.util.AndroidEnvUtils
import maestro.cli.util.EnvUtils
import maestro.cli.util.ErrorReporter
import maestro.cli.util.IOSEnvUtils
import maestro.cli.view.box
import maestro.debuglog.DebugLogStore
import maestro.orchestra.ai.Defect
import okio.sink
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.Properties
import kotlin.random.Random
import kotlin.system.exitProcess

@Command(
    name = "maestro",
    subcommands = [
        TestCommand::class,
        CloudCommand::class,
        RecordCommand::class,
        UploadCommand::class,
        PrintHierarchyCommand::class,
        QueryCommand::class,
        DownloadSamplesCommand::class,
        LoginCommand::class,
        LogoutCommand::class,
        BugReportCommand::class,
        StudioCommand::class,
        StartDeviceCommand::class
    ]
)
class App {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.Mixin
    var showHelpMixin: ShowHelpMixin? = null

    @Option(names = ["-v", "--version"], versionHelp = true, description = ["Display CLI version"])
    var requestedVersion: Boolean? = false

    @Option(names = ["-p", "--platform"], hidden = true)
    var platform: String? = null

    @Option(names = ["--host"], hidden = true)
    var host: String? = null

    @Option(names = ["--port"], hidden = true)
    var port: Int? = null

    @Option(
        names = ["--device", "--udid"],
        description = ["(Optional) Device ID to run on explicitly, can be a comma separated list of IDs: --device \"Emulator_1,Emulator_2\" "]
    )
    var deviceId: String? = null
}

private fun printVersion() {
    val props = App::class.java.classLoader.getResourceAsStream("version.properties").use {
        Properties().apply { load(it) }
    }

    println(props["version"])
}

fun main(args: Array<String>) {
    // Disable icon in Mac dock
    // https://stackoverflow.com/a/17544259
    System.setProperty("apple.awt.UIElement", "true")

    val reporter = HtmlAITestSuiteReporter()
    val flowOutputs = listOf(
        FlowAIOutput(
            flowName = "Flow 1",
            flowFile = File("flow1.yaml"),
            screenOutputs = mutableListOf(
                SingleScreenFlowAIOutput(
                    screenshotPath = File("/Users/bartek/Desktop/example_1.png"),
                    defects = mutableListOf(
                        Defect(reasoning = "Lorem impsum dolor sit amet".repeat(20), category = "Category 1"),
                        Defect(reasoning = "Defect 2".repeat(5), category = "Category 2"),
                    )
                ),
                SingleScreenFlowAIOutput(
                    screenshotPath = File("/Users/bartek/Desktop/example_2.png"),
                    defects = mutableListOf(
                        Defect(reasoning = "Lorem impsum dolor sit amet".repeat(20), category = "Category 1"),
                        Defect(reasoning = "Defect 2".repeat(5), category = "Category 2"),
                    )
                ),
            ),
        ),
        FlowAIOutput(
            flowName = "Flow 2",
            flowFile = File("flow2.yaml"),
            screenOutputs = mutableListOf(
                SingleScreenFlowAIOutput(
                    screenshotPath = File("/Users/bartek/Desktop/example_3.png"),
                    defects = mutableListOf(
                        Defect(reasoning = "Lorem impsum dolor sit amet".repeat(20), category = "Category 1"),
                        Defect(reasoning = "Defect 2".repeat(5), category = "Category 2"),
                    )
                ),
            ),
        ),
    )
    flowOutputs.forEach { flowAIoutput ->
        val file = File("/Users/bartek/Desktop/${flowAIoutput.flowName}.html")
        file.delete()
        val created = file.createNewFile()
        println("Generating report for ${flowAIoutput.flowName}, created: $created")
        reporter.report(flowAIoutput, file.sink())
    }

    return

    Analytics.maybeMigrate()
    Analytics.maybeAskToEnableAnalytics()

    Dependencies.install()
    Updates.fetchUpdatesAsync()
    Analytics.maybeUploadAnalyticsAsync()

    val commandLine = CommandLine(App())
        .setUsageHelpWidth(160)
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setExecutionStrategy(DisableAnsiMixin::executionStrategy)
        .setExecutionExceptionHandler { ex, cmd, cmdParseResult ->
            runCatching { ErrorReporter.report(ex, cmdParseResult) }

            val message = if (ex is CliError) {
                ex.message
            } else {
                ex.stackTraceToString()
            }

            println()

            // make errors red
            cmd.colorScheme =  CommandLine.Help.ColorScheme.Builder()
                .errors(CommandLine.Help.Ansi.Style.fg_red)
                .build()

            cmd.err.println(
                cmd.colorScheme.errorText(message)
            )

            1
        }

    val exitCode = commandLine
        .execute(*args)

    DebugLogStore.finalizeRun()

    val newVersion = Updates.checkForUpdates()
    if (newVersion != null) {
        System.err.println()
        System.err.println(
            ("A new version of the Maestro CLI is available ($newVersion). Upgrade command:\n" +
                    "curl -Ls \"https://get.maestro.mobile.dev\" | bash").box()
        )
    }

    if (commandLine.isVersionHelpRequested) {
        printVersion()
        exitProcess(0)
    }

    exitProcess(exitCode)
}
