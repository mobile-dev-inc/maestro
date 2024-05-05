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

import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import maestro.cli.App
import maestro.cli.CliError
import maestro.cli.DisableAnsiMixin
import maestro.cli.device.Device
import maestro.cli.device.DeviceCreateUtil
import maestro.cli.device.DeviceService
import maestro.cli.device.PickDeviceView
import maestro.cli.report.ReportFormat
import maestro.cli.report.ReporterFactory
import maestro.cli.report.TestDebugReporter
import maestro.cli.runner.TestRunner
import maestro.cli.runner.TestSuiteInteractor
import maestro.cli.runner.resultview.AnsiResultView
import maestro.cli.runner.resultview.PlainTextResultView
import maestro.cli.session.MaestroSessionManager
import maestro.cli.util.PrintUtils
import maestro.orchestra.error.ValidationError
import maestro.orchestra.util.Env.withInjectedShellEnvVars
import maestro.orchestra.workspace.WorkspaceExecutionPlanner
import maestro.orchestra.workspace.WorkspaceExecutionPlanner.ExecutionPlan
import maestro.orchestra.yaml.YamlCommandReader
import okio.buffer
import okio.sink
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.io.path.absolutePathString

@CommandLine.Command(
    name = "test",
    description = [
        "Test a Flow or set of Flows on a local iOS Simulator or Android Emulator"
    ]
)
class TestCommand : Callable<Int> {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.ParentCommand
    private val parent: App? = null

    @CommandLine.Parameters
    private lateinit var flowFile: File

    @Option(
        names = ["-s", "--shards"],
        description = ["Number of parallel shards to distribute tests across"]
    )
    private var shards: Int = 1

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
        description = ["Configures the debug output in this path, instead of default"]
    )
    private var debugOutput: String? = null

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

    private val deviceCreationSemaphore = Semaphore(1)
    private val usedPorts = ConcurrentHashMap<Int, Boolean>()
    private val activeDevices = ConcurrentSet<String>()

    private fun isWebFlow(): Boolean {
        if (!flowFile.isDirectory) {
            val config = YamlCommandReader.readConfig(flowFile.toPath())
            return Regex("http(s?)://").containsMatchIn(config.appId)
        }

        return false
    }

    override fun call(): Int {
        if (parent?.platform != null) {
            throw CliError("--platform option was deprecated. You can remove it to run your test.")
        }

        val executionPlan = try {
            WorkspaceExecutionPlanner.plan(
                flowFile.toPath().toAbsolutePath(),
                includeTags,
                excludeTags
            )
        } catch (e: ValidationError) {
            throw CliError(e.message)
        }

        env = env.withInjectedShellEnvVars()

        TestDebugReporter.install(debugOutputPathAsString = debugOutput)
        val debugOutputPath = TestDebugReporter.getDebugOutputPath()

        return handleSessions(debugOutputPath, executionPlan)
    }

    private fun handleSessions(debugOutputPath: Path, plan: ExecutionPlan): Int = runBlocking {
        val deviceIds = (if (isWebFlow())
            "chromium".also {
                PrintUtils.warn("Web support is an experimental feature and may be removed in future versions.\n")
            }
        else parent?.deviceId)
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        activeDevices.addAll(DeviceService.listConnectedDevices().map {
            it.instanceId
        }.toMutableSet())
        val effectiveShards = shards.coerceAtMost(plan.flowsToRun.size)
        val chunkPlans = plan.flowsToRun
            .withIndex()
            .groupBy { it.index % shards }
            .map { (shardIndex, files) ->
                ExecutionPlan(
                    files.map { it.value },
                    if (shardIndex == 0) plan.sequence else null
                )
            }

        val barrier = CountDownLatch(effectiveShards)

        (0 until effectiveShards).map { shardIndex ->
            async(Dispatchers.IO) {
                val driverHostPort = (9001..9128).shuffled().first { port ->
                    usedPorts.putIfAbsent(port, true) == null
                }

                // Acquire lock to execute device creation block
                deviceCreationSemaphore.acquire()

                val deviceId = deviceIds.getOrNull(shardIndex) // 1. Reuse existing device if deviceId is provided
                        ?: activeDevices.elementAtOrNull(shardIndex) // 2. Reuse existing device if connected device found
                        ?: run { // 3. Create a new device
                            val deviceCreated = createDevice(shardIndex)
                            launchDevice(deviceCreated, driverHostPort, activeDevices).also {
                                // Add to active devices as soon as bootload with launchDevice finishes
                                activeDevices.add(it)
                            }
                        }
                // Release lock if device ID was obtained from the connected devices
                deviceCreationSemaphore.release()
                // Signal that this thread has reached the barrier
                barrier.countDown()
                // Wait for all threads/shards to complete device creation before proceeding
                barrier.await()

                MaestroSessionManager.newSession(
                    host = parent?.host,
                    port = parent?.port,
                    driverHostPort = driverHostPort,
                    deviceId = deviceId
                ) { session ->
                    val maestro = session.maestro
                    val device = session.device

                    if (flowFile.isDirectory || format != ReportFormat.NOOP) {
                        if (continuous) {
                            throw CommandLine.ParameterException(
                                commandSpec.commandLine(),
                                "Continuous mode is not supported for directories. $flowFile is a directory",
                            )
                        }

                        val suiteResult = TestSuiteInteractor(
                            maestro = maestro,
                            device = device,
                            reporter = ReporterFactory.buildReporter(format, testSuiteName),
                        ).runTestSuite(
                            executionPlan = chunkPlans[shardIndex],
                            env = env,
                            reportOut = format.fileExtension
                                ?.let { extension ->
                                    (output ?: File("report$extension"))
                                        .sink()
                                        .buffer()
                                },
                            debugOutputPath = debugOutputPath
                        )

                        TestDebugReporter.deleteOldFiles()
                        if (suiteResult.passed) {
                            0
                        } else {
                            printExitDebugMessage()
                            1
                        }
                    } else {
                        if (continuous) {
                            TestDebugReporter.deleteOldFiles()
                            TestRunner.runContinuous(maestro, device, flowFile, env)
                        } else {
                            val resultView =
                                if (DisableAnsiMixin.ansiEnabled) AnsiResultView()
                                else PlainTextResultView()
                            val resultSingle = TestRunner.runSingle(
                                maestro,
                                device,
                                flowFile,
                                env,
                                resultView,
                                debugOutputPath
                            )
                            if (resultSingle == 1) {
                                printExitDebugMessage()
                            }
                            TestDebugReporter.deleteOldFiles()
                            return@newSession resultSingle
                        }
                    }
                }
            }
        }.awaitAll().sum()
    }

    private fun createDevice(shardIndex: Int): Device.AvailableForLaunch {
        PrintUtils.clearConsole()
        val options = PickDeviceView.requestDeviceOptions()

        return DeviceCreateUtil.getOrCreateDevice(
            options.platform,
            options.osVersion,
            null,
            null,
            true,
            shardIndex
        )
    }

    private fun launchDevice(
        deviceAvailable: Device.AvailableForLaunch,
        deviceHostPort: Int,
        connectedDevices: MutableSet<String>,
    ) = DeviceService.startDevice(deviceAvailable, deviceHostPort, connectedDevices).instanceId

    private fun printExitDebugMessage() {
        println()
        println("==== Debug output (logs & screenshots) ====")
        PrintUtils.message(TestDebugReporter.getDebugOutputPath().absolutePathString())
    }
}
