package maestro.orchestra.nlp

import maestro.orchestra.MaestroCommand
import maestro.orchestra.RepeatCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpRepeatMapper {

    private val PATTERN = "(?<command>.*) (?<times>\\d+) (time|times)"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String): Boolean {
        return PATTERN.matches(command)
    }

    fun map(command: String, appId: String): MaestroCommand {
        val matchResult = PATTERN.matchEntire(command)
            ?: error("Not a valid repeat command: $command")

        val times = matchResult.groups["times"]
            ?.value
            ?.toIntOrNull()
            ?: error("Not a valid repeat command: $command")

        val subCommand = matchResult.groups["command"]
            ?.value
            ?.let { NlpMapper.map(it, appId) }
            ?: error("Not a valid repeat command: $command")

        return MaestroCommand(
            RepeatCommand(
                times = times.toString(),
                commands = listOf(subCommand),
            )
        )
    }

}