package maestro.cli.util

object CiUtils {
    private val ciEnvVarMap = mapOf(
        "APPVEYOR" to "appveyor",
        "BITBUCKET_BUILD_NUMBER" to "bitbucket",
        "BITRISE_IO" to "bitrise",
        "BUILDKITE" to "buildkite",
        "CIRCLECI" to "circleci",
        "CIRRUS_CI" to "cirrusci",
        "DRONE" to "drone",
        "GITHUB_ACTIONS" to "github",
        "GITLAB_CI" to "gitlab",
        "JENKINS_HOME" to "jenkins",
        "TEAMCITY_VERSION" to "teamcity",
        "CI" to "ci"
    )

    private fun isTruthy(envVar: String?): Boolean {
        if (envVar == null) return false
        return envVar != "0" && envVar != "false"
    }

    fun getCiProvider(): String? {
        val mdevCiEnvVar = System.getenv("MDEV_CI")
        if (isTruthy(mdevCiEnvVar)) {
            return mdevCiEnvVar
        }

        for (ciEnvVar in ciEnvVarMap.entries) {
            try {
                if (isTruthy(System.getenv(ciEnvVar.key).lowercase())) return ciEnvVar.value
            } catch (e: Exception) {
                // We don't care
            }
        }

        return null
    }
}
