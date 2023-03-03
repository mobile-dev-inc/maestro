package maestro.orchestra.nlp

import maestro.orchestra.MaestroCommand

object NlpMapper {

    fun map(
        action: String,
        appId: String?,
    ): MaestroCommand? {
        val type = CommandClassifier.classify(action)

        return when (type) {
            CommandClassifier.CommandType.TAP -> NlpTapMapper.map(action)
            CommandClassifier.CommandType.ASSERTION -> NlpAssertionMapper.map(action)
            CommandClassifier.CommandType.INPUT_TEXT -> NlpInputTextMapper.map(action)
            CommandClassifier.CommandType.PRESS_KEY -> NlpPressKeyMapper.map(action)
            CommandClassifier.CommandType.LAUNCH_APP -> NlpLaunchAppMapper.map(
                defaultAppId = appId,
                command = action,
            )
            CommandClassifier.CommandType.GO_BACK -> NlpGoBackMapper.map(action)
            CommandClassifier.CommandType.WAIT_FOR_ANIMATION -> NlpWaitForAnimationToEndMapper.map(action)
            CommandClassifier.CommandType.SWIPE -> NlpSwipeMapper.map(action)
            CommandClassifier.CommandType.UNKNOWN -> null
        }
    }

}