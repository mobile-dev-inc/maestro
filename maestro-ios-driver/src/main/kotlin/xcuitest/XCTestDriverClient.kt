package xcuitest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hierarchy.ViewHierarchy
import maestro.utils.network.XCUITestServerError
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import xcuitest.api.*
import xcuitest.api.NetworkErrorHandler.Companion.RETRY_RESPONSE_CODE
import xcuitest.installer.XCTestInstaller
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import xcuitest.process.XCTestStarter

const val UI_TEST_RUNNER_APP_BUNDLE_ID = "dev.mobile.maestro-driver-iosUITests.xctrunner"

class XCTestDriverClient(
    private val installer: XCTestInstaller,
    private val starter: XCTestStarter,
    private val httpInterceptor: HttpLoggingInterceptor? = null
) {
    private val logger = LoggerFactory.getLogger(XCTestDriverClient::class.java)

    private lateinit var client: XCTestClient

    constructor(installer: XCTestInstaller, starter: XCTestStarter, client: XCTestClient): this(installer, starter) {
        this.client = client
    }

    private var isShuttingDown = false

    private val networkErrorHandler by lazy { NetworkErrorHandler(installer, starter) }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            isShuttingDown = true
        })
        httpInterceptor?.level = HttpLoggingInterceptor.Level.BODY
    }

    fun restartXCTestRunner() {
        logger.trace("Restarting XCTest Runner (uninstalling, installing and starting)")
        installer.uninstall()

        logger.trace("XCTest Runner uninstalled, will install")
        val xcTestFile = installer.install()
        client = starter.start(xcTestFile) ?: throw XCTestDriverUnreachable("Failed to start XCTest Driver")
        // TODO: Ensure that it has started
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(40, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .apply {
            httpInterceptor?.let {
                this.addInterceptor(it)
            }
        }
        .addRetryInterceptor()
        .addRetryAndShutdownInterceptor()
        .build()

    class XCTestDriverUnreachable(message: String) : IOException(message)

    private val mapper = jacksonObjectMapper()

    fun viewHierarchy(installedApps: Set<String>, excludeKeyboardElements: Boolean): ViewHierarchy {
        val responseString = executeJsonRequest(
            "viewHierarchy",
            ViewHierarchyRequest(installedApps, excludeKeyboardElements)
        )
        return mapper.readValue(responseString, ViewHierarchy::class.java)
    }

    fun screenshot(compressed: Boolean): ByteArray {
        val url = client.xctestAPIBuilder("screenshot")
            .addQueryParameter("compressed", compressed.toString())
            .build()

        return executeJsonRequest(url)
    }

    fun keyboardInfo(installedApps: Set<String>): KeyboardInfoResponse {
        val response = executeJsonRequest(
            "keyboard",
            KeyboardInfoRequest(installedApps)
        )
        return mapper.readValue(response, KeyboardInfoResponse::class.java)
    }

    fun isScreenStatic(): IsScreenStaticResponse {
        val responseString = executeJsonRequest("isScreenStatic")
        return mapper.readValue(responseString, IsScreenStaticResponse::class.java)
    }

    fun runningAppId(appIds: Set<String>): GetRunningAppIdResponse {
        val response = executeJsonRequest(
            "runningApp",
            GetRunningAppRequest(appIds)
        )
        return mapper.readValue(response, GetRunningAppIdResponse::class.java)
    }

    fun swipe(
        appId: String,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        duration: Double,
    ) {
        executeJsonRequest("swipe",
            SwipeRequest(
                appId = appId,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                duration = duration
            )
        )
    }

    fun swipeV2(
        installedApps: Set<String>,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        duration: Double,
    ) {
        executeJsonRequest("swipeV2",
            SwipeRequest(
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                duration = duration,
                appIds = installedApps
            )
        )
    }

    fun inputText(
        text: String,
        appIds: Set<String>,
    ) {
        executeJsonRequest("inputText", InputTextRequest(text, appIds))
    }

    fun tap(
        x: Float,
        y: Float,
        duration: Double? = null,
    ) {
        executeJsonRequest("touch", TouchRequest(
            x = x,
            y = y,
            duration = duration
        ))
    }

    fun pressKey(name: String) {
        executeJsonRequest("pressKey", PressKeyRequest(name))
    }

    fun pressButton(name: String) {
        executeJsonRequest("pressButton", PressButtonRequest(name))
    }

    fun eraseText(charactersToErase: Int, appIds: Set<String>) {
        executeJsonRequest("eraseText", EraseTextRequest(charactersToErase, appIds))
    }

    fun deviceInfo(httpUrl: HttpUrl = client.xctestAPIBuilder("deviceInfo").build()): DeviceInfo {
        val response = executeJsonRequest(httpUrl, Unit)
        return mapper.readValue(response, DeviceInfo::class.java)
    }

    fun isChannelAlive(): Boolean {
        return starter.isRunning()
    }

    fun close() {
        installer.uninstall()
    }

    fun setPermissions(permissions: Map<String, String>) {
        executeJsonRequest("setPermissions", SetPermissionsRequest(permissions))
    }

    private fun executeJsonRequest(httpUrl: HttpUrl, body: Any): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val bodyData = mapper.writeValueAsString(body).toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(httpUrl)
            .post(bodyData)

        return okHttpClient
            .newCall(requestBuilder.build())
            .execute().use { processResponse(it, httpUrl.toString()) }
    }

    private fun executeJsonRequest(httpUrl: HttpUrl): ByteArray {
        val request = Request.Builder()
            .get()
            .url(httpUrl)
            .build()

        return okHttpClient
            .newCall(request)
            .execute().use {
                val bytes = it.body?.bytes() ?: ByteArray(0)
                if (!it.isSuccessful) {
                    //handle exception
                    val responseBodyAsString = String(bytes)
                    handleExceptions(it.code, request.url.pathSegments.first(), responseBodyAsString)
                }
                bytes
            }
    }

    private fun executeJsonRequest(pathSegment: String, body: Any): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val bodyData = mapper.writeValueAsString(body).toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(client.xctestAPIBuilder(pathSegment).build())
            .post(bodyData)

        return okHttpClient
            .newCall(requestBuilder.build())
            .execute().use { processResponse(it, pathSegment) }
    }

    private fun executeJsonRequest(pathSegment: String): String {
        val requestBuilder = Request.Builder()
            .url(client.xctestAPIBuilder(pathSegment).build())
            .get()

        return okHttpClient
            .newCall(requestBuilder.build())
            .execute().use { processResponse(it, pathSegment) }
    }

    private fun processResponse(response: Response, url: String): String {
        val responseBodyAsString = response.body?.bytes()?.let { bytes -> String(bytes) } ?: ""

        return if (!response.isSuccessful) {
            val code = response.code
            handleExceptions(code, url, responseBodyAsString)
        } else {
            responseBodyAsString
        }
    }

    private fun handleExceptions(
        code: Int,
        pathString: String,
        responseBodyAsString: String,
    ): String {
        val error = mapper.readValue(responseBodyAsString, Error::class.java)
        when {
            code in 400..499 -> {
                logger.error("Request for $pathString failed with bad request ${code}, body: $responseBodyAsString")
                throw XCUITestServerError.BadRequest(
                    "Request for $pathString failed with bad request ${code}, body: $responseBodyAsString",
                    responseBodyAsString
                )
            }
            code == NetworkErrorHandler.NO_RETRY_RESPONSE_CODE -> {
                logger.error("Request for $pathString failed, because of XCUITest server got crashed/exit, body: $responseBodyAsString")
                throw XCUITestServerError.NetworkError(
                    "Request for $pathString failed, because of XCUITest server got crashed/exit, body: $responseBodyAsString"
                )
            }
            error.errorMessage.contains("Lost connection to the application.*".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Application [a-zA-Z0-9.]+ is not running".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Error getting main window kAXErrorCannotComplete") -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Error getting main window.*".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            else -> {
                logger.error("Request for $pathString failed, because of unknown reason, body: $responseBodyAsString")
                throw XCUITestServerError.UnknownFailure(
                    "Request for $pathString failed, code: ${code}, body: $responseBodyAsString"
                )
            }
        }
    }

    private fun OkHttpClient.Builder.addRetryInterceptor() = addInterceptor(Interceptor { chain ->
        val response = try {
             chain.proceed(chain.request())
        } catch (ioException: IOException) {
            val networkException = mapNetworkException(ioException)
            return@Interceptor networkErrorHandler.retryConnection(chain, networkException) {
                client = it
            }
        }

        return@Interceptor when (response.code) {
            RETRY_RESPONSE_CODE -> {
                networkErrorHandler.retryConnection(chain.call(), response) {
                    logger.info("Reinitialized the xctest client after reestablishing connection")
                    client = it
                }
            }
            else -> {
                networkErrorHandler.resetRetryCount()
                response
            }
        }
    })

    private fun OkHttpClient.Builder.addRetryAndShutdownInterceptor() = addNetworkInterceptor(Interceptor {
        val request = it.request()
        try {
            it.proceed(request)
        } catch (ioException: IOException) {
            // Fake an Ok response when shutting down and receiving an error
            // to prevent a stack trace in the cli when running maestro studio.

            if (isShuttingDown) {
                val message = "Shutting down xctest server"
                val responseBody = """
                    { "message" : "$message" }
                """.trimIndent().toResponseBody("application/json; charset=utf-8".toMediaType())

                Response.Builder()
                    .request(it.request())
                    .protocol(Protocol.HTTP_1_1)
                    .message(message)
                    .body(responseBody)
                    .code(200)
                    .build()
            } else {
                val networkException = mapNetworkException(ioException)
                return@Interceptor networkErrorHandler.getRetrialResponse(networkException, request)
            }
        }
    })

    private fun mapNetworkException(e: IOException): NetworkException {
        return when (e) {
            is SocketTimeoutException -> NetworkException.TimeoutException("Socket timeout")
            is ConnectException -> NetworkException.ConnectionException("Connection error")
            is UnknownHostException -> NetworkException.UnknownHostException("Unknown host")
            else -> {
                logger.info("Exception $e is not mapped io exception")
                NetworkException.UnknownNetworkException(e.message ?: e.stackTraceToString())
            }
        }
    }
}
