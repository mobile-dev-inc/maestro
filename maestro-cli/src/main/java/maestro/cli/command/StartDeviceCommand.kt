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

                validateLocale(language, country)

                return Pair(language, country)
            } else {
                throw CliError("Wrong device locale format was provided $it. A combination of lowercase ISO-639-1 code and uppercase ISO-3166-1 code should be used, i.e. \"de_DE\" for Germany. More info can be found here https://maestro.mobile.dev/")
            }
        }

        // use en_US locale as a default
        return Pair("en", "US")
    }

    private fun validateLocale(language: String, country: String) {
        if (!SUPPORTED_LANGUAGES.map { it.first }.contains(language)) {
            val languages = SUPPORTED_LANGUAGES.joinToString("\n")
            throw CliError("$language language is currently not supported by Maestro, please check that it is a valid ISO-639-1 code. Here is a full list of supported languages:\n" +
                    "\n" +
                    languages
            )
        }
        if (!SUPPORTED_COUNTRIES.map { it.first }.contains(country)) {
            val countries = SUPPORTED_COUNTRIES.joinToString("\n")
            throw CliError("$country country is currently not supported by Maestro, please check that it is a valid ISO-3166-1 code. Here is a full list of supported countries:\n" +
                    "\n" +
                    countries
            )
        }
    }

    companion object {
        // ISO-639-1
        private val SUPPORTED_LANGUAGES = listOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "ar" to "Arabic",
            "ru" to "Russian",
            "pt" to "Portuguese",
            "it" to "Italian",
            "nl" to "Dutch",
            "sv" to "Swedish",
            "no" to "Norwegian",
            "da" to "Danish",
            "fi" to "Finnish",
            "tr" to "Turkish",
            "he" to "Hebrew",
            "el" to "Greek",
            "th" to "Thai",
            "hi" to "Hindi",
            "uk" to "Ukrainian",
            "vi" to "Vietnamese",
            "ms" to "Malay",
            "id" to "Indonesian"
        )

        // ISO-3166-1
        private val SUPPORTED_COUNTRIES = listOf(
            "US" to "United States",
            "GB" to "United Kingdom",
            "CA" to "Canada",
            "AU" to "Australia",
            "DE" to "Germany",
            "FR" to "France",
            "JP" to "Japan",
            "CN" to "China",
            "IN" to "India",
            "BR" to "Brazil",
            "MX" to "Mexico",
            "KR" to "South Korea",
            "RU" to "Russia",
            "ES" to "Spain",
            "IT" to "Italy",
            "NL" to "Netherlands",
            "BE" to "Belgium",
            "CH" to "Switzerland",
            "SE" to "Sweden",
            "NO" to "Norway",
            "DK" to "Denmark",
            "FI" to "Finland",
            "TR" to "Turkey",
            "AE" to "United Arab Emirates",
            "UA" to "Ukraine",
            "SA" to "Saudi Arabia",
            "ZA" to "South Africa",
            "SG" to "Singapore",
            "MY" to "Malaysia",
            "ID" to "Indonesia",
            "TH" to "Thailand"
        )
    }
}
