package maestro.cli.util

object CiUtils {
    private val ciEnvVarMap = mapOf(
        "JENKINS_HOME" to "jenkins",
        "BITRISE_IO" to "bitrise",
        "CIRCLECI" to "circleci",
        "GITLAB_CI" to "gitlab",
        "GITHUB_ACTIONS" to "github",
        "BITBUCKET_BUILD_NUMBER" to "bitbucket",
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
