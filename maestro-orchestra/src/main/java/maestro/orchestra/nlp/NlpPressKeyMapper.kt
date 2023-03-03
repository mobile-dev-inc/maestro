package maestro.orchestra.nlp

import maestro.KeyCode
import maestro.orchestra.MaestroCommand
import maestro.orchestra.PressKeyCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpPressKeyMapper {

    private val KEY_PATTERN = KeyCode.values()
        .joinToString(separator = "|") { it.description }

    private val PATTERN = "Press (?<target>$KEY_PATTERN)( key)?"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String) = PATTERN.matches(command)

    fun map(command: String): MaestroCommand {
        val target = PATTERN.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?: throw IllegalArgumentException("Not a pressKey command: $command")

        val keyCode = KeyCode.getByName(target)
            ?: throw IllegalArgumentException("Not a pressKey command: $command")

        return MaestroCommand(
            PressKeyCommand(
                code = keyCode
            )
        )
    }

}