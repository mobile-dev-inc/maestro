package maestro.orchestra.nlp

import maestro.orchestra.BackPressCommand
import maestro.orchestra.MaestroCommand

object NlpGoBackMapper {

    fun matches(command: String): Boolean {
        return command.equals("Back", ignoreCase = true)
            || command.startsWith("Go back", ignoreCase = true)
    }

    fun map(command: String): MaestroCommand {
        return MaestroCommand(
            BackPressCommand()
        )
    }

}