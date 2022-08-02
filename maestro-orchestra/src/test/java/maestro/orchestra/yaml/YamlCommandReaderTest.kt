package maestro.orchestra.yaml

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import maestro.orchestra.MaestroCommand
import maestro.orchestra.NoInputException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File

@Suppress("TestFunctionName")
internal class YamlCommandReaderTest {

    @JvmField
    @Rule
    val name: TestName = TestName()

    @Test
    fun T001_empty() = expectException<NoInputException>()

    private inline fun <reified T : Throwable> expectException(block: (e: T) -> Unit = {}) {
        try {
            parseCommands()
            assertWithMessage("Expected exception: ${T::class.java}").fail()
        } catch (e: Throwable) {
            assertThat(e).isInstanceOf(T::class.java)
            block(e as T)
        }
    }

    private fun runTest(
        expectedCommands: List<MaestroCommand>,
    ) {
        parseCommands()
    }

    private fun parseCommands(): List<MaestroCommand> {
        val resourceName = name.methodName.removePrefix("T") + ".yaml"
        val resource = this::class.java.getResource("/$resourceName")!!
        val resourceFile = File(resource.toURI())
        return YamlCommandReader.readCommands(resourceFile)
    }
}