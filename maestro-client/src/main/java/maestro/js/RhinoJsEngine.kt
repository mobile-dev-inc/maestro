package maestro.js

import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import java.util.concurrent.TimeUnit

class RhinoJsEngine(
    httpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .protocols(listOf(Protocol.HTTP_1_1))
        .followRedirects(System.getenv("MAESTRO_FOLLOW_REDIRECTS")?.equals("true")?: true)
        .followSslRedirects(System.getenv("MAESTRO_FOLLOW_REDIRECTS")?.equals("true")?: true)
        .build(),
) : JsEngine {

    private val context = Context.enter()

    private var currentScope = JsScope(root = true)
    private var onLogMessage: (String) -> Unit = {}

    init {
        context.initSafeStandardObjects(currentScope)

        val jsHttp = JsHttp(httpClient)
        jsHttp.defineFunctionProperties(
            arrayOf("request", "get", "post", "put", "delete"),
            JsHttp::class.java,
            ScriptableObject.DONTENUM
        )
        context.initSafeStandardObjects(jsHttp)
        currentScope.put("http", currentScope, jsHttp)

        val jsConsole = JsConsole(
            onLogMessage = { onLogMessage(it) }
        )
        jsConsole.defineFunctionProperties(
            arrayOf("log"),
            JsConsole::class.java,
            ScriptableObject.DONTENUM
        )
        context.initSafeStandardObjects(jsConsole)
        currentScope.put("console", currentScope, jsConsole)

        context.evaluateString(
            currentScope,
            Js.initScript,
            "maestro-runtime",
            1,
            null
        )
        currentScope.sealObject()

        // We are entering a sub-scope so that no more declarations can be made
        // on the root scope that is now sealed.
        enterScope()
    }

    override fun close() {
        context.close()
    }

    override fun onLogMessage(callback: (String) -> Unit) {
        onLogMessage = callback
    }

    override fun enterScope() {
        val subScope = JsScope(root = false)
        subScope.parentScope = currentScope
        currentScope = subScope
    }

    override fun leaveScope() {
        currentScope = currentScope.parentScope as JsScope
    }

    override fun setCopiedText(text: String?) {
        evaluateScript("maestro.copiedText = '${Js.sanitizeJs(text ?: "")}'")
    }

    override fun putEnv(key: String, value: String) {
        val cleanValue = Js.sanitizeJs(value)
        evaluateScript("var $key = '$cleanValue'")
    }

    override fun evaluateScript(
        script: String,
        env: Map<String, String>,
        sourceName: String,
        runInSubScope: Boolean,
    ): Any? {
        val scope = if (runInSubScope) {
            // We create a new scope for each evaluation to prevent local variables
            // from clashing with each other across multiple scripts.
            // Only 'output' is shared across scopes.
            JsScope(root = false)
                .apply { parentScope = currentScope }
        } else {
            currentScope
        }

        if (env.isNotEmpty()) {
            env.forEach { (key, value) ->
                val wrappedValue = Context.javaToJS(value, scope)
                ScriptableObject.putProperty(scope, key, wrappedValue)
            }
        }

        return context.evaluateString(
            scope,
            script,
            sourceName,
            1,
            null
        )
    }

}