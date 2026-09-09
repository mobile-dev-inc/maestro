package ios.devicectl

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import device.IOSDevice
import device.IOSScreenRecording
import hierarchy.ViewHierarchy
import maestro.utils.TempFileHandler
import okio.Sink
import okio.buffer
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.slf4j.LoggerFactory
import util.CommandLineUtils
import util.LocalIOSDevice
import xcuitest.api.DeviceInfo
import xcuitest.XCTestDriverClient
import xcuitest.installer.LocalXCTestInstaller
import java.io.InputStream

class DeviceControlIOSDevice(
    override val deviceId: String,
) : IOSDevice {
    private val tempFileHandler = TempFileHandler()
    private val localIOSDevice by lazy { LocalIOSDevice() }
    private var xcTestClient: XCTestDriverClient? = null
    private val goIOS = GoIOSCommands()

    companion object {
        private val logger = LoggerFactory.getLogger(DeviceControlIOSDevice::class.java)
    }

    fun setXCTestClient(client: XCTestDriverClient) {
        this.xcTestClient = client
    }

    private fun getXCTestClient(): XCTestDriverClient {
        return xcTestClient ?: throw IllegalStateException("XCTestClient not initialized")
    }

    override fun open() {
        getXCTestClient().restartXCTestRunner()
    }

    override fun deviceInfo(): DeviceInfo {
        return getXCTestClient().deviceInfo()
    }

    override fun viewHierarchy(excludeKeyboardElements: Boolean): ViewHierarchy {
        return getXCTestClient().viewHierarchy(emptySet(), excludeKeyboardElements)
    }

    override fun tap(x: Int, y: Int) {
        getXCTestClient().tap(x.toFloat(), y.toFloat())
    }

    override fun longPress(x: Int, y: Int, durationMs: Long) {
        getXCTestClient().tap(x.toFloat(), y.toFloat(), durationMs.toDouble() / 1000)
    }

    override fun scroll(xStart: Double, yStart: Double, xEnd: Double, yEnd: Double, duration: Double) {
        getXCTestClient().swipeV2(
            installedApps = emptySet(),
            startX = xStart,
            startY = yStart,
            endX = xEnd,
            endY = yEnd,
            duration = duration
        )
    }

    override fun input(text: String) {
        getXCTestClient().inputText(text, emptySet())
    }

    override fun install(stream: InputStream) {
        // Create temporary container directory using Maestro's tool
        // Create the target file wrapper inside that directory
        val extractDir = tempFileHandler.createTempDirectory()
        val tempAppFile = extractDir.resolve("maestro_upload.ipa")

        try {
            // Create an output stream and copy bytes natively using Kotlin's copyTo extension
            stream.use { input ->
                tempAppFile.outputStream().use { output ->
                    input.copyTo(output) // Pure Kotlin stream copy mechanism
                }
            }

            // Pass the file path directly to go-ios
            goIOS.install(deviceId, tempAppFile.absolutePath)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to stream write application payload onto disk: ${e.message}", e)
        }
    }

    override fun uninstall(id: String) {
        goIOS.uninstall(deviceId, id)
    }

    override fun clearAppState(id: String) {
        logger.info("Clear App State is not implemented")
    }

    override fun clearKeychain(): Result<Unit, Throwable> {
        return runCatching {
            logger.info("Keychain clearing not supported on real devices")
        }
    }

    override fun launch(id: String, launchArguments: Map<String, Any>) {
        goIOS.launch(deviceId, id)
    }

    override fun stop(id: String) {
        getXCTestClient().terminateApp(id)
    }

    override fun isKeyboardVisible(): Boolean {
        return getXCTestClient().keyboardInfo(emptySet()).isKeyboardVisible
    }

    override fun openLink(link: String): Result<Unit, Throwable> {
        return runCatching {
            openLinkWithDiscovery(deviceId, link)
        }
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        val bytes = getXCTestClient().screenshot(compressed)
        out.buffer().use { it.write(bytes) }
    }

    override fun startScreenRecording(out: Sink): IOSScreenRecording {
        error("Screen recording not supported for real devices")
    }

    override fun setLocation(latitude: Double, longitude: Double): Result<Unit, Throwable> {
        return runCatching {
            logger.info("Location simulation not supported on real devices")
        }
    }

    override fun setOrientation(orientation: String) {
        getXCTestClient().setOrientation(orientation)
    }

    override fun isDarkModeEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setAppearance(appearance: String) {
        TODO("Not yet implemented")
    }

    override fun isShutdown(): Boolean {
        return !getXCTestClient().isChannelAlive()
    }

    override fun isScreenStatic(): Boolean {
        return getXCTestClient().isScreenStatic().isScreenStatic
    }

    override fun setPermissions(id: String, permissions: Map<String, String>) {
        getXCTestClient().setPermissions(permissions)
    }

    override fun pressKey(name: String) {
        getXCTestClient().pressKey(name)
    }

    override fun pressButton(name: String) {
        getXCTestClient().pressButton(name)
    }

    override fun eraseText(charactersToErase: Int) {
        getXCTestClient().eraseText(charactersToErase, emptySet())
    }

    override fun addMedia(path: String) {
        logger.info("Adding media to real device not supported")
    }

    override fun close() {
        logger.info("[Start] Uninstall the runner app")
        uninstall(id = LocalXCTestInstaller.UI_TEST_RUNNER_APP_BUNDLE_ID)
        logger.info("[Done] Uninstall the runner app")
        tempFileHandler.close()
    }

    /* This logic is to perform dynamic lookup for deep link handling */
    private fun openLinkWithDiscovery(deviceId: String, link: String) {
        val targetBundleId = determineTargetBundleId(deviceId, link)
        goIOS.launch(deviceId, targetBundleId, link)
    }
    
    private fun determineTargetBundleId(deviceId: String, link: String): String {
        // Open Safari if http(s) link
        if (link.startsWith("http://", ignoreCase = true) || link.startsWith("https://", ignoreCase = true)) {
            return "com.apple.mobilesafari"
        }

        // Extract schema keyword (e.g., extracts "spotify" from "spotify://album/123")
        val schemeKeyword = link.substringBefore("://").lowercase()
        if (schemeKeyword == link.lowercase()) {
            // No schema delimiter found; fallback safely
            return "com.apple.mobilesafari"
        }

        // Lookup for installed apps
        val installedApps = goIOS.listApps(deviceId)

        for (app in installedApps) {
            val lowercaseApp = app.lowercase()
            if (lowercaseApp.contains(schemeKeyword)) {
                val discoveredId = extractBundleId(app)
                if (discoveredId != null) {
                    return discoveredId
                }
            }
        }

        // Fallback if didn't find app fitting deeplink
        return "com.apple.mobilesafari"
    }

    private fun extractBundleId(line: String): String? {
        val startIndex = line.lastIndexOf("(")
        val endIndex = line.lastIndexOf(")")
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return line.substring(startIndex + 1, endIndex).trim()
        }
        
        // Alternative fallback: if go-ios prints raw bundle IDs separated by space/tabs
        val tokens = line.split(Regex("\\s+"))
        return tokens.lastOrNull { it.contains(".") }
    }
}

