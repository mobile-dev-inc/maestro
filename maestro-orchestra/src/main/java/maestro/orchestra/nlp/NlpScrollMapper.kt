package maestro.orchestra.nlp

import maestro.SwipeDirection
import maestro.orchestra.MaestroCommand
import maestro.orchestra.ScrollCommand
import maestro.orchestra.SwipeCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpScrollMapper {

    private val PATTERN = "Scroll( (?<direction>.*))?"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String) = PATTERN.matches(command)

    fun map(command: String): MaestroCommand {
        val directionStr = PATTERN.matchEntire(command)
            ?.groups
            ?.get("direction")
            ?.value
            ?: return MaestroCommand(ScrollCommand())

        return when (directionStr.lowercase()) {
            "down" -> MaestroCommand(ScrollCommand())
            "up" -> MaestroCommand(
                SwipeCommand(
                    direction = SwipeDirection.DOWN,
                )
            )
            "left" -> MaestroCommand(
                SwipeCommand(
                    direction = SwipeDirection.RIGHT,
                )
            )
            "right" -> MaestroCommand(
                SwipeCommand(
                    direction = SwipeDirection.LEFT,
                )
            )
            else -> error("Not a valid swipe command: $command")
        }
    }

}