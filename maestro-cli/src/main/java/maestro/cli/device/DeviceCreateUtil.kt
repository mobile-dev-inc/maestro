package maestro.cli.device

import maestro.cli.util.DeviceConfigAndroid
import maestro.cli.util.DeviceConfigIos
import maestro.cli.util.PrintUtils

object DeviceCreateUtil {

    fun getOrCreateDevice(platform: Platform, osVersion: Int?, forceCreate: Boolean): Device? {
        return when (platform) {
            Platform.ANDROID -> {
                getOrCreateAndroidDevice(osVersion, forceCreate)
            }

            Platform.IOS -> {
                getOrCreateIosDevice(osVersion, forceCreate)
            }

            else -> throw IllegalArgumentException("Unsupported platform $platform")
        }
    }

    fun getOrCreateIosDevice(version: Int?, forceCreate: Boolean): Device.AvailableForLaunch? {
        if (version !in DeviceConfigIos.versions) {
            PrintUtils.err("Provided iOS version is not supported. Please use one of ${DeviceConfigIos.versions}")
            return null
        }

        val runtime = DeviceConfigIos.runtimes[version]
        if (runtime == null) {
            PrintUtils.err("Provided iOS runtime is not supported $runtime")
            return null
        }

        val deviceName = DeviceConfigIos.generateDeviceName(version!!)
        val device = DeviceConfigIos.device

        // check connected device
        if (DeviceService.isDeviceConnected(deviceName, Platform.IOS) != null) {
            PrintUtils.warn("A device with name $deviceName is already connected")
            return null
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
                PrintUtils.err("Required runtime to create the simulator is not installed: $runtime")
                PrintUtils.err("To install additional iOS runtimes checkout this guide:\n* https://developer.apple.com/documentation/xcode/installing-additional-simulator-runtimes")
            } else if (error.contains("Invalid device type")) {
                PrintUtils.err("Device type $device is either not supported or not found.")
            } else {
                PrintUtils.err(error)
            }

            return null
        }

        if (existingDeviceId == null) PrintUtils.message("Created simulator $deviceName ($deviceUUID)")

        return Device.AvailableForLaunch(
            modelId = deviceUUID,
            description = deviceName,
            platform = Platform.IOS
        )

    }

    fun getOrCreateAndroidDevice(version: Int?, forceCreate: Boolean): Device.AvailableForLaunch? {
        if (version !in DeviceConfigAndroid.versions) {
            PrintUtils.err("Provided Android version is not supported. Please use one of ${DeviceConfigAndroid.versions}")
            return null
        }

        val systemImage = DeviceConfigAndroid.systemImages[version]
        if (systemImage == null) {
            PrintUtils.err("Provided system image is not supported. Please use one of ${DeviceConfigAndroid.versions}")
            return null
        }

        val name = DeviceConfigAndroid.generateDeviceName(version!!)

        // check connected device
        if (DeviceService.isDeviceConnected(name, Platform.ANDROID) != null) {
            PrintUtils.warn("A device with name $name is already connected")
            return null
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
                    PrintUtils.err("Was unable to install required dependencies.")
                    return null
                }
            } else {
                PrintUtils.warn(
                    "To install the system image manually, you can run this command:\n${
                        DeviceService.getAndroidSystemImageInstallCommand(
                            systemImage
                        )
                    }"
                )
                return null
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
            PrintUtils.err("${e.message}")
            return null
        }

        if (existingDevice == null) PrintUtils.message("Created Android emulator: $name ($systemImage)")

        return Device.AvailableForLaunch(
            modelId = deviceLaunchId,
            description = deviceLaunchId,
            platform = Platform.ANDROID
        )
    }
}