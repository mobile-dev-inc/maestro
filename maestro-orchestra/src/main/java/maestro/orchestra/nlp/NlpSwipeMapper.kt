package maestro.orchestra.nlp

import maestro.SwipeDirection
import maestro.orchestra.MaestroCommand
import maestro.orchestra.SwipeCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpSwipeMapper {

    private val SWIPE_DIRECTIONS = listOf(
        "left to right",
        "right to left",
        "top to bottom",
        "bottom to top",
    )

    private val SWIPE_DIRECTIONS_REGEX = SWIPE_DIRECTIONS
        .joinToString("|")
        .toRegexSafe(RegexOption.IGNORE_CASE)

    private val PATTERN_A = "Swipe from (?<target>$SWIPE_DIRECTIONS_REGEX)"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    private val PATTERN_B = "Swipe (?<target>.*)"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String) = command.startsWith("Swipe", ignoreCase = true)

    fun map(command: String): MaestroCommand {
        if (PATTERN_A.matches(command)) {
            return patternA(command)
        }

        if (PATTERN_B.matches(command)) {
            return patternB(command)
        }

        return MaestroCommand(
            SwipeCommand(
                direction = SwipeDirection.LEFT,
            )
        )
    }

    private fun patternB(command: String): MaestroCommand {
        val directionStr = PATTERN_B.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?: error("Not a valid swipe command: $command")

        val direction = when (directionStr) {
            "left" -> SwipeDirection.LEFT
            "right" -> SwipeDirection.RIGHT
            "up" -> SwipeDirection.UP
            "down" -> SwipeDirection.DOWN
            else -> error("Not a valid swipe command: $command")
        }

        return MaestroCommand(
            SwipeCommand(
                direction = direction,
            )
        )
    }

    private fun patternA(command: String): MaestroCommand {
        val directionStr = PATTERN_A.matchEntire(command)
            ?.groups
            ?.get("target")
            ?.value
            ?: error("Not a valid swipe command: $command")

        val direction = when (directionStr) {
            "left to right" -> SwipeDirection.RIGHT
            "right to left" -> SwipeDirection.LEFT
            "top to bottom" -> SwipeDirection.DOWN
            "bottom to top" -> SwipeDirection.UP
            else -> error("Not a valid swipe command: $command")
        }

        return MaestroCommand(
            SwipeCommand(
                direction = direction,
            )
        )
    }

}