package maestro.cli.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object EnvUtils {

    fun androidHome(): String? {
        return System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: System.getenv("ANDROID_SDK_HOME")
            ?: System.getenv("ANDROID_SDK")
            ?: System.getenv("ANDROID")
    }

    fun maestroCloudApiKey(): String? {
        return System.getenv("MAESTRO_CLOUD_API_KEY")
    }

    /**
     * @return true, if we're executing from Windows Linux shell (WSL)
     */
    fun isWSL(): Boolean {
        try {
            val p1 = ProcessBuilder("printenv", "WSL_DISTRO_NAME").start()
            if (!p1.waitFor(20, TimeUnit.SECONDS)) throw TimeoutException()
            if (p1.exitValue() == 0 && String(p1.inputStream.readBytes()).trim().isNotEmpty()) {
                return true
            }

            val p2 = ProcessBuilder("printenv", "IS_WSL").start()
            if (!p2.waitFor(20, TimeUnit.SECONDS)) throw TimeoutException()
            if (p2.exitValue() == 0 && String(p2.inputStream.readBytes()).trim().isNotEmpty()) {
                return true
            }
        } catch (ignore: Exception) {
            // ignore
        }

        return false
    }

    fun getJavaVersion(): String? {
        return runCatching {
            val processBuilder = ProcessBuilder("java", "-version")
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.errorStream)) // java -version prints to error stream

            val javaVersionLine = reader.readLine()
            val versionPattern = "\"(.*?)\"".toRegex() // capture the version between double quotes
            val matchResult = versionPattern.find(javaVersionLine)

            return matchResult?.groups?.get(1)?.value // return matched version or null if not found
        }.getOrNull()
    }

}