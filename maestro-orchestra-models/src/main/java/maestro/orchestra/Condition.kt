package maestro.orchestra

import maestro.js.JsEngine

data class Condition(
    val visible: ElementSelector? = null,
    val notVisible: ElementSelector? = null,
    val scriptCondition: String? = null,
    private val scriptDescription: String? = scriptCondition,
) {

    fun evaluateScripts(jsEngine: JsEngine): Condition {
        // Note that we are not evaluating `scriptCondition` here, because it is evaluated
        // at the time of condition check

        return copy(
            visible = visible?.evaluateScripts(jsEngine),
            notVisible = notVisible?.evaluateScripts(jsEngine),
            scriptDescription = scriptCondition?.let { jsEngine.evaluateScript(it) }
                .toString()
        )
    }

    fun description(): String {
        val descriptions = mutableListOf<String>()

        visible?.let {
            descriptions.add("${it.description()} is visible")
        }

        notVisible?.let {
            descriptions.add("${it.description()} is not visible")
        }

        scriptDescription?.let {
            descriptions.add("\${$it} is true")
        }

        return if (descriptions.isEmpty()) {
            "true"
        } else {
            descriptions.joinToString(" and ")
        }
    }

}
