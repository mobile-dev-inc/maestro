package maestro.orchestra.nlp

import maestro.orchestra.BackPressCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.WaitForAnimationToEndCommand

object NlpWaitForAnimationToEndMapper {

    fun matches(command: String): Boolean {
        return command.startsWith("Wait", ignoreCase = true)
    }

    fun map(command: String): MaestroCommand {
        return MaestroCommand(
            WaitForAnimationToEndCommand(
                timeout = null,
            )
        )
    }

}