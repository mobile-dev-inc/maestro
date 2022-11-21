package maestro

import groovy.lang.Binding
import groovy.lang.GroovyShell

fun main() {
    val shell = GroovyShell(
        Binding(
            mutableMapOf(
                "ext" to { println("Hello from Groovy") },
                "VALUE" to "Injected Value",
            )
        )
    )

    // Defining a convenience json parser
    shell.evaluate(
        """
        def json(text) {
            def jsonSlurper = new groovy.json.JsonSlurper()
            return jsonSlurper.parseText(text)
        }
        json = this::json
    """.trimIndent()
    )

    // Parse JSON and use a field
    println(shell.evaluate("json('{ \"name\": \"John Doe\" }').name"))

    // Inject VALUE env parameter
    println(shell.evaluate("\"\${VALUE}\""))

    // Call Java from Groovy (but only the function that we defined)
    shell.evaluate("ext.invoke()")
}