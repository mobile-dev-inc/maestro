package xcuitest.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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
        private const val MAX_RETRY = 3
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
            resetRetryCount()
            response
        }
    }

    fun retryConnection(chain: Interceptor.Chain, networkException: NetworkException): Response {
        return if (networkException.shouldRetryDriverInstallation() && retry < MAX_RETRY) {
            xcTestInstaller.start()?.let {
                client = it
            }
            retry++
            PrintUtils.log("ℹ️ Retrying connection to the XCUITest server for ${retry}...")
            chain.call().clone().execute()
        } else {
            val userNetworkException = networkException.toUserNetworkException()
            val json = mapper.writeValueAsString(userNetworkException)
            val responseBody = json.toResponseBody("application/json; charset=utf-8".toMediaType())
            PrintUtils.log("⚠️ Error: ${userNetworkException.userFriendlyMessage}")
            resetRetryCount()
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .message(userNetworkException.userFriendlyMessage)
                .body(responseBody)
                .code(NO_RETRY_RESPONSE_CODE)
                .build()
        }
    }

    fun resetRetryCount() {
        retry = 0
    }

    fun reinitializedXCTestClient(): XCTestClient? {
        if (!::client.isInitialized) return null

        return client
    }
}