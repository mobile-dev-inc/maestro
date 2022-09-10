package maestro.orchestra

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import maestro.Point
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

    @Test
    fun `serialize TapOnPointCommand`() {
        // given
        val command = MaestroCommand(
            tapOnPoint = TapOnPointCommand(
                x = 100,
                y = 100,
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
              "tapOnPoint" : {
                "x" : 100,
                "y" : 100,
                "retryIfNoChange" : false,
                "waitUntilVisible" : true,
                "longPress" : false
              }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    @Test
    fun `serialize ScrollCommand`() {
        // given
        val command = MaestroCommand(
            scrollCommand = ScrollCommand()
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "scrollCommand" : { }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    @Test
    fun `serialize SwipeCommand`() {
        // given
        val command = MaestroCommand(
            swipeCommand = SwipeCommand(
                Point(10, 10),
                Point(100, 100),
            )
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "swipeCommand" : {
                "startPoint" : {
                  "x" : 10,
                  "y" : 10
                },
                "endPoint" : {
                  "x" : 100,
                  "y" : 100
                }
              }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    @Test
    fun `serialize BackPressCommand`() {
        // given
        val command = MaestroCommand(
            backPressCommand = BackPressCommand()
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "backPressCommand" : { }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    @Test
    fun `serialize AssertCommand`() {
        // given
        val command = MaestroCommand(
            assertCommand = AssertCommand(
                ElementSelector(textRegex = "[A-f0-9]"),
                ElementSelector(textRegex = "\\s")
            )
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "assertCommand" : {
                "visible" : {
                  "textRegex" : "[A-f0-9]",
                  "optional" : false
                },
                "notVisible" : {
                  "textRegex" : "\\s",
                  "optional" : false
                }
              }
            }
          """.trimIndent()
        assertThat(serializedCommandJson)
            .isEqualTo(expectedJson)
    }

    @Test
    fun `serialize InputTextCommand`() {
        // given
        val command = MaestroCommand(
            inputTextCommand = InputTextCommand("Hello, world!")
        )

        // when
        val serializedCommandJson = command.toJson()

        // the
        @Language("json")
        val expectedJson = """
            {
              "inputTextCommand" : {
                "text" : "Hello, world!"
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
