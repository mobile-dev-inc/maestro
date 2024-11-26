package maestro.ios

import com.google.common.truth.Truth.assertThat
import java.io.File
import xcuitest.XCTestClient
import xcuitest.installer.XCTestInstaller

class MockXCTestInstaller(
    private val simulator: Simulator,
) : XCTestInstaller {

    private var attempts = 0

    override fun install(): File {
        attempts++
        for (i in 0..simulator.installationRetryCount) {
            assertThat(simulator.runningApps()).doesNotContain("dev.mobile.maestro-driver-iosUITests.xctrunner")
        }
        if (simulator.shouldInstall) {
            simulator.installXCTestDriver()
        }
        return File("xctestrun")
    }

    override fun uninstall() {
        simulator.uninstallXCTestDriver()
        return
    }

    fun assertInstallationRetries(expectedRetries: Int) {
        assertThat(attempts).isEqualTo(expectedRetries)
    }

    data class Simulator(
        val installationRetryCount: Int = 0,
        val shouldInstall: Boolean = true
    ) {

        private val runningApps = mutableListOf<String>()

        fun runningApps() = runningApps

        fun uninstallXCTestDriver() = runningApps.clear()

        fun installXCTestDriver() = runningApps.add("dev.mobile.maestro-driver-iosUITests.xctrunner")
    }
}
