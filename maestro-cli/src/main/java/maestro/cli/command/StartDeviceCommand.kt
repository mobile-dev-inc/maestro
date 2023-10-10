package maestro.cli.command

import maestro.cli.CliError
import maestro.cli.device.DeviceCreateUtil
import maestro.cli.device.DeviceService
import maestro.cli.device.Platform
import maestro.cli.report.TestDebugReporter
import maestro.cli.util.DeviceConfigAndroid
import maestro.cli.util.DeviceConfigIos
import maestro.cli.util.EnvUtils
import maestro.cli.util.PrintUtils
import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "start-device",
    description = [
        "Starts or creates an iOS Simulator or Android Emulator similar to the ones on Maestro Cloud",
        "Supported device types: iPhone11 (iOS), Pixel 6 (Android)",
    ]
)
class StartDeviceCommand : Callable<Int> {

    @CommandLine.Option(
        order = 0,
        names = ["--platform"],
        required = true,
        description = ["Platforms: android, ios"],
    )
    private lateinit var platform: String

    @CommandLine.Option(
        order = 1,
        names = ["--os-version"],
        description = ["OS version to use:", "iOS: 15, 16", "Android: 28, 29, 30, 31, 33"],
    )
    private lateinit var osVersion: String

    @CommandLine.Option(
        order = 2,
        names = ["--device-locale"],
        description = ["a combination of lowercase ISO-639-1 code and uppercase ISO-3166-1 code i.e. \"de_DE\" for Germany"],
    )
    private var deviceLocale: String? = null

    @CommandLine.Option(
        order = 4,
        names = ["--force-create"],
        description = ["Will override existing device if it already exists"],
    )
    private var forceCreate: Boolean = false

    override fun call(): Int {
        TestDebugReporter.install(null)

        if (EnvUtils.isWSL()) {
            throw CliError("This command is not supported in Windows WSL. You can launch your emulator manually.")
        }

        val p = Platform.fromString(platform)
            ?: throw CliError("Unsupported platform $platform. Please specify one of: android, ios")

        // default OS version
        if (!::osVersion.isInitialized) {
            osVersion = when (p) {
                Platform.IOS -> DeviceConfigIos.defaultVersion.toString()
                Platform.ANDROID -> DeviceConfigAndroid.defaultVersion.toString()
                else -> ""
            }
        }
        val o = osVersion.toIntOrNull()

        val (deviceLanguage, deviceCountry) = validateLocaleParams()

        DeviceCreateUtil.getOrCreateDevice(p, o, deviceLanguage, deviceCountry, forceCreate).let {
            PrintUtils.message(if (p == Platform.IOS) "Launching simulator..." else "Launching emulator...")
            DeviceService.startDevice(it)
        }

        return 0
    }

    private fun validateLocaleParams(): Pair<String, String> {
        deviceLocale?.let {
            val parts = it.split("_")

            if (parts.size == 2) {
                val language = parts[0]
                val country = parts[1]
                return Pair(language, country)
            } else {
                throw CliError("Wrong device locale format was provided $it. A combination of lowercase ISO-639-1 code and uppercase ISO-3166-1 code should be used, i.e. \"de_DE\" for Germany")
            }
        }

        // use en_US locale as a default
        return Pair("en", "US")
    }
}
