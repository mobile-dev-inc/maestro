package maestro.js

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.mozilla.javascript.Context

class JsEngine {

    private lateinit var context: Context
    private lateinit var currentScope: JsScope

    fun init() {
        context = Context.enter()
        currentScope = JsScope()
        context.initSafeStandardObjects(currentScope)

        context.evaluateString(
            currentScope,
            """
                function json(text) {
                    return JSON.parse(text)
                }
                
                var ext = {}
                var output = {}
            """.trimIndent(),
            "maestro-runtime",
            1,
            null
        )
    }

    fun enterScope() {
        val subScope = JsScope()
        subScope.parentScope = currentScope
        currentScope = subScope

        // Each subscope keeps its own version of output to not override the one from its parent
        context.evaluateString(
            currentScope,
            """
                var output = {}
            """.trimIndent(),
            "inline-script",
            1,
            null
        ).toString()
    }

    fun leaveScope() {
        currentScope = currentScope.parentScope as JsScope
    }

    fun evaluateScript(
        script: String,
        env: Map<String, String> = emptyMap(),
        sourceName: String = "inline-script"
    ): String {
        // We create a new scope for each evaluation to prevent local variables
        // from clashing with each other across mutliple scripts.
        // Only 'output' and 'ext' are shared across scopes.
        val subScope = JsScope()
        subScope.parentScope = currentScope

        if (env.isNotEmpty()) {
            env.forEach { (key, value) ->
                context.evaluateString(
                    subScope,
                    "var $key = '${Jsoup.clean(value, Safelist.none())}'",
                    sourceName,
                    1,
                    null
                ).toString()
            }
        }

        return context.evaluateString(
            subScope,
            script,
            sourceName,
            1,
            null
        ).toString()
    }

}