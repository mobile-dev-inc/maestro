package maestro.cli.command

import maestro.cli.device.Device
import maestro.cli.device.DeviceService
import maestro.cli.device.Platform
import maestro.cli.model.DeviceCreateException
import maestro.cli.report.TestDebugReporter
import maestro.cli.util.AndroidDeviceConfig
import maestro.cli.util.IosDeviceConfig
import maestro.cli.util.PrintUtils
import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "create-device",
    description = [
        "Create an iOS Simulator or Android Emulator"
    ]
)
class CreateDeviceCommand : Callable<Int> {

    @CommandLine.Option(order = 0, names = ["--platform"], description = ["Platforms: android, ios"])
    private lateinit var platform: String

    @CommandLine.Option(
        order = 0,
        names = ["--os-version"],
        description = ["OS version i.e 15, 16 (iOS) / 29, 30, 31, 33 (Android)"]
    )
    private lateinit var osVersion: String


    // show that only one device type is supported at the moment
    // throw device type error if missing
    // Link to documentation when runtime version is not available

    override fun call(): Int {
        TestDebugReporter.install(null)

        when (platform.lowercase()) {
            "ios" -> {
                val version = osVersion.toIntOrNull()
                if (version !in IosDeviceConfig.versions) {
                    PrintUtils.err("Provided iOS version is not supported. Please use one of ${IosDeviceConfig.versions}")
                    return 1
                }

                val runtime = IosDeviceConfig.runtimes[version]
                if (runtime == null) {
                    PrintUtils.err("Provided iOS runtime is not supported $runtime")
                    return 1
                }

                val deviceName = IosDeviceConfig.generateDeviceName(version!!)
                val device = IosDeviceConfig.device

                PrintUtils.message("Attempting to create iOS simulator: $deviceName ...")

                val deviceUUID = try {
                    DeviceService.createIosDevice(deviceName, device, runtime)
                } catch (e: DeviceCreateException) {

                    when (e) {
                        is DeviceCreateException.InvalidRuntimeException -> {
                            PrintUtils.err("Required runtime to create the simulator is not installed: $version")
                            PrintUtils.err("To install additional iOS runtimes checkout this guide:\n* https://developer.apple.com/documentation/xcode/installing-additional-simulator-runtimes")
                        }

                        is DeviceCreateException.InvalidDeviceTypeException -> {
                            PrintUtils.err("Device type $device either not supported or not found.")
                        }

                        is DeviceCreateException.UnableToCreateException -> {
                            PrintUtils.err(e.message)
                        }
                    }

                    return 1
                }


                PrintUtils.message("Created simulator with UUID: $deviceUUID")

                PrintUtils.message("Launching simulator...")
                val launchDevice = Device.AvailableForLaunch(
                    modelId = deviceUUID.toString(),
                    description = deviceName,
                    platform = Platform.IOS
                )
                DeviceService.startDevice(launchDevice)


            }

            "android" -> {
                val version = osVersion.toIntOrNull()
                if (version !in AndroidDeviceConfig.versions) {
                    PrintUtils.err("Provided Android version is not supported. Please use one of ${AndroidDeviceConfig.versions}")
                    return 1
                }

                val systemImage = AndroidDeviceConfig.systemImages[version]
                if (systemImage == null) {
                    PrintUtils.err("Provided system image is not supported. Please use one of ${AndroidDeviceConfig.versions}")
                    return 1
                }


                // dependencies
                if (!DeviceService.isAndroidSystemImageInstalled(systemImage)) {
                    PrintUtils.err("The required system image $systemImage is not installed.")

                    PrintUtils.message("Would you like to install it? y/n")
                    val r = readlnOrNull()?.lowercase()
                    if (r == "y" || r == "yes") {
                        PrintUtils.message("Attempting to install $systemImage via Android SDK Manager...\n")
                        if (!DeviceService.installAndroidSystemImage(systemImage)) {
                            PrintUtils.err("Was unable to install required dependencies.")
                            return 1
                        }
                    } else {
                        PrintUtils.warn(
                            "To install the system image manually, you can run this command:\n${
                                DeviceService.getAndroidSystemImageInstallCommand(
                                    systemImage
                                )
                            }"
                        )
                        return 1
                    }
                }


                // emulator
                try {
                    val name = AndroidDeviceConfig.generateDeviceName(version!!)
                    PrintUtils.message("Attempting to create Android emulator: $name ...")
                    DeviceService.createAndroidDevice(
                        deviceName = name,
                        device = AndroidDeviceConfig.device,
                        systemImage = systemImage,
                        tag = AndroidDeviceConfig.tag,
                        abi = AndroidDeviceConfig.abi
                    )
                    PrintUtils.message("Created Android emulator: $name ($systemImage)")

                    PrintUtils.message("Will launch emulator...")

                    val launchDevice = Device.AvailableForLaunch(
                        modelId = name,
                        description = name,
                        platform = Platform.ANDROID
                    )

                    DeviceService.startDevice(launchDevice)

                    return 0

                } catch (e: DeviceCreateException.UnableToCreateException) {
                    PrintUtils.err("Failed to create emulator ${e.message}")
                }

                return 1
            }

            else -> {
                PrintUtils.err("Invalid platform. Please specify one of: android, ios")
                return 1
            }
        }


        return 0
    }

}
