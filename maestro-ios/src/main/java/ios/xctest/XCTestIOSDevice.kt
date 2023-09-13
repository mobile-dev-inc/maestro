package ios.xctest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import hierarchy.ViewHierarchy
import ios.IOSDevice
import ios.IOSScreenRecording
import xcuitest.api.DeviceInfo
import logger.Logger
import maestro.utils.DepthTracker
import maestro.utils.network.UnknownFailure
import okio.Sink
import okio.buffer
import xcuitest.XCTestDriverClient
import xcuitest.api.IsScreenStaticResponse
import java.io.InputStream
import java.util.UUID

class XCTestIOSDevice(
    override val deviceId: String?,
    private val client: XCTestDriverClient,
    private val logger: Logger,
    private val getInstalledApps: () -> Set<String>,
) : IOSDevice {

    override fun open() {
        client.restartXCTestRunnerService()
    }

    override fun deviceInfo(): Result<DeviceInfo, Throwable> {
        return runCatching {
            val deviceInfo = client.deviceInfo()
            logger.info("Device info $deviceInfo")
            deviceInfo
        }
    }

    override fun viewHierarchy(): Result<ViewHierarchy, Throwable> {
        val installedApps = getInstalledApps()
        val result = runCatching {
            val viewHierarchy = client.viewHierarchy(installedApps)
            DepthTracker.trackDepth(viewHierarchy.depth)
            logger.info("Depth received: ${viewHierarchy.depth}")
            viewHierarchy
        }
        return result
    }

    override fun tap(x: Int, y: Int): Result<Unit, Throwable> {
        return runCatching {
            client.tap(
                x = x.toFloat(),
                y = y.toFloat(),
            )
        }
    }

    override fun longPress(x: Int, y: Int, durationMs: Long): Result<Unit, Throwable> {
        return runCatching {
            client.tap(
                x = x.toFloat(),
                y = y.toFloat(),
                duration = durationMs.toDouble() / 1000
            )
        }
    }

    override fun pressKey(name: String) {
        client.pressKey(name)
    }

    override fun pressButton(name: String) {
        client.pressButton(name)
    }

    override fun addMedia(path: String) {
        error("Not supported")
    }

    override fun deleteMedia() {
        error("Not supported")
    }

    override fun scroll(
        xStart: Double,
        yStart: Double,
        xEnd: Double,
        yEnd: Double,
        duration: Double,
    ): Result<Unit, Throwable> {
        return runCatching {
            client.swipe(
                appId = activeAppId(),
                startX = xStart,
                startY = yStart,
                endX = xEnd,
                endY = yEnd,
                duration = duration
            )
        }
    }

    fun scrollV2(
        xStart: Double,
        yStart: Double,
        xEnd: Double,
        yEnd: Double,
        duration: Double,
    ): Result<Unit, Throwable> {
        return runCatching {
            client.swipeV2(
                installedApps = getInstalledApps(),
                startX = xStart,
                startY = yStart,
                endX = xEnd,
                endY = yEnd,
                duration = duration,
            )
        }
    }

    override fun input(text: String): Result<Unit, Throwable> {
        return runCatching {
            val appIds = getInstalledApps()
            client.inputText(
                text = text,
                appIds = appIds,
            )
        }
    }

    override fun install(stream: InputStream): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun uninstall(id: String): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun clearAppState(id: String): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun clearKeychain(): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun launch(
        id: String,
        launchArguments: Map<String, Any>,
        maestroSessionId: UUID?,
    ): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun stop(id: String): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun openLink(link: String): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean): Result<Unit, Throwable> {
        return runCatching {
            client.screenshot(compressed).use { response ->
                response.body.use { body ->
                    body ?: throw UnknownFailure("Error - body for snapshot request not available")

                    if (response.isSuccessful) {
                        out.buffer().use {
                            it.write(body.bytes())
                        }
                    } else {
                        val errorResponse = String(body.bytes()).trim()
                        throw UnknownFailure(errorResponse)
                    }
                }
            }
        }
    }

    override fun startScreenRecording(out: Sink): Result<IOSScreenRecording, Throwable> {
        error("Not supported")
    }

    override fun setLocation(latitude: Double, longitude: Double): Result<Unit, Throwable> {
        error("Not supported")
    }

    override fun isShutdown(): Boolean {
        return !client.isChannelAlive()
    }

    override fun close() {
        client.close()
    }

    override fun isScreenStatic(): Result<Boolean, Throwable> {
        return runCatching {
            client.isScreenStatic().use { response ->
                response.body.use { body ->
                    body ?: throw UnknownFailure("Error - body for isScreenStatic request not available")

                    if (response.isSuccessful) {
                        val responseBody: IsScreenStaticResponse = mapper.readValue(
                            String(body.bytes()),
                            IsScreenStaticResponse::class.java
                        )
                        val isScreenStatic = responseBody.isScreenStatic
                        logger.info("Screen diff request finished with isScreenStatic = $isScreenStatic")
                        isScreenStatic
                    } else {
                        val errorResponse = String(body.bytes()).trim()
                        logger.info("Screen diff request failed with error = $errorResponse")
                        throw UnknownFailure(errorResponse)
                    }
                }
            }
        }
    }

    override fun setPermissions(id: String, permissions: Map<String, String>): Result<Unit, Throwable> {
        return runCatching {
            val mutable = permissions.toMutableMap()
            if (mutable.containsKey("all")) {
                val value = mutable.remove("all")
                allPermissions.forEach {
                    when (value) {
                        "allow" -> mutable.putIfAbsent(it, "allow")
                        "deny" -> mutable.putIfAbsent(it, "deny")
                        "unset" -> mutable.putIfAbsent(it, "unset")
                        else -> throw IllegalArgumentException("Permission 'all' can be set to 'allow', 'deny' or 'unset', not '$value'")
                    }
                }
            }

            client.setPermissions(mutable)
        }
    }

    override fun eraseText(charactersToErase: Int) {
        val appIds = getInstalledApps()
        client.eraseText(charactersToErase, appIds)
    }

    private fun activeAppId(): String {
        val appIds = getInstalledApps()
        logger.info("installed apps: $appIds")

        return client.runningAppId(appIds).runningAppBundleId
    }

    companion object {

        private val mapper by lazy { jacksonObjectMapper() }

        private val allPermissions = listOf(
            "notifications"
        )
    }

}
