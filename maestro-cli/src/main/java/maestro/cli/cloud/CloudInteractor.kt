package maestro.cli.cloud

import maestro.cli.CliError
import maestro.cli.api.ApiClient
import maestro.cli.api.RobinUploadResponse
import maestro.cli.api.DeviceInfo
import maestro.cli.api.UploadStatus
import maestro.cli.api.DeviceConfiguration
import maestro.cli.api.MaestroCloudUploadResponse
import maestro.cli.auth.Auth
import maestro.cli.device.Platform
import maestro.cli.model.RunningFlow
import maestro.cli.model.FlowStatus
import maestro.cli.model.RunningFlows
import maestro.cli.model.TestExecutionSummary
import maestro.cli.report.ReportFormat
import maestro.cli.report.ReporterFactory
import maestro.cli.util.EnvUtils
import maestro.cli.util.FileUtils.isZip
import maestro.cli.util.PrintUtils
import maestro.cli.util.TimeUtils
import maestro.cli.util.WorkspaceUtils
import maestro.cli.view.ProgressBar
import maestro.cli.view.TestSuiteStatusView
import maestro.cli.view.TestSuiteStatusView.TestSuiteViewModel.Companion.toViewModel
import maestro.cli.view.TestSuiteStatusView.uploadUrl
import maestro.cli.view.box
import maestro.utils.TemporaryDirectory
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolute

