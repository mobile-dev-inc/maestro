package maestro.orchestra.util

object Secrets {

    fun String.injectSecrets(env: Map<String, String>): String {
        if (env.isEmpty()) {
            return this
        }

        return env
            .entries
            .fold(this) { acc, (key, value) ->
                acc.replace("\\$\\{$key}".toRegex(), value)
            }
    }

}