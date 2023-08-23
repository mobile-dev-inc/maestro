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
        order = 3,
        names = ["--force-create"],
        description = ["Will override existing device if it already exists"],
    )
    private var forceCreate: Boolean = false


    private fun printUsage() {
        val messages = listOf(
            "Usage: maestro start-device --os-version=<osVersion> --platform=<platform>\n",
            "Starts or creates an iOS Simulator or Android Emulator similar to the ones on Maestro Cloud",
            "Supported device types: iPhone11 (iOS), Pixel 6 (Android)",
            "Supported os versions:\n* iOS: 15, 16\n* Android: 28, 29, 30, 31, 33",
        )
        PrintUtils.message(messages.joinToString(separator = "\n"))
    }

    override fun call(): Int {
        TestDebugReporter.install(null)

        // platform
        if (!::platform.isInitialized) {
            printUsage()
            PrintUtils.message("Please specify device platform (android, ios):")
            platform = readlnOrNull()?.lowercase() ?: ""
        }

        // default OS version
        if (!::osVersion.isInitialized) {
            osVersion = when (platform) {
                "ios" -> IosDeviceConfig.defaultVersion.toString()
                "android" -> AndroidDeviceConfig.defaultVersion.toString()
                else -> ""
            }
        }

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

                // check connected device
                if (DeviceService.isDeviceConnected(deviceName, Platform.IOS) != null) {
                    PrintUtils.message("A device with name $deviceName is already connected")
                    return 1
                }

                // check existing device
                val existingDeviceId = DeviceService.isDeviceAvailableToLaunch(deviceName, Platform.IOS)?.let {
                    if (forceCreate) {
                        DeviceService.deleteIosDevice(it.modelId)
                        null
                    } else it.modelId
                }

                if (existingDeviceId != null) PrintUtils.message("Using existing device $deviceName (${existingDeviceId}).\nTo override the instance use: --force-create=true")
                else PrintUtils.message("Attempting to create iOS simulator: $deviceName ")


                val deviceUUID = try {
                    existingDeviceId ?: DeviceService.createIosDevice(deviceName, device, runtime).toString()
                } catch (e: DeviceCreateException) {

                    when (e) {
                        is DeviceCreateException.InvalidRuntimeException -> {
                            PrintUtils.err("Required runtime to create the simulator is not installed: $runtime")
                            PrintUtils.err("To install additional iOS runtimes checkout this guide:\n* https://developer.apple.com/documentation/xcode/installing-additional-simulator-runtimes")
                        }

                        is DeviceCreateException.InvalidDeviceTypeException -> {
                            PrintUtils.err("Device type $device is either not supported or not found.")
                        }

                        is DeviceCreateException.UnableToCreateException -> {
                            PrintUtils.err(e.message)
                        }
                    }

                    return 1
                }


                if (existingDeviceId == null) PrintUtils.message("Created simulator $deviceName ($deviceUUID)")

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

                val name = AndroidDeviceConfig.generateDeviceName(version!!)

                // check connected device
                if (DeviceService.isDeviceConnected(name, Platform.ANDROID) != null) {
                    PrintUtils.message("A device with name $name is already connected")
                    return 1
                }

                // existing device
                val existingDevice = if (forceCreate) null else DeviceService.isDeviceAvailableToLaunch(name, Platform.ANDROID)?.modelId

                // dependencies
                if (existingDevice == null && !DeviceService.isAndroidSystemImageInstalled(systemImage)) {
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

                if (existingDevice != null) PrintUtils.message("Using existing device $name.\nTo override the instance use: --force-create=true")
                else PrintUtils.message("Attempting to create Android emulator: $name ")

                val deviceLaunchId = try {
                    existingDevice ?: DeviceService.createAndroidDevice(
                        deviceName = name,
                        device = AndroidDeviceConfig.device,
                        systemImage = systemImage,
                        tag = AndroidDeviceConfig.tag,
                        abi = AndroidDeviceConfig.abi,
                        force = forceCreate
                    )
                } catch (e: DeviceCreateException) {
                    PrintUtils.err("Failed to create emulator ${e.message}")
                    return 1
                }

                if (existingDevice == null) PrintUtils.message("Created Android emulator: $name ($systemImage)")

                PrintUtils.message("Launching emulator...")

                val launchDevice = Device.AvailableForLaunch(
                    modelId = deviceLaunchId,
                    description = deviceLaunchId,
                    platform = Platform.ANDROID
                )

                DeviceService.startDevice(launchDevice)
            }

            else -> {
                PrintUtils.err("Invalid platform. Please specify one of: android, ios")
                return 1
            }
        }


        return 0
    }

}
