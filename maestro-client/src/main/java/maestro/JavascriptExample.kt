package maestro

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class Script : ScriptableObject() {

    override fun getClassName(): String {
        return Script::class.qualifiedName!!
    }

    companion object {

        fun run() {
            Context.enter().use { ctx ->
                val script = Script()
                ctx.initSafeStandardObjects(script)

                script.defineFunctionProperties(
                    arrayOf("ext", "json"),
                    Script::class.java,
                    DONTENUM
                )

                ctx.evaluateString(
                    script,
                    """
                        function json(text) {
                            return JSON.parse(text)
                        }
                        
                        var x = 'Outer'
                    """.trimIndent(),
                    "script",
                    1,
                    null
                )

                val result = ctx.evaluateString(
                    script,
                    """
                    json(ext()).name
                """.trimIndent(),
                    "script",
                    1,
                    null
                )

                println(result)

                val subScript = Script()
                subScript.parentScope = script

                println(
                    ctx.evaluateString(
                        subScript,
                        "x",
                        "script",
                        1,
                        null
                    )
                )

                ctx.evaluateString(
                    subScript,
                    """ 
                        var x = 'Inner'
                    """.trimIndent(),
                    "subScript",
                    1,
                    null
                )

                println(
                    ctx.evaluateString(
                        subScript,
                        "x",
                        "script",
                        1,
                        null
                    )
                )

                println(
                    ctx.evaluateString(
                        script,
                        "x",
                        "script",
                        1,
                        null
                    )
                )
            }
        }

        @JvmStatic
        fun ext(
            ctx: Context,
            thisObj: Scriptable,
            args: Array<Any>,
            funObj: org.mozilla.javascript.Function
        ): String {
            return """
                {
                    "name": "John Doe"
                }
            """.trimIndent()
        }

        @JvmStatic
        fun json(
            ctx: Context,
            thisObj: Scriptable,
            args: Array<Any>,
            funObj: org.mozilla.javascript.Function
        ): Any {
            return mapOf("name" to "Test")
        }

    }

}

fun main() {
    Script.run()
}