package maestro.cli.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.MultipartBuilder
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import maestro.cli.CliError
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class ApiClient(
    private val baseUrl: String,
    private val apiKey: String,
) {

    private val client = OkHttpClient()

    fun upload(
        appFile: Path,
        workspaceZip: Path,
        uploadName: String,
        repoOwner: String?,
        repoName: String?,
        branch: String?,
        pullRequestId: String?,
    ) {
        if (!appFile.exists()) throw CliError(appFile.absolutePathString())
        if (!workspaceZip.exists()) throw CliError(workspaceZip.absolutePathString())

        val requestPart = mutableMapOf<String, String>()
        requestPart["benchmarkName"] = uploadName
        repoOwner?.let { requestPart["repoOwner"] = it }
        repoName?.let { requestPart["repoName"] = it }
        branch?.let { requestPart["branch"] = it }
        pullRequestId?.let { requestPart["pullRequestId"] = it }

        val body = MultipartBuilder()
            .type(MultipartBuilder.FORM)
            .addFormDataPart("app_binary", "app.zip", RequestBody.create(MediaType.parse("application/zip"), appFile.toFile()))
            .addFormDataPart("workspace", "workspace.zip", RequestBody.create(MediaType.parse("application/zip"), workspaceZip.toFile()))
            .addFormDataPart("request", JSON.writeValueAsString(requestPart))
            .build()

        val request = Request.Builder()
            .header("Authorization", "Bearer $apiKey")
            .url("$baseUrl/v2/upload")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw CliError("Upload request failed (${response.code()}): ${response.body().string()}")
        }
    }

    companion object {

        private val JSON = ObjectMapper()
    }
}