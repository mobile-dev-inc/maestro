package maestro.orchestra.util

object Secrets {

    fun String.injectSecrets(secrets: Map<String, String>): String {
        if (secrets.isEmpty()) {
            return this
        }

        return secrets
            .entries
            .fold(this) { acc, (key, value) ->
                acc.replace("\\$\\{$key}".toRegex(), value)
            }
    }

}