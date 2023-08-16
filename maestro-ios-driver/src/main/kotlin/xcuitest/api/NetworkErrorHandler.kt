package xcuitest.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import util.PrintUtils
import xcuitest.XCTestClient
import xcuitest.api.NetworkException.Companion.toUserNetworkException
import xcuitest.installer.XCTestInstaller

class NetworkErrorHandler(private val xcTestInstaller: XCTestInstaller) {


    private lateinit var client: XCTestClient
    private var retry = 0

    companion object {
        const val RETRY_RESPONSE_CODE = 503
        private const val NO_RETRY_RESPONSE_CODE = 502
        private val mapper = jacksonObjectMapper()

        fun NetworkException.getRetrialResponse(request: Request): Response {
            val userNetworkModel = toUserNetworkException()
            val json = mapper.writeValueAsString(userNetworkModel)
            val responseBody = json.toResponseBody("application/json; charset=utf-8".toMediaType())
            return if (shouldRetryDriverInstallation()) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .message(userNetworkModel.userFriendlyMessage)
                    .body(responseBody)
                    .code(RETRY_RESPONSE_CODE)
                    .build()
            } else {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .message(userNetworkModel.userFriendlyMessage)
                    .body(responseBody)
                    .code(NO_RETRY_RESPONSE_CODE)
                    .build()
            }
        }

        private fun NetworkException.shouldRetryDriverInstallation(): Boolean {
            return when (this) {
                is NetworkException.ConnectionException,
                is NetworkException.TimeoutException,
                is NetworkException.UnknownNetworkException -> true
                is NetworkException.UnknownHostException -> false
            }
        }
    }

    fun retryConnection(call: Call, response: Response): Response {
        return if (retry < 3) {
            xcTestInstaller.start()?.let {
                client = it
            }
            response.close()
            retry++
            PrintUtils.log("ℹ️ Retrying connection to the XCUITest server for ${retry}...")
            call.clone().execute()
        } else {
            PrintUtils.log("⚠️ Error: ${response.message}")
            retry = 0
            response
        }
    }

    fun resetRetryCount() {
        retry = 0
    }

    fun retryWithoutConnection() {

    }

    fun reinitializedXCTestClient(): XCTestClient? {
        if (!::client.isInitialized) return null

        return client
    }
}