class GoIOSCommands(
    val path: String = System.getenv("GO_IOS") ?: "go-ios"
) {
    fun launch(deviceId: String, bundleId: String, parameters: String = "") {
        var args = listOf(
            path,
            "--udid",
            deviceId,
            "launch",
            bundleId
        )

        if (parameters != "") {
            args += listOf("--arg=$parameters")
        }

        CommandLineUtils.runCommand(args)
    }

    fun install(deviceId: String, appPath: String) {
        val args = listOf(
            path,
            "install",
            "--udid",
            deviceId,
            "--path",
            appPath
        )

        CommandLineUtils.runCommand(args)
    }

    fun uninstall(deviceId: String, bundleId: String) {
        val args = listOf(
            path,
            "uninstall",
            "--udid",
            deviceId,
            bundleId
        )

        CommandLineUtils.runCommand(args)
    }

    fun listApps(deviceId: String): List<String> {
        val args = listOf(
            path, 
            "apps", 
            "--udid",
            deviceId
        )

        // BOTH blocks now strictly evaluate to a String
        val rawOutput: String = try {
            CommandLineUtils.runCommand(args)
                .inputStream     // 1. Get the process byte pipe
                .reader()        // 2. Wrap it in a stream character reader
                .readText()      // 3. Extract all data into a standard String
        } catch (e: Exception) {
            System.err.println("[Warning] Error looking up for installed apps on $deviceId: ${e.message}")
            System.err.flush()
            "" // 4. Matches the String type above perfectly
        }

        if (rawOutput.isBlank()) return emptyList()

        // Process strings directly
        return rawOutput
            .split("\n")
    }

}
