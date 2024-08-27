package maestro.cli.command

import maestro.cli.App
import maestro.cli.CliError
import maestro.cli.DisableAnsiMixin
import maestro.cli.ShowHelpMixin
import maestro.cli.device.Platform
import maestro.cli.session.MaestroSessionManager
import maestro.cli.util.DeviceConfigIos.device
import picocli.CommandLine
import picocli.CommandLine.Command

@Command(
    name = "start-session",
    description = [
        "Setup Maestro session once, so you can run 'maestro test' faster since the setup is already done.",
    ],
    hidden = true
)
class StartSessionCommand : Runnable {
    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.Mixin
    var showHelpMixin: ShowHelpMixin? = null

    @CommandLine.ParentCommand
    private val parent: App? = null

    override fun run() {
        MaestroSessionManager.newSession(
            host = parent?.host,
            port = parent?.port,
            driverHostPort = parent?.port,
            deviceId = parent?.deviceId
        ) { session ->
            /**
             * As of now on iOS, the session gets started but this has no impact on subsequent tests
             * running maestro test after the session would still install the XCTest runner
             */
            if (session.device?.platform != Platform.ANDROID) {
                throw CliError ("This command is only supported for Android devices.")
            }

            println("Maestro session started. Keep this running in the background.")
            println("Run your tests with `maestro test <test.yaml>` in another terminal.")

            while (!Thread.interrupted()) {
                Thread.sleep(100)
            }
        }
    }
}
