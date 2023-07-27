package maestro.orchestra.yaml

import com.google.common.truth.Truth.assertThat
import java.nio.file.FileSystems
import java.nio.file.Paths
import maestro.TapRepeat
import maestro.orchestra.ApplyConfigurationCommand
import maestro.orchestra.AssertConditionCommand
import maestro.orchestra.BackPressCommand
import maestro.orchestra.Command
import maestro.orchestra.Condition
import maestro.orchestra.ElementSelector
import maestro.orchestra.LaunchAppCommand
import maestro.orchestra.MaestroCommand
import maestro.orchestra.MaestroConfig
import maestro.orchestra.MaestroOnFlowComplete
import maestro.orchestra.MaestroOnFlowStart
import maestro.orchestra.ScrollCommand
import maestro.orchestra.SetLocationCommand
import maestro.orchestra.StopAppCommand
import maestro.orchestra.TakeScreenshotCommand
import maestro.orchestra.TapOnElementCommand
import maestro.orchestra.TapOnPointV2Command
import maestro.orchestra.WaitForAnimationToEndCommand
import maestro.orchestra.error.InvalidInitFlowFile
import maestro.orchestra.error.SyntaxError
import maestro.orchestra.yaml.junit.YamlCommandsExtension
import maestro.orchestra.yaml.junit.YamlExceptionExtension
import maestro.orchestra.yaml.junit.YamlFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Suppress("JUnitMalformedDeclaration")
@ExtendWith(YamlCommandsExtension::class, YamlExceptionExtension::class)
internal class YamlCommandReaderTest {

    @Test
    fun empty(
        @YamlFile("001_empty.yaml") e: SyntaxError,
    ) {
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
    fun config_empty(
        @YamlFile("004_config_empty.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun config_noAppId(
        @YamlFile("005_config_noAppId.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("appId due to missing (therefore NULL) value for creator parameter appId which is a non-nullable type")
    }

    @Test
    fun emptyCommands(
        @YamlFile("006_emptyCommands.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun initFlow(
        @YamlFile("007_initFlow.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).containsMatch("initFlow command used at.*is deprecated")
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
    fun invalidCommand(
        @YamlFile("009_invalidCommand.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Unrecognized field \"invalid\"")
    }

    @Test
    fun invalidCommand_string(
        @YamlFile("010_invalidCommand_string.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Invalid command: \"invalid\"")
    }

    @Test
    fun initFlow_file(
        @YamlFile("011_initFlow_file.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).containsMatch("initFlow command used at.*is deprecated")
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
    fun initFlow_invalidFile(
        @YamlFile("013_initFlow_invalidFile.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).containsMatch("initFlow command used at.*is deprecated")
    }

    @Test
    fun initFlow_recursive(
        @YamlFile("014_initFlow_recursive.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).containsMatch("initFlow command used at.*is deprecated")
    }

    @Test
    fun onlyCommands(
        @YamlFile("015_onlyCommands.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Flow files must contain a config section and a commands section")
    }

    @Test
    fun launchApp_emptyString(
        @YamlFile("016_launchApp_emptyString.yaml") e: SyntaxError,
    ) {
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
                    appId = "com.example.app"
                )
            ),
            LaunchAppCommand(
                appId = "com.example.app"
            )
        ))
    }

    @Test
    fun launchAppSyntaxError(
        @YamlFile("021_launchApp_syntaxError.yaml") e: SyntaxError,
    ) {
        assertThat(e.message).contains("Cannot deserialize value of type")
    }

    @Test
    fun onFlowStartCompleteHooks(
        @YamlFile("022_on_flow_start_complete.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(
                config = MaestroConfig(
                    appId = "com.example.app",
                    onFlowStart = MaestroOnFlowStart(
                        commands = commands(
                            BackPressCommand()
                        )
                    ),
                    onFlowComplete = MaestroOnFlowComplete(
                        commands = commands(
                            ScrollCommand()
                        )
                    )
                )
            ),
            LaunchAppCommand(
                appId = "com.example.app"
            )
        )
    }

    @Test
    fun labels(
        @YamlFile("023_labels.yaml") commands: List<Command>,
    ) {
        assertThat(commands).containsExactly(
            ApplyConfigurationCommand(
                config=MaestroConfig(
                    appId="com.example.app"
                )
            ),
            TapOnElementCommand(
                selector = ElementSelector(idRegex = "foo"),
                retryIfNoChange = true,
                waitUntilVisible = false,
                longPress = false,
                label = "Tap on the important button"
            ),
            TapOnElementCommand(
                selector = ElementSelector(idRegex = "foo"),
                retryIfNoChange = true,
                waitUntilVisible = false,
                longPress = false,
                repeat = TapRepeat(
                    repeat = 2,
                    delay = 100L
                ),
                label = "Tap on the important button twice more"
            ),
            TapOnElementCommand(
                selector = ElementSelector(idRegex = "foo"),
                retryIfNoChange = true,
                waitUntilVisible = false,
                longPress = true,
                label = "Press and hold the important button"
            ),
            TapOnPointV2Command(
                point = "50%,50%",
                retryIfNoChange = true,
                longPress = false,
                label = "Tap on the middle of the screen"
            ),
            AssertConditionCommand(
                condition = Condition(visible = ElementSelector(idRegex = "bar")),
                label = "Check that the important number is visible"
            ),
            AssertConditionCommand(
                condition = Condition(notVisible = ElementSelector(idRegex = "bar2")),
                label = "Check that the secret number is invisible"
            ),
            TakeScreenshotCommand(
                path = "baz",
                label = "Snap this for later evaluation"
            ),
            StopAppCommand(
                appId = "com.some.other",
                label = "Stop that other app from running"
            ),
            SetLocationCommand(
                latitude = 12.5266,
                longitude = 78.2150,
                label = "Set Location to Test Laboratory"
            ),
            WaitForAnimationToEndCommand(
                timeout = 4000,
                label = "Wait for the thing to stop spinning"
            )
        )
    }

    private fun commands(vararg commands: Command): List<MaestroCommand> =
        commands.map(::MaestroCommand).toList()
}
