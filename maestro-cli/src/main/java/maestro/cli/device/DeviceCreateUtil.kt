package maestro.cli.device

import maestro.cli.CliError
import maestro.cli.util.DeviceConfigAndroid
import maestro.cli.util.DeviceConfigIos
import maestro.cli.util.PrintUtils

internal object DeviceCreateUtil {

    fun getOrCreateDevice(platform: Platform, osVersion: Int?, forceCreate: Boolean): Device.AvailableForLaunch {
        return when (platform) {
            Platform.ANDROID -> {
                getOrCreateAndroidDevice(osVersion, forceCreate)
            }

            Platform.IOS -> {
                getOrCreateIosDevice(osVersion, forceCreate)
            }

            else -> throw CliError("Unsupported platform $platform. Please one of: android, ios")
        }
    }

    fun getOrCreateIosDevice(version: Int?, forceCreate: Boolean): Device.AvailableForLaunch {
        if (version !in DeviceConfigIos.versions) {
            throw CliError("Provided iOS version is not supported. Please use one of ${DeviceConfigIos.versions}")
        }

        val runtime = DeviceConfigIos.runtimes[version]
        if (runtime == null) {
            throw CliError("Provided iOS runtime is not supported $runtime")
        }

        val deviceName = DeviceConfigIos.generateDeviceName(version!!)
        val device = DeviceConfigIos.device

        // check connected device
        if (DeviceService.isDeviceConnected(deviceName, Platform.IOS) != null) {
            throw CliError("A device with name $deviceName is already connected")
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
        } catch (e: IllegalStateException) {
            val error = e.message ?: ""
            if (error.contains("Invalid runtime")) {
                val msg = "Required runtime to create the simulator is not installed: $runtime\n" +
                        "To install additional iOS runtimes checkout this guide:\n" +
                        "* https://developer.apple.com/documentation/xcode/installing-additional-simulator-runtimes"
                throw CliError(msg)
            } else if (error.contains("Invalid device type")) {
                throw CliError("Device type $device is either not supported or not found.")
            } else {
                throw CliError(error)
            }
        }

        if (existingDeviceId == null) PrintUtils.message("Created simulator $deviceName ($deviceUUID)")

        return Device.AvailableForLaunch(
            modelId = deviceUUID,
            description = deviceName,
            platform = Platform.IOS
        )

    }

    fun getOrCreateAndroidDevice(version: Int?, forceCreate: Boolean): Device.AvailableForLaunch {
        if (version !in DeviceConfigAndroid.versions) {
            throw CliError("Provided Android version is not supported. Please use one of ${DeviceConfigAndroid.versions}")
        }

        val systemImage = DeviceConfigAndroid.systemImages[version]
            ?: throw CliError("Provided system image is not supported. Please use one of ${DeviceConfigAndroid.versions}")

        val name = DeviceConfigAndroid.generateDeviceName(version!!)

        // check connected device
        if (DeviceService.isDeviceConnected(name, Platform.ANDROID) != null) {
            throw CliError("A device with name $name is already connected")
        }

        // existing device
        val existingDevice =
            if (forceCreate) null else DeviceService.isDeviceAvailableToLaunch(name, Platform.ANDROID)?.modelId

        // dependencies
        if (existingDevice == null && !DeviceService.isAndroidSystemImageInstalled(systemImage)) {
            PrintUtils.err("The required system image $systemImage is not installed.")

            PrintUtils.message("Would you like to install it? y/n")
            val r = readlnOrNull()?.lowercase()
            if (r == "y" || r == "yes") {
                PrintUtils.message("Attempting to install $systemImage via Android SDK Manager...\n")
                if (!DeviceService.installAndroidSystemImage(systemImage)) {
                    throw CliError("Was unable to install required dependencies.")
                }
            } else {
                throw CliError(
                    "To install the system image manually, you can run this command:\n${
                        DeviceService.getAndroidSystemImageInstallCommand(
                            systemImage
                        )
                    }"
                )
            }
        }

        if (existingDevice != null) PrintUtils.message("Using existing device $name.\nTo override the instance use: --force-create=true")
        else PrintUtils.message("Attempting to create Android emulator: $name ")

        val deviceLaunchId = try {
            existingDevice ?: DeviceService.createAndroidDevice(
                deviceName = name,
                device = DeviceConfigAndroid.device,
                systemImage = systemImage,
                tag = DeviceConfigAndroid.tag,
                abi = DeviceConfigAndroid.abi,
                force = forceCreate
            )
        } catch (e: IllegalStateException) {
            throw CliError("${e.message}")
        }

        if (existingDevice == null) PrintUtils.message("Created Android emulator: $name ($systemImage)")

        return Device.AvailableForLaunch(
            modelId = deviceLaunchId,
            description = deviceLaunchId,
            platform = Platform.ANDROID
        )
    }
}