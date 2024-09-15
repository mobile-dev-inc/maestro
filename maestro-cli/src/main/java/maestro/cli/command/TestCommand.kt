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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import maestro.Maestro
import maestro.cli.App
import maestro.cli.CliError
import maestro.cli.DisableAnsiMixin
import maestro.cli.ShowHelpMixin
import maestro.cli.device.Device
import maestro.cli.device.DeviceService
import maestro.cli.model.TestExecutionSummary
import maestro.cli.report.ReportFormat
import maestro.cli.report.ReporterFactory
import maestro.cli.report.TestDebugReporter
import maestro.cli.runner.TestRunner
import maestro.cli.runner.TestSuiteInteractor
import maestro.cli.runner.resultview.AnsiResultView
import maestro.cli.runner.resultview.PlainTextResultView
import maestro.cli.session.MaestroSessionManager
import maestro.cli.util.PrintUtils
import maestro.cli.view.box
import maestro.orchestra.error.ValidationError
import maestro.orchestra.util.Env.withDefaultEnvVars
import maestro.orchestra.util.Env.withInjectedShellEnvVars
import maestro.orchestra.workspace.WorkspaceExecutionPlanner
import maestro.orchestra.workspace.WorkspaceExecutionPlanner.ExecutionPlan
import maestro.orchestra.yaml.YamlCommandReader
import maestro.utils.isSingleFile
import okio.sink
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.math.roundToInt
import maestro.cli.device.Platform

@CommandLine.Command(
    name = "test",
    description = ["Test a Flow or set of Flows on a local iOS Simulator or Android Emulator"],
)
class TestCommand : Callable<Int> {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.Mixin
    var showHelpMixin: ShowHelpMixin? = null

    @CommandLine.ParentCommand
    private val parent: App? = null

    @CommandLine.Parameters(description = ["One or more flow files or folders containing flow files"], arity = "1..*")
    private var flowFiles: Set<File> = emptySet()

    @Option(
        names = ["--config"],
        description = ["Optional YAML configuration file for the workspace. If not provided, Maestro will look for a config.yaml file in the workspace's root directory."]
    )
    private var configFile: File? = null

    @Option(
        names = ["-s", "--shards"],
        description = ["Number of parallel shards to distribute tests across"],
    )
    @Deprecated("Use --shard-split or --shard-all instead")
    private var legacyShardCount: Int? = null

    @Option(
        names = ["--shard-split"],
        description = ["Run the tests across N connected devices, splitting the tests evenly across them"],
    )
    private var shardSplit: Int? = null

    @Option(
        names = ["--shard-all"],
        description = ["Run all the tests across N connected devices"],
    )
    private var shardAll: Int? = null

    @Option(names = ["-c", "--continuous"])
    private var continuous: Boolean = false

    @Option(names = ["-e", "--env"])
    private var env: Map<String, String> = emptyMap()

    @Option(
        names = ["--format"],
        description = ["Test report format (default=\${DEFAULT-VALUE}): \${COMPLETION-CANDIDATES}"],
    )
    private var format: ReportFormat = ReportFormat.NOOP

    @Option(
        names = ["--test-suite-name"],
        description = ["Test suite name"],
    )
    private var testSuiteName: String? = null

    @Option(names = ["--output"])
    private var output: File? = null

    @Option(
        names = ["--debug-output"],
        description = ["Configures the debug output in this path, instead of default"],
    )
    private var debugOutput: String? = null

    @Option(
        names = ["--flatten-debug-output"],
        description = ["All file outputs from the test case are created in the folder without subfolders or timestamps for each run. It can be used with --debug-output. Useful for CI."]
    )
    private var flattenDebugOutput: Boolean = false

    @Option(
        names = ["--include-tags"],
        description = ["List of tags that will remove the Flows that does not have the provided tags"],
        split = ",",
    )
    private var includeTags: List<String> = emptyList()

    @Option(
        names = ["--exclude-tags"],
        description = ["List of tags that will remove the Flows containing the provided tags"],
        split = ",",
    )
    private var excludeTags: List<String> = emptyList()

    @CommandLine.Spec
    lateinit var commandSpec: CommandLine.Model.CommandSpec

    private val usedPorts = ConcurrentHashMap<Int, Boolean>()
    private val logger = LoggerFactory.getLogger(TestCommand::class.java)

    private fun isWebFlow(): Boolean {
        if (flowFiles.isSingleFile) {
            val config = YamlCommandReader.readConfig(flowFiles.first().toPath())
            return Regex("http(s?)://").containsMatchIn(config.appId)
        }

        return false
    }

