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

import com.squareup.okhttp.OkHttpClient
import maestro.cli.api.ApiClient
import maestro.cli.util.WorkspaceUtils
import maestro.utils.FileUtils
import maestro.utils.TemporaryDirectory
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "upload",
)
class UploadCommand : Callable<Int> {

    @CommandLine.Parameters
    private lateinit var uploadName: String

    @CommandLine.Parameters
    private lateinit var appFile: File

    @CommandLine.Parameters
    private lateinit var flowFile: File

    @Option(names = ["-k", "--apiKey"], required = true)
    private lateinit var apiKey: String

    @Option(names = ["--apiUrl"])
    private var apiUrl: String = "https://api.mobile.dev"

    @Option(names = ["--repoOwner"])
    private var repoOwner: String? = null

    @Option(names = ["--repoName"])
    private var repoName: String? = null

    @Option(names = ["--branch"])
    private var branch: String? = null

    @Option(names = ["--pullRequestId"])
    private var pullRequestId: String? = null

    @Option(names = ["-e", "--env"])
    private var env: Map<String, String> = emptyMap()

    override fun call(): Int {
        val client = ApiClient(apiUrl, apiKey)
        TemporaryDirectory.use { tmpDir ->
            val workspaceZip = tmpDir.resolve("workspace.zip")
            WorkspaceUtils.createWorkspaceZip(flowFile.toPath(), workspaceZip)
            client.upload(appFile.toPath(), workspaceZip, uploadName, repoOwner, repoName, branch, pullRequestId)
        }
        return 0
    }
}
