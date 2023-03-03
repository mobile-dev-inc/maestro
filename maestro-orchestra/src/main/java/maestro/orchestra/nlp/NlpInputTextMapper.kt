package maestro.orchestra.nlp

import maestro.orchestra.InputTextCommand
import maestro.orchestra.MaestroCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpInputTextMapper {

    private val PATTERN = "(Type|Input|Enter) (?<target>.*)".toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String) = (command.contains("Input")
        || command.contains("Enter")
        || command.contains("Type"))

    fun map(command: String): MaestroCommand {
        val target = PATTERN.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?.replace("\"", "")
            ?: throw IllegalArgumentException("Not an inputText command: $command")

        return MaestroCommand(
            InputTextCommand(
                text = target
            )
        )
    }

}