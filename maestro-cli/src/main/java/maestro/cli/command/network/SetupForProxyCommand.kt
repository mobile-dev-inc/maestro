package maestro.cli.command.network

import maestro.cli.device.Device
import maestro.cli.device.PickDeviceInteractor
import maestro.cli.device.Platform
import maestro.cli.device.ios.Simctl
import maestro.networkproxy.NetworkProxy
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(
    name = "setup",
)
class SetupForProxyCommand : Callable<Int> {

    @CommandLine.ParentCommand
    lateinit var parent: NetworkCommand

    override fun call(): Int {
        if (parent.app.host != null) {
            error("Automatic proxy setup of remote devices is not supported. Please configure the device manually.")
        }

        val device = PickDeviceInteractor.pickDevice(parent.app.deviceId)

        when (device.platform) {
            Platform.IOS -> setupIOS(device)
            Platform.ANDROID -> TODO()
        }

        return 0
    }

    private fun setupIOS(device: Device.Connected) {
        val certFile = NetworkProxy.unpackPemCertificates()

        Simctl.addTrustedCertificate(
            device.instanceId,
            certFile,
        )
    }

}
