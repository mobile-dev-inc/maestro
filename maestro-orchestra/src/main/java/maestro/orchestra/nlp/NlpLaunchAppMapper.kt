package maestro.orchestra.nlp

import maestro.orchestra.LaunchAppCommand
import maestro.orchestra.MaestroCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpLaunchAppMapper {

    private val COMMAND_PATTERN = "(Launch|Start|Open)( (?<target>.*))?"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String): Boolean {
        return COMMAND_PATTERN.matches(command)
    }

    fun map(defaultAppId: String?, command: String): MaestroCommand {
        val target = COMMAND_PATTERN.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?.split(" ")
            ?.firstOrNull()

        val appId = if (target == null
            || target.equals("app", ignoreCase = true)
            || target.equals("application", ignoreCase = true)
        ) {
            defaultAppId
        } else {
            target
        } ?: error("Not a launchApp command: $command")

        return MaestroCommand(
            LaunchAppCommand(
                appId = appId
            )
        )
    }

}