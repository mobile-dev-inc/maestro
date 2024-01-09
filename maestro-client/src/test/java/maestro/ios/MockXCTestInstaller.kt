package maestro.ios

import com.google.common.truth.Truth.assertThat
import xcuitest.XCTestClient
import xcuitest.installer.Intent
import xcuitest.installer.SourceIntent
import xcuitest.installer.XCTestInstaller

class MockXCTestInstaller(
    private val simulator: Simulator,
) : XCTestInstaller {

    private var attempts = 0

    override fun start(sourceIntent: SourceIntent): XCTestClient? {
        attempts++
        for (i in 0..simulator.installationRetryCount) {
            assertThat(simulator.runningApps()).doesNotContain("dev.mobile.maestro-driver-iosUITests.xctrunner")
        }
        return if (simulator.shouldInstall) {
            simulator.installXCTestDriver()
            XCTestClient("localhost", 22807)
        } else {
            null
        }
    }

    override fun uninstall(sourceIntent: SourceIntent) {
        simulator.uninstallXCTestDriver()
    }

    override fun isChannelAlive(): Boolean {
        return simulator.isXCTestRunnerAlive()
    }

    override fun close(sourceIntent: SourceIntent) {
        simulator.uninstallXCTestDriver()
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

        fun isXCTestRunnerAlive() = runningApps.contains("dev.mobile.maestro-driver-iosUITests.xctrunner")

        fun uninstallXCTestDriver() = runningApps.clear()

        fun installXCTestDriver() = runningApps.add("dev.mobile.maestro-driver-iosUITests.xctrunner")
    }
}