package maestro.orchestra.yaml

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import maestro.orchestra.ApplyConfigurationCommand
import maestro.orchestra.BackPressCommand
import maestro.orchestra.Command
import maestro.orchestra.LaunchAppCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroConfig
import maestro.orchestra.MaestroInitFlow
import maestro.orchestra.ScrollCommand
import maestro.orchestra.error.InvalidInitFlowFile
import maestro.orchestra.error.SyntaxError
import maestro.orchestra.yaml.junit.YamlFile
import maestro.orchestra.yaml.junit.YamlCommandsExtension
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.rules.TestName
import java.nio.file.FileSystems
import java.nio.file.Paths

@Suppress("TestFunctionName")
@ExtendWith(YamlCommandsExtension::class)
internal class YamlCommandReaderTest {

    @JvmField
    @Rule
    val name: TestName = TestName()

    @Test
    fun T001_empty() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun launchApp(
        @YamlFile("002_launchApp.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
                ApplyConfigurationCommand(MaestroConfig(
                    appId = "com.example.app"
                )),
                LaunchAppCommand(
                    appId = "com.example.app"
                ),
        )
    }

    @Test
    fun launchApp_withClearState(
        @YamlFile("003_launchApp_withClearState.yaml") commands: List<Command>
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
            )),
            LaunchAppCommand(
                appId = "com.example.app",
                clearState = true,
            ),
        )
    }

    @Test
    fun T004_config_empty() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun T005_config_noAppId() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("appId due to missing (therefore NULL) value for creator parameter appId which is a non-nullable type")
    }

    @Test
    fun T006_emptyCommands() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun initFlow(
        @YamlFile("007_initFlow.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
                initFlow = MaestroInitFlow(
                    appId = "com.example.app",
                    commands = commands(
                        LaunchAppCommand(
                            appId = "com.example.app",
                            clearState = true,
                        )
                    ),
                )
            )),
            LaunchAppCommand(
                appId = "com.example.app",
            ),
        )
    }

    @Test
    fun config_unknownKeys(
        @YamlFile("008_config_unknownKeys.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
                ext = mapOf(
                    "extra" to true,
                    "extraMap" to mapOf(
                        "keyA" to "valueB"
                    ),
                    "extraArray" to listOf("itemA")
                )
            )),
            LaunchAppCommand(
                appId = "com.example.app",
            ),
        )
    }

    @Test
    fun T009_invalidCommand() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Unrecognized field \"invalid\"")
    }

    @Test
    fun T010_invalidCommand_string() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Invalid command: \"invalid\"")
    }

    @Test
    fun initFlow_file(
        @YamlFile("011_initFlow_file.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
                initFlow = MaestroInitFlow(
                    appId = "com.example.app",
                    commands = commands(
                        ApplyConfigurationCommand(
                            config = MaestroConfig(
                                appId = "com.example.app",
                            )
                        ),
                        LaunchAppCommand(
                            appId = "com.example.app",
                        )
                    ),
                ),
            )),
            LaunchAppCommand(
                appId = "com.example.app",
            ),
        )
    }

    @Test
    fun initFlow_emptyString(
        @YamlFile("012_initFlow_emptyString.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
            )),
            LaunchAppCommand(
                appId = "com.example.app",
            ),
        )
    }

    @Test
    fun T013_initFlow_invalidFile() = expectException<InvalidInitFlowFile>()

    @Test
    fun T014_initFlow_recursive() = expectException<InvalidInitFlowFile>()

    @Test
    fun T015_onlyCommands() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun T016_launchApp_emptyString() = expectException<SyntaxError> { e ->
        assertThat(e.message).contains("No mapping provided for YamlFluentCommand")
    }

    @Test
    fun launchApp_otherPackage(
        @YamlFile("017_launchApp_otherPackage.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
            )),
            LaunchAppCommand(
                appId = "com.other.app"
            ),
        )
    }

    @Test
    fun backPress_string(
        @YamlFile("018_backPress_string.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
            )),
            BackPressCommand(),
        )
    }

    @Test
    fun scroll_string(
        @YamlFile("019_scroll_string.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
            )),
            ScrollCommand(),
        )
    }

    @Test
    fun config_name(
        @YamlFile("020_config_name.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(MaestroConfig(
                appId = "com.example.app",
                name = "Example Flow"
            )),
            LaunchAppCommand(
                appId = "com.example.app"
            ),
        )
    }

    // Misc. tests

    @Test
    fun readFromZip() {
        val resource = this::class.java.getResource("/YamlCommandReaderTest/flow.zip")!!.toURI()
        assertThat(resource.scheme).isEqualTo("file")

        val commands = FileSystems.newFileSystem(Paths.get(resource), null).use { fs ->
            YamlCommandReader.readCommands(fs.getPath("flow.yaml"))
        }

        assertThat(commands).isEqualTo(commands(
            ApplyConfigurationCommand(
                config = MaestroConfig(
                    appId = "com.example.app",
                    initFlow = MaestroInitFlow(
                        appId = "com.example.app",
                        commands = commands(
                            ApplyConfigurationCommand(
                                config = MaestroConfig(
                                    appId = "com.example.app",
                                )
                            ),
                            LaunchAppCommand(
                                appId = "com.example.app"
                            ),
                        )
                    )
                )
            ),
            LaunchAppCommand(
                appId = "com.example.app"
            )
        ))
    }

    private fun commands(vararg commands: Command): List<MaestroCommand> {
        return commands.map(::MaestroCommand).toList()
    }

    private inline fun <reified T : Throwable> expectException(block: (e: T) -> Unit = {}) {
        try {
            parseCommands()
            assertWithMessage("Expected exception: ${T::class.java}").fail()
        } catch (e: Throwable) {
            if (e is AssertionError) throw e
            assertThat(e).isInstanceOf(T::class.java)
            block(e as T)
        }
    }

    private fun parseCommands(): List<MaestroCommand> {
        val resourceName = name.methodName.removePrefix("T") + ".yaml"
        val resource = this::class.java.getResource("/YamlCommandReaderTest/$resourceName")!!
        val resourceFile = Paths.get(resource.toURI())
        return YamlCommandReader.readCommands(resourceFile)
    }
}
