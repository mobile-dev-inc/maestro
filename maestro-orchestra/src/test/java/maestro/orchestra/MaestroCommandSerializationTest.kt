package maestro.orchestra

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

internal class MaestroCommandSerializationTest {
    @Test
    fun `serialize TapOnElementCommand`() {
        // given
        val command = MaestroCommand(
            tapOnElement = TapOnElementCommand(
                selector = ElementSelector(textRegex = "[A-f0-9]"),
                retryIfNoChange = false,
                waitUntilVisible = true,
                longPress = false,
            )
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "tapOnElement" : {
                "selector" : {
                  "textRegex" : "[A-f0-9]",
                  "optional" : false
                },
                "retryIfNoChange" : false,
                "waitUntilVisible" : true,
                "longPress" : false
              }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    private fun MaestroCommand.toJson(): String =
        objectWriter().writeValueAsString(this)

    private fun objectWriter(): ObjectWriter {
        return ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .registerModule(KotlinModule.Builder().build())
            .writerWithDefaultPrettyPrinter()
    }
}
