package maestro.orchestra.nlp

import maestro.orchestra.EraseTextCommand
import maestro.orchestra.MaestroCommand
import maestro.utils.StringUtils.toRegexSafe

object NlpEraseTextMapper {

    private val PATTERN = "(Erase|Clear|Delete) (text|field|input|all)"
        .toRegexSafe(RegexOption.IGNORE_CASE)

    fun matches(command: String): Boolean {
        return PATTERN.matches(command)
    }

    fun map(command: String): MaestroCommand {
        return MaestroCommand(
            EraseTextCommand(charactersToErase = null),
        )
    }

}