    override fun call(): Int {
        TestDebugReporter.install(
            debugOutputPathAsString = debugOutput,
            flattenDebugOutput = flattenDebugOutput,
            printToConsole = parent?.verbose == true,
        )

        if (shardSplit != null && shardAll != null) {
            throw CliError("Options --shard-split and --shard-all are mutually exclusive.")
        }

        @Suppress("DEPRECATION")
        if (legacyShardCount != null) {
            PrintUtils.warn("--shards option is deprecated and will be removed in the next Maestro version. Use --shard-split or --shard-all instead.")
            shardSplit = legacyShardCount
        }

        if (configFile != null && configFile?.exists()?.not() == true) {
            throw CliError("The config file ${configFile?.absolutePath} does not exist.")
        }

        val executionPlan = try {
            WorkspaceExecutionPlanner.plan(
                input = flowFiles.map { it.toPath().toAbsolutePath() }.toSet(),
                includeTags = includeTags,
                excludeTags = excludeTags,
                config = configFile?.toPath()?.toAbsolutePath(),
            )
        } catch (e: ValidationError) {
            throw CliError(e.message)
        }

        val debugOutputPath = TestDebugReporter.getDebugOutputPath()

        return handleSessions(debugOutputPath, executionPlan)
    }

    private fun handleSessions(debugOutputPath: Path, plan: ExecutionPlan): Int = runBlocking(Dispatchers.IO) {
        val requestedShards = shardSplit ?: shardAll ?: 1
        if (requestedShards > 1 && plan.sequence.flows.isNotEmpty()) {
            error("Cannot run sharded tests with sequential execution")
        }

        val onlySequenceFlows = plan.sequence.flows.isNotEmpty() && plan.flowsToRun.isEmpty() // An edge case

        val connectedDevices = DeviceService.listConnectedDevices()
        val availableDevicesIds = connectedDevices.map { it.instanceId }.toSet()

        val deviceIds = getPassedOptionsDeviceIds()
            .filter { device ->
                if (device !in availableDevicesIds) {
                    throw CliError("Device $device was requested, but it is not connected.")
                } else {
                    true
                }
            }
            .ifEmpty {
                connectedDevices
                    .filter { it.platform == Platform.fromString(parent?.platform) }
                    .map { it.instanceId }.toSet()
            }
            .toList()

        val missingDevices = requestedShards - deviceIds.size
        if (missingDevices > 0) {
            PrintUtils.warn("Want to use ${deviceIds.size} devices, which is not enough to run $requestedShards shards. Missing $missingDevices device(s).")
            throw CliError("Not enough devices connected ($missingDevices) to run the requested number of shards ($requestedShards).")
        }

        val effectiveShards = when {

            onlySequenceFlows -> 1

            shardAll == null -> requestedShards.coerceAtMost(plan.flowsToRun.size)

            shardSplit == null -> requestedShards.coerceAtMost(deviceIds.size)

            else -> 1
        }

        val warning = "Requested $requestedShards shards, " +
                "but it cannot be higher than the number of flows (${plan.flowsToRun.size}). " +
                "Will use $effectiveShards shards instead."
        if (shardAll == null && requestedShards > plan.flowsToRun.size) PrintUtils.warn(warning)

        val chunkPlans = makeChunkPlans(plan, effectiveShards, onlySequenceFlows)

        val flowCount = if (onlySequenceFlows) plan.sequence.flows.size else plan.flowsToRun.size
        val message = when {
            shardAll != null -> "Will run $effectiveShards shards, with all $flowCount flows in each shard"
            shardSplit != null -> {
                val flowsPerShard = (flowCount.toFloat() / effectiveShards).roundToInt()
                val isApprox = flowCount % effectiveShards != 0
                val prefix = if (isApprox) "approx. " else ""
                "Will split $flowCount flows across $effectiveShards shards (${prefix}$flowsPerShard flows per shard)"
            }

            else -> null
        }
        message?.let { PrintUtils.info(it) }

        val results = (0 until effectiveShards).map { shardIndex ->
            async(Dispatchers.IO + CoroutineName("shard-$shardIndex")) {
                runShardSuite(
                    effectiveShards = effectiveShards,
                    deviceIds = deviceIds,
                    shardIndex = shardIndex,
                    chunkPlans = chunkPlans,
                    debugOutputPath = debugOutputPath,
                )
            }
        }.awaitAll()

        val passed = results.sumOf { it.first ?: 0 }
        val total = results.sumOf { it.second ?: 0 }
        val suites = results.mapNotNull { it.third }

        suites.mergeSummaries()?.saveReport()

        if (effectiveShards > 1) printShardsMessage(passed, total, suites)
        if (passed == total) 0 else 1
    }

