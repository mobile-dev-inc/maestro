package maestro.orchestra.nlp

object CommandClassifier {

    fun classify(command: String): CommandType {
        if (NlpPressKeyMapper.matches(command)) {
            return CommandType.PRESS_KEY
        }

        if (NlpTapMapper.matches(command)) {
            return CommandType.TAP
        }

        if (NlpAssertionMapper.matches(command)) {
            return CommandType.ASSERTION
        }

        if (NlpLaunchAppMapper.matches(command)) {
            return CommandType.LAUNCH_APP
        }

        if (NlpInputTextMapper.matches(command)) {
            return CommandType.INPUT_TEXT
        }

        if (NlpGoBackMapper.matches(command)) {
            return CommandType.GO_BACK
        }

        if (NlpWaitForAnimationToEndMapper.matches(command)) {
            return CommandType.WAIT_FOR_ANIMATION
        }

        if (NlpSwipeMapper.matches(command)) {
            return CommandType.SWIPE
        }

        return CommandType.UNKNOWN
    }

    enum class CommandType {

        TAP,
        ASSERTION,
        INPUT_TEXT,
        PRESS_KEY,
        LAUNCH_APP,
        GO_BACK,
        WAIT_FOR_ANIMATION,
        SWIPE,
        UNKNOWN

    }

}