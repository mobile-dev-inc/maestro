package ios

import com.github.michaelbull.result.Result
import hierarchy.ElementNode
import ios.device.DeviceInfo
import ios.simctl.SimctlIOSDevice
import ios.xctest.XCTestIOSDevice
import okio.Sink
import java.io.File
import java.io.InputStream

class LocalIOSDevice(
    override val deviceId: String?,
    private val xcTestDevice: XCTestIOSDevice,
    private val simctlIOSDevice: SimctlIOSDevice,
) : IOSDevice {

    override fun open() {
        xcTestDevice.open()
    }

    override fun deviceInfo(): Result<DeviceInfo, Throwable> {
        return xcTestDevice.deviceInfo()
    }

    override fun contentDescriptor(): Result<ElementNode, Throwable> {
        return xcTestDevice.contentDescriptor()
    }

    override fun tap(x: Int, y: Int): Result<Unit, Throwable> {
        return xcTestDevice.tap(x, y)
    }

    override fun longPress(x: Int, y: Int, durationMs: Long): Result<Unit, Throwable> {
        return xcTestDevice.longPress(x, y, durationMs)
    }

    override fun pressKey(name: String) {
        xcTestDevice.pressKey(name)
    }

    override fun pressButton(name: String) {
        xcTestDevice.pressButton(name)
    }

    override fun scroll(
        xStart: Double,
        yStart: Double,
        xEnd: Double,
        yEnd: Double,
        duration: Double
    ): Result<Unit, Throwable> {
        return xcTestDevice.scroll(xStart, yStart, xEnd, yEnd, duration)
    }

    override fun input(text: String): Result<Unit, Throwable> {
        return xcTestDevice.input(text)
    }

    override fun install(stream: InputStream): Result<Unit, Throwable> {
        return simctlIOSDevice.install(stream)
    }

    override fun uninstall(id: String): Result<Unit, Throwable> {
        return simctlIOSDevice.uninstall(id)
    }

    override fun pullAppState(id: String, file: File): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    override fun pushAppState(id: String, file: File): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    override fun clearAppState(id: String): Result<Unit, Throwable> {
        return simctlIOSDevice.clearAppState(id)
    }

    override fun clearKeychain(): Result<Unit, Throwable> {
        return simctlIOSDevice.clearKeychain()
    }

    override fun launch(id: String): Result<Unit, Throwable> {
        return simctlIOSDevice.launch(id)
    }

    override fun stop(id: String): Result<Unit, Throwable> {
        return simctlIOSDevice.stop(id)
    }

    override fun openLink(link: String): Result<Unit, Throwable> {
        return simctlIOSDevice.openLink(link)
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean): Result<Unit, Throwable> {
        return xcTestDevice.takeScreenshot(out, compressed)
    }

    override fun startScreenRecording(out: Sink): Result<IOSScreenRecording, Throwable> {
        return simctlIOSDevice.startScreenRecording(out)
    }

    override fun setLocation(latitude: Double, longitude: Double): Result<Unit, Throwable> {
        return simctlIOSDevice.setLocation(latitude, longitude)
    }

    override fun isShutdown(): Boolean {
        return xcTestDevice.isShutdown()
    }

    override fun close() {
        xcTestDevice.close()
        simctlIOSDevice.close()
    }

    override fun isScreenStatic(): Result<Boolean, Throwable> {
        return xcTestDevice.isScreenStatic()
    }

    override fun setPermissions(id: String, permissions: Map<String, String>) {
        simctlIOSDevice.setPermissions(id, permissions)
    }

    override fun eraseText(charactersToErase: Int) {
        xcTestDevice.eraseText(charactersToErase)
    }
}