    private fun runShardSuite(
        effectiveShards: Int,
        deviceIds: List<String>,
        shardIndex: Int,
        chunkPlans: List<ExecutionPlan>,
        debugOutputPath: Path,
    ): Triple<Int?, Int?, TestExecutionSummary?> {
        val driverHostPort = selectPort(effectiveShards)
        val deviceId = deviceIds[shardIndex]

        logger.info("[shard ${shardIndex + 1}] Selected device $deviceId using port $driverHostPort")

        return MaestroSessionManager.newSession(
            host = parent?.host,
            port = parent?.port,
            driverHostPort = driverHostPort,
            deviceId = deviceId,
            platform = parent?.platform,
        ) { session ->
            val maestro = session.maestro
            val device = session.device

            val isReplicatingSingleFile = shardAll != null && effectiveShards > 1 && flowFiles.isSingleFile
            val isMultipleFiles = flowFiles.isSingleFile.not()
            val isAskingForReport = format != ReportFormat.NOOP
            if (isMultipleFiles || isAskingForReport || isReplicatingSingleFile) {
                if (continuous) {
                    throw CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        "Continuous mode is not supported when running multiple flows. (${flowFiles.joinToString(", ")})",
                    )
                }
                runMultipleFlows(maestro, device, chunkPlans, shardIndex, debugOutputPath)
            } else {
                val flowFile = flowFiles.first()
                if (continuous) {
                    if (!flattenDebugOutput) {
                        TestDebugReporter.deleteOldFiles()
                    }
                    TestRunner.runContinuous(maestro, device, flowFile, env)
                } else {
                    runSingleFlow(maestro, device, flowFile, debugOutputPath)
                }
            }
        }
    }

    private fun selectPort(effectiveShards: Int): Int =
        if (effectiveShards == 1) parent?.port ?: 7001
        else (7001..7128).shuffled().find { port ->
            usedPorts.putIfAbsent(port, true) == null
        } ?: error("No available ports found")

    private fun runSingleFlow(
        maestro: Maestro,
        device: Device?,
        flowFile: File,
        debugOutputPath: Path,
    ): Triple<Int, Int, Nothing?> {
        val resultView =
            if (DisableAnsiMixin.ansiEnabled) AnsiResultView()
            else PlainTextResultView()

        env = env
            .withInjectedShellEnvVars()
            .withDefaultEnvVars(flowFile)

        val resultSingle = TestRunner.runSingle(
            maestro = maestro,
            device = device,
            flowFile = flowFile,
            env = env,
            resultView = resultView,
            debugOutputPath = debugOutputPath,
        )

        if (resultSingle == 1) {
            printExitDebugMessage()
        }

        if (!flattenDebugOutput) {
            TestDebugReporter.deleteOldFiles()
        }

        val result = if (resultSingle == 0) 1 else 0
        return Triple(result, 1, null)
    }

    private fun runMultipleFlows(
        maestro: Maestro,
        device: Device?,
        chunkPlans: List<ExecutionPlan>,
        shardIndex: Int,
        debugOutputPath: Path
    ): Triple<Int?, Int?, TestExecutionSummary> {
        val suiteResult = TestSuiteInteractor(
            maestro = maestro,
            device = device,
            shardIndex = if (chunkPlans.size == 1) null else shardIndex,
            reporter = ReporterFactory.buildReporter(format, testSuiteName),
        ).runTestSuite(
            executionPlan = chunkPlans[shardIndex],
            env = env,
            reportOut = null,
            debugOutputPath = debugOutputPath
        )

        if (!flattenDebugOutput) {
            TestDebugReporter.deleteOldFiles()
        }
        return Triple(suiteResult.passedCount, suiteResult.totalTests, suiteResult)
    }

    private fun makeChunkPlans(
        plan: ExecutionPlan,
        effectiveShards: Int,
        onlySequenceFlows: Boolean,
    ) = when {
        onlySequenceFlows -> listOf(plan) // We only want to run sequential flows in this case.
        shardAll != null -> (0 until effectiveShards).reversed().map { plan.copy() }
        else -> plan.flowsToRun
            .withIndex()
            .groupBy { it.index % effectiveShards }
            .map { (_, files) ->
                val flowsToRun = files.map { it.value }
                ExecutionPlan(flowsToRun, plan.sequence)
            }
    }

    private fun getPassedOptionsDeviceIds(): List<String> {
        val arguments = if (isWebFlow()) {
            PrintUtils.warn("Web support is an experimental feature and may be removed in future versions.\n")
            "chromium"
        } else parent?.deviceId
        val deviceIds = arguments
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return deviceIds
    }

    private fun printExitDebugMessage() {
        println()
        println("==== Debug output (logs & screenshots) ====")
        PrintUtils.message(TestDebugReporter.getDebugOutputPath().absolutePathString())
    }

    private fun printShardsMessage(passedTests: Int, totalTests: Int, shardResults: List<TestExecutionSummary>) {
        val lines = listOf("Passed: $passedTests/$totalTests") +
                shardResults.mapIndexed { _, result ->
                    "[ ${result.suites.first().deviceName} ] - ${result.passedCount ?: 0}/${result.totalTests ?: 0}"
                }
        PrintUtils.message(lines.joinToString("\n").box())
    }

    private fun TestExecutionSummary.saveReport() {
        val reporter = ReporterFactory.buildReporter(format, testSuiteName)

        format.fileExtension?.let { extension ->
            (output ?: File("report$extension")).sink()
        }?.also { sink ->
            reporter.report(this, sink)
        }
    }

    private fun List<TestExecutionSummary>.mergeSummaries(): TestExecutionSummary? = reduceOrNull { acc, summary ->
        TestExecutionSummary(
            passed = acc.passed && summary.passed,
            suites = acc.suites + summary.suites,
            passedCount = sumOf { it.passedCount ?: 0 },
            totalTests = sumOf { it.totalTests ?: 0 }
        )
    }
}