class CloudInteractor(
    private val client: ApiClient,
    private val auth: Auth = Auth(client),
    private val waitTimeoutMs: Long = TimeUnit.MINUTES.toMillis(30),
    private val minPollIntervalMs: Long = TimeUnit.SECONDS.toMillis(10),
    private val maxPollingRetries: Int = 5,
    private val failOnTimeout: Boolean = true,
) {

    fun upload(
        flowFile: File,
        appFile: File?,
        async: Boolean,
        mapping: File? = null,
        apiKey: String? = null,
        uploadName: String? = null,
        repoOwner: String? = null,
        repoName: String? = null,
        branch: String? = null,
        commitSha: String? = null,
        pullRequestId: String? = null,
        env: Map<String, String> = emptyMap(),
        androidApiLevel: Int? = null,
        iOSVersion: String? = null,
        appBinaryId: String? = null,
        failOnCancellation: Boolean = false,
        includeTags: List<String> = emptyList(),
        excludeTags: List<String> = emptyList(),
        reportFormat: ReportFormat = ReportFormat.NOOP,
        reportOutput: File? = null,
        testSuiteName: String? = null,
        disableNotifications: Boolean = false,
        deviceLocale: String? = null,
        projectId: String? = null,
    ): Int {
        if (appBinaryId == null && appFile == null) throw CliError("Missing required parameter for option '--app-file' or '--app-binary-id'")
        if (!flowFile.exists()) throw CliError("File does not exist: ${flowFile.absolutePath}")
        if (mapping?.exists() == false) throw CliError("File does not exist: ${mapping.absolutePath}")
        if (async && reportFormat != ReportFormat.NOOP) throw CliError("Cannot use --format with --async")

        val authToken = apiKey              // Check for API key
            ?: auth.getCachedAuthToken()    // Otherwise, if the user has already logged in, use the cached auth token
            ?: EnvUtils.maestroCloudApiKey()        // Resolve API key from shell if set
            ?: auth.triggerSignInFlow() // Otherwise, trigger the sign-in flow

        PrintUtils.message("Uploading Flow(s)...")

        TemporaryDirectory.use { tmpDir ->
            val workspaceZip = tmpDir.resolve("workspace.zip")
            WorkspaceUtils.createWorkspaceZip(flowFile.toPath().absolute(), workspaceZip)
            println()
            val progressBar = ProgressBar(20)

            // Binary id or Binary file
            var appFileToSend: File? = null
            if (appFile != null && appBinaryId == null) {
                appFileToSend = if (appFile.isZip()) {
                    appFile
                } else {
                    val archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP)

                    // An awkward API of Archiver that has a different behaviour depending on
                    // whether we call a vararg method or a normal method. The *arrayOf() construct
                    // forces compiler to choose vararg method.
                    @Suppress("RemoveRedundantSpreadOperator")
                    archiver.create(appFile.name + ".zip", tmpDir.toFile(), *arrayOf(appFile.absoluteFile))
                }
            }

            val response = client.upload(
                authToken = authToken,
                appFile = appFileToSend?.toPath(),
                workspaceZip = workspaceZip,
                uploadName = uploadName,
                mappingFile = mapping?.toPath(),
                repoOwner = repoOwner,
                repoName = repoName,
                branch = branch,
                commitSha = commitSha,
                pullRequestId = pullRequestId,
                env = env,
                androidApiLevel = androidApiLevel,
                iOSVersion = iOSVersion,
                appBinaryId = appBinaryId,
                includeTags = includeTags,
                excludeTags = excludeTags,
                disableNotifications = disableNotifications,
                deviceLocale = deviceLocale,
                projectId = projectId,
                progressListener = { totalBytes, bytesWritten ->
                    progressBar.set(bytesWritten.toFloat() / totalBytes.toFloat())
                }
            )

            when (response) {
                is RobinUploadResponse -> {
                    println()
                    val project = requireNotNull(projectId)
                    val appId = response.appId
                    val uploadUrl = uploadUrl(project, appId, client.domain)
                    val deviceMessage = if (response.deviceConfiguration != null) printDeviceInfo(response.deviceConfiguration) else ""
                    val appBinaryIdResponseId = if (appBinaryId != null) response.appBinaryId else null
                    return printMaestroCloudResponse(
                        async,
                        authToken,
                        failOnCancellation,
                        reportFormat,
                        reportOutput,
                        testSuiteName,
                        uploadUrl,
                        deviceMessage,
                        appId,
                        appBinaryIdResponseId,
                        response.uploadId,
                        projectId,
                    )
                }
                is MaestroCloudUploadResponse -> {
                    println()
                    val deviceInfo = response.deviceInfo
                    val teamId = response.teamId
                    val appId = response.appId
                    val uploadId = response.uploadId
                    val appBinaryIdResponse = response.appBinaryId
                    val uploadUrl = uploadUrl(uploadId, teamId, appId, client.domain)
                    val deviceInfoMessage = if (deviceInfo != null) printDeviceInfo(deviceInfo, iOSVersion, androidApiLevel) else ""
                    return printMaestroCloudResponse(
                        async = async,
                        authToken = authToken,
                        failOnCancellation = failOnCancellation,
                        reportFormat= reportFormat,
                        reportOutput = reportOutput,
                        testSuiteName = testSuiteName,
                        uploadUrl = uploadUrl,
                        deviceInfoMessage = deviceInfoMessage,
                        appId = appId,
                        appBinaryIdResponse = appBinaryIdResponse,
                        uploadId = uploadId
                    )
                }
            }
        }
    }

    private fun printMaestroCloudResponse(
        async: Boolean,
        authToken: String,
        failOnCancellation: Boolean,
        reportFormat: ReportFormat,
        reportOutput: File?,
        testSuiteName: String?,
        uploadUrl: String,
        deviceInfoMessage: String,
        appId: String,
        appBinaryIdResponse: String?,
        uploadId: String,
        projectId: String? = null
    ): Int {
        if (async) {
            PrintUtils.message("✅ Upload successful!")

            println(deviceInfoMessage)
            PrintUtils.message("View the results of your upload below:")
            PrintUtils.message(uploadUrl)

            if (appBinaryIdResponse != null) PrintUtils.message("App binary id: $appBinaryIdResponse")

            return 0
        } else {

            println(deviceInfoMessage)

            PrintUtils.message(
                "Visit the web console for more details about the upload: $uploadUrl"
            )

            if (appBinaryIdResponse != null) PrintUtils.message("App binary id: $appBinaryIdResponse")

            PrintUtils.message("Waiting for analyses to complete...")
            println()

            return waitForCompletion(
                authToken = authToken,
                uploadId = uploadId,
                appId = appId,
                failOnCancellation = failOnCancellation,
                reportFormat = reportFormat,
                reportOutput = reportOutput,
                testSuiteName = testSuiteName,
                uploadUrl = uploadUrl,
                projectId = projectId,
            )
        }
    }

    private fun printDeviceInfo(deviceInfo: DeviceInfo, iOSVersion: String?, androidApiLevel: Int?): String {

        val platform = Platform.fromString(deviceInfo.platform)

        val line1 = "Maestro Cloud device specs:\n* ${deviceInfo.displayInfo} - ${deviceInfo.deviceLocale}"
        val line2 = "To change OS version use this option: ${if (platform == Platform.IOS) "--ios-version=<version>" else "--android-api-level=<version>"}"
        val line3 = "To change device locale use this option: --device-locale=<device_locale>"

        val version = when(platform) {
            Platform.ANDROID -> "${androidApiLevel ?: 30}" // todo change with constant from DeviceConfigAndroid
            Platform.IOS -> "${iOSVersion ?: 15}" // todo change with constant from DeviceConfigIos
            else -> return ""
        }

        val line4 = "To create a similar device locally, run: `maestro start-device --platform=${platform.toString().lowercase()} --os-version=$version --device-locale=${deviceInfo.deviceLocale}`"
        return "$line1\n\n$line2\n\n$line3\n\n$line4".box()
    }

    private fun printDeviceInfo(deviceConfiguration: DeviceConfiguration): String {
        val platform = Platform.fromString(deviceConfiguration.platform)

        val line1 = "Maestro Cloud device specs:\n* ${deviceConfiguration.displayInfo} - ${deviceConfiguration.deviceLocale}"
        val line2 = "To change OS version use this option: ${if (platform == Platform.IOS) "--ios-version=<version>" else "--android-api-level=<version>"}"
        val line3 = "To change device locale use this option: --device-locale=<device_locale>"

        val version = deviceConfiguration.osVersion

        val line4 = "To create a similar device locally, run: `maestro start-device --platform=${platform.toString().lowercase()} --os-version=$version --device-locale=${deviceConfiguration.deviceLocale}`"
        return "$line1\n\n$line2\n\n$line3\n\n$line4".box()
    }


    private fun waitForCompletion(
        authToken: String,
        uploadId: String,
        appId: String,
        failOnCancellation: Boolean,
        reportFormat: ReportFormat,
        reportOutput: File?,
        testSuiteName: String?,
        uploadUrl: String,
        projectId: String?
    ): Int {
        val startTime = System.currentTimeMillis()

        val runningFlows = RunningFlows()

        var pollingInterval = minPollIntervalMs
        var retryCounter = 0
        do {
            val upload = try {
                client.uploadStatus(authToken, uploadId, projectId)
            } catch (e: ApiClient.ApiException) {
                if (e.statusCode == 429) {
                    // back off through extending sleep duration with 25%
                    pollingInterval = (pollingInterval * 1.25).toLong()
                    Thread.sleep(pollingInterval)
                    continue
                }

                if (e.statusCode == 500 || e.statusCode == 502 || e.statusCode == 404) {
                    if (++retryCounter <= maxPollingRetries) {
                        // retry on 500
                        Thread.sleep(pollingInterval)
                        continue
                    }
                }

                throw CliError("Failed to fetch the status of an upload $uploadId. Status code = ${e.statusCode}")
            }

            val flows = upload.flows
            for (uploadFlowResult in flows) {
                if (runningFlows.flows.none { it.name == uploadFlowResult.name }) {
                    runningFlows.flows.add(RunningFlow(name = uploadFlowResult.name))
                }
                val runningFlow = runningFlows.flows.find { it.name == uploadFlowResult.name } ?: continue
                runningFlow.status = uploadFlowResult.status
                when (runningFlow.status) {
                    UploadStatus.Status.PENDING -> { /* do nothing */ }
                    UploadStatus.Status.RUNNING -> {
                        if (runningFlow.startTime == null) {
                            runningFlow.startTime = System.currentTimeMillis()
                        }
                    }
                    else -> {
                        if (runningFlow.duration == null) {
                            runningFlow.duration = TimeUtils.durationInSeconds(startTimeInMillis = runningFlow.startTime, endTimeInMillis = System.currentTimeMillis())
                        }
                        if (!runningFlow.reported) {
                            TestSuiteStatusView.showFlowCompletion(
                                uploadFlowResult.toViewModel(runningFlow.duration)
                            )
                            runningFlow.reported = true
                        }
                    }
                }
            }

            if (upload.completed) {
                runningFlows.duration = TimeUtils.durationInSeconds(startTimeInMillis = startTime, endTimeInMillis = System.currentTimeMillis())
                return handleSyncUploadCompletion(
                    upload = upload,
                    runningFlows = runningFlows,
                    appId = appId,
                    failOnCancellation = failOnCancellation,
                    reportFormat = reportFormat,
                    reportOutput = reportOutput,
                    testSuiteName = testSuiteName,
                    uploadUrl = uploadUrl
                )
            }

            Thread.sleep(pollingInterval)
        } while (System.currentTimeMillis() - startTime < waitTimeoutMs)

        val displayedMin = TimeUnit.MILLISECONDS.toMinutes(waitTimeoutMs)

        PrintUtils.warn("Waiting for flows to complete has timed out ($displayedMin minutes)")
        PrintUtils.warn("* To extend the timeout, run maestro with this option `maestro cloud --timeout=<timeout in minutes>`")

        PrintUtils.warn("* Follow the results of your upload here:\n$uploadUrl")


        return if (failOnTimeout) {
            PrintUtils.message("Process will exit with code 1 (FAIL)")
            PrintUtils.message("* To change exit code on Timeout, run maestro with this option: `maestro cloud --fail-on-timeout=<true|false>`")

            1
        } else {
            PrintUtils.message("Process will exit with code 0 (SUCCESS)")
            PrintUtils.message("* To change exit code on Timeout, run maestro with this option: `maestro cloud --fail-on-timeout=<true|false>`")

            0
        }
    }

    private fun handleSyncUploadCompletion(
        upload: UploadStatus,
        runningFlows: RunningFlows,
        appId: String,
        failOnCancellation: Boolean,
        reportFormat: ReportFormat,
        reportOutput: File?,
        testSuiteName: String?,
        uploadUrl: String,
    ): Int {
        TestSuiteStatusView.showSuiteResult(
            upload.toViewModel(
                TestSuiteStatusView.TestSuiteViewModel.UploadDetails(
                    uploadId = upload.uploadId,
                    appId = appId,
                    domain = client.domain,
                )
            ),
            uploadUrl
        )

        val isCancelled = upload.status == UploadStatus.Status.CANCELED
        val isFailure = upload.status == UploadStatus.Status.ERROR
        val containsFailure =
            upload.flows.find { it.status == UploadStatus.Status.ERROR } != null // status can be cancelled but also contain flow with failure

        val failed = isFailure || containsFailure || isCancelled && failOnCancellation

        val reportOutputSink = reportFormat.fileExtension
            ?.let { extension ->
                (reportOutput ?: File("report$extension"))
            }

        if (reportOutputSink != null) {
            saveReport(reportFormat, !failed, createSuiteResult(!failed, upload, runningFlows), reportOutputSink, testSuiteName)
        }


        return if (!failed) {
            PrintUtils.message("Process will exit with code 0 (SUCCESS)")
            if (isCancelled) {
                PrintUtils.message("* To change exit code on Cancellation, run maestro with this option: `maestro cloud --fail-on-cancellation=<true|false>`")
            }
            0
        } else {
            PrintUtils.message("Process will exit with code 1 (FAIL)")
            if (isCancelled && !containsFailure) {
                PrintUtils.message("* To change exit code on cancellation, run maestro with this option: `maestro cloud --fail-on-cancellation=<true|false>`")
            }
            1
        }
    }

    private fun saveReport(
        reportFormat: ReportFormat,
        passed: Boolean,
        suiteResult: TestExecutionSummary.SuiteResult,
        reportOutput: File,
        testSuiteName: String?
    ) {
        ReporterFactory.buildReporter(reportFormat, testSuiteName)
            .report(
                TestExecutionSummary(
                    passed = passed,
                    suites = listOf(suiteResult)
                ),
                reportOutput,
            )
    }

    private fun createSuiteResult(passed: Boolean, upload: UploadStatus, runningFlows: RunningFlows): TestExecutionSummary.SuiteResult {
        return TestExecutionSummary.SuiteResult(
            passed = passed,
            flows = upload.flows.map { uploadFlowResult ->
                val failure = uploadFlowResult.errors.firstOrNull()
                TestExecutionSummary.FlowResult(
                    name = uploadFlowResult.name,
                    fileName = null,
                    status = FlowStatus.from(uploadFlowResult.status),
                    failure = if (failure != null) TestExecutionSummary.Failure(failure) else null,
                    duration = runningFlows.flows.find { it.name == uploadFlowResult.name }?.duration
                )
            },
            duration = runningFlows.duration
        )
    }
}
