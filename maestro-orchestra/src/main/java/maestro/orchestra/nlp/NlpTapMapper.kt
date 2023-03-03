package maestro.orchestra.nlp

import maestro.orchestra.ElementSelector
import maestro.orchestra.MaestroCommand
import maestro.orchestra.TapOnElementCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpTapMapper {

    private val PATTERN = "(Tap|Click|Press|Touch) (on )?(?<target>.*)".toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String) = (command.contains("Tap")
        || command.contains("Click")
        || command.contains("Press")
        || command.contains("Touch"))

    fun map(command: String): MaestroCommand {
        val target = PATTERN.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?: throw IllegalArgumentException("Not a tapOn command: $command")

        val unquoted = target.replace("\"", "")

        val regex = "$target|$unquoted"

        return MaestroCommand(
            command = TapOnElementCommand(
                selector = ElementSelector(
                    textRegex = regex,
                )
            )
        )
    }

}