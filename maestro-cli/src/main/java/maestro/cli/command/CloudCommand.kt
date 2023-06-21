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

import maestro.cli.CliError
import maestro.cli.DisableAnsiMixin
import maestro.cli.api.ApiClient
import maestro.cli.cloud.CloudInteractor
import maestro.cli.report.ReportFormat
import maestro.cli.util.PrintUtils
import maestro.orchestra.util.Env.withInjectedShellEnvVars
import maestro.orchestra.workspace.WorkspaceExecutionPlanner
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "cloud",
    description = [
        "Test a Flow or set of Flows on Maestro Cloud (https://cloud.mobile.dev)",
        "Provide your application file and a folder with Maestro flows to run them in parallel on multiple devices in Maestro Cloud",
        "By default, the command will block until all analyses have completed. You can use the --async flag to run the command asynchronously and exit immediately.",
    ]
)
class CloudCommand : Callable<Int> {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.Parameters(hidden = true, arity = "0..2", description = ["App file and/or Flow file i.e <appFile> <flowFile>"])
    private lateinit var files: List<File>

    @Option(names = ["--appFile"], description = ["App binary to run your Flows against"])
    private var appFile: File? = null

    @Option(names = ["--workspace"], description = ["Flow file or workspace directory"])
    private lateinit var flowFile: File

    @Option(order = 0, names = ["--apiKey"], description = ["API key"])
    private var apiKey: String? = null

    @Option(order = 1, names = ["--apiUrl"], description = ["API base URL"])
    private var apiUrl: String = "https://api.mobile.dev"

    @Option(order = 2, names = ["--mapping"], description = ["dSYM file (iOS) or Proguard mapping file (Android)"])
    private var mapping: File? = null

    @Option(order = 3, names = ["--repoOwner"], description = ["Repository owner (ie: GitHub organization or user slug)"])
    private var repoOwner: String? = null

    @Option(order = 4, names = ["--repoName"], description = ["Repository name (ie: GitHub repo slug)"])
    private var repoName: String? = null

    @Option(order = 5, names = ["--branch"], description = ["The branch this upload originated from"])
    private var branch: String? = null

    @Option(order = 6, names = ["--commitSha"], description = ["The commit SHA of this upload"])
    private var commitSha: String? = null

    @Option(order = 7, names = ["--pullRequestId"], description = ["The ID of the pull request this upload originated from"])
    private var pullRequestId: String? = null

    @Option(order = 8, names = ["-e", "--env"], description = ["Environment variables to inject into your Flows"])
    private var env: Map<String, String> = emptyMap()

    @Option(order = 9, names = ["--name"], description = ["Name of the upload"])
    private var uploadName: String? = null

    @Option(order = 10, names = ["--async"], description = ["Run the upload asynchronously"])
    private var async: Boolean = false

    @Option(order = 11, names = ["--android-api-level"], description = ["Android API level to run your flow against"])
    private var androidApiLevel: Int? = null

    @Option(
        order = 12,
        names = ["--include-tags"],
        description = ["List of tags that will remove the Flows that does not have the provided tags"],
        split = ",",

        )
    private var includeTags: List<String> = emptyList()

    @Option(
        order = 13,
        names = ["--exclude-tags"],
        description = ["List of tags that will remove the Flows containing the provided tags"],
        split = ",",
    )
    private var excludeTags: List<String> = emptyList()

    @Option(
        order = 14,
        names = ["--format"],
        description = ["Test report format (default=\${DEFAULT-VALUE}): \${COMPLETION-CANDIDATES}"],
    )
    private var format: ReportFormat = ReportFormat.NOOP

    @Option(
        names = ["--test-suite-name"],
        description = ["Test suite name"],
    )
    private var testSuiteName: String? = null

    @Option(
        order = 15,
        names = ["--output"],
        description = ["File to write report into (default=report.xml)"],
    )
    private var output: File? = null

    @Option(order = 16, names = ["--ios-version"], description = ["iOS version to run your flow against"])
    private var iOSVersion: String? = null

    @Option(order = 17, names = ["--appBinaryId"], description = ["The ID of the app binary previously uploaded to Maestro Cloud"])
    private var appBinaryId: String? = null

    @Option(hidden = true, names = ["--fail-on-cancellation"], description = ["Fail the command if the upload is marked as cancelled"])
    private var failOnCancellation: Boolean = false

    override fun call(): Int {

        validateFiles()
        validateWorkSpace()

        // Upload
        return CloudInteractor(
            client = ApiClient(apiUrl),
            failOnTimeout = failOnCancellation,
        ).upload(
            async = async,
            flowFile = flowFile,
            appFile = appFile,
            mapping = mapping,
            env = env.withInjectedShellEnvVars(),
            uploadName = uploadName,
            repoOwner = repoOwner,
            repoName = repoName,
            branch = branch,
            commitSha = commitSha,
            pullRequestId = pullRequestId,
            apiKey = apiKey,
            androidApiLevel = androidApiLevel,
            iOSVersion = iOSVersion,
            appBinaryId = appBinaryId,
            includeTags = includeTags,
            excludeTags = excludeTags,
            reportFormat = format,
            reportOutput = output,
            failOnCancellation = failOnCancellation,
            testSuiteName = testSuiteName
        )
    }

    private fun validateWorkSpace() {
        try {
            PrintUtils.message("Evaluating workspace...")
            WorkspaceExecutionPlanner
                .plan(
                    input = flowFile.toPath().toAbsolutePath(),
                    includeTags = includeTags,
                    excludeTags = excludeTags,
                )
        } catch (e: Exception) {
            throw CliError("Upload aborted. Received error when evaluating workspace: ${e.message}")
        }
    }

    private fun validateFiles() {

        // Maintains backwards compatibility for this syntax: maestro cloud <appFile> <workspace>
        // App file can be optional now
        if (this::files.isInitialized) {
            when (files.size) {
                2 -> {
                    appFile = files[0]
                    flowFile = files[1]
                }
                1 -> {
                    flowFile = files[0]
                }
            }
        }
        if (appFile == null && appBinaryId == null) throw CliError("Missing required parameter for option '--appFile' or '--appBinaryId'")
        if (!this::flowFile.isInitialized) throw CliError("Missing required parameter for option '--workspace'")
    }

}
