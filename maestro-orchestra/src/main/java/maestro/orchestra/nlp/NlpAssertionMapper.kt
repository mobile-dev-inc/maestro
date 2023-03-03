package maestro.orchestra.nlp

import maestro.orchestra.AssertConditionCommand
import maestro.orchestra.Condition
import maestro.orchestra.ElementSelector
import maestro.orchestra.MaestroCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpAssertionMapper {

    private val PROPERTY = "(visible|hidden|enabled|disabled|selected|unselected|checked|unchecked|focused|unfocused)"
    private val PROPERTY_REGEX = PROPERTY
        .toRegexSafe(RegexOption.IGNORE_CASE)

    private val PATTERNS = listOf(
        "(?<target>.*) should (not )?be $PROPERTY".toRegexSafe(RegexOption.IGNORE_CASE),
        "(Assert|Check|Verify|Expect|Ensure) (that )?(?<target>.*) is (not )?$PROPERTY".toRegexSafe(RegexOption.IGNORE_CASE),
        "User should (not )?see (a )?(?<target>.*)".toRegexSafe(RegexOption.IGNORE_CASE),
    )

    fun matches(command: String) = (command.contains("Assert")
        || command.contains("Check")
        || command.contains("Verify")
        || command.contains("Expect")
        || command.contains("should"))

    fun map(command: String): MaestroCommand {
        val selector = mapSelector(command)

        return MaestroCommand(
            command = AssertConditionCommand(
                condition = Condition(
                    visible = selector.takeUnless { isNegated(command) },
                    notVisible = selector.takeIf { isNegated(command) },
                )
            )
        )
    }

    private fun isNegated(command: String) = command.contains("not")
        || command.contains("hidden")
        || command.contains("disabled")
        || command.contains("unselected")
        || command.contains("unchecked")
        || command.contains("unfocused")

    private fun mapSelector(command: String): ElementSelector {
        val target = PATTERNS
            .firstNotNullOfOrNull { it.matchEntire(command)?.groups?.get("target")?.value }
            ?: error("Not an assertion command: $command")

        val unquoted = target.replace("\"", "")

        val regex = "$target|$unquoted"

        var selector = ElementSelector(
            textRegex = regex,
        )

        if (command.contains(PROPERTY_REGEX)) {
            PROPERTY_REGEX
                .findAll(command)
                .lastOrNull()
                ?.value
                ?.let {
                    if (it == "checked" || it == "unchecked") {
                        selector = selector.copy(
                            checked = true,
                        )
                    }

                    if (it == "selected" || it == "unselected") {
                        selector = selector.copy(
                            selected = true,
                        )
                    }

                    if (it == "focused" || it == "unfocused") {
                        selector = selector.copy(
                            focused = true,
                        )
                    }

                    if (it == "enabled" || it == "disabled") {
                        selector = selector.copy(
                            enabled = true,
                        )
                    }
                }
        }

        return selector
    }

}