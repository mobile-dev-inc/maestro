package conductor.cli.command

import conductor.Conductor
import conductor.ConductorException
import conductor.cli.continuous.ContinuousTestRunner
import conductor.cli.util.ConductorFactory
import conductor.orchestra.CommandReader
import conductor.orchestra.ConductorCommand
import conductor.orchestra.Orchestra
import conductor.orchestra.yaml.YamlCommandReader
import okio.source
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "test",
)
class TestCommand : Callable<Int> {

    @CommandLine.Parameters
    private lateinit var testFile: File

    @Option(names = ["-t", "--target"])
    private var target: String? = null

    @Option(names = ["-c", "--continuous"])
    private var continuous: Boolean = false

    @CommandLine.Spec
    lateinit var commandSpec: CommandLine.Model.CommandSpec

    override fun call(): Int {
        if (target !in setOf("android", "ios", null)) {
            throw CommandLine.ParameterException(
                commandSpec.commandLine(),
                "Target must be one of: android, ios"
            )
        }

        if (!testFile.exists()) {
            throw CommandLine.ParameterException(
                commandSpec.commandLine(),
                "File not found: $testFile"
            )
        }

        val conductor = ConductorFactory.createConductor(target)

        return if (continuous) {
            ContinuousTestRunner(conductor, testFile).run()
            0
        } else {
            runSingleTest(conductor)
        }
    }

    private fun runSingleTest(conductor: Conductor): Int {
        val commands = readCommands(testFile)

        conductor.use {
            println("Running test on ${it.deviceName()}")

            try {
                Orchestra(it).executeCommands(commands)
            } catch (e: ConductorException) {
                println(e.message)

                println("FAILURE")
                return 1
            }
        }

        println("SUCCESS")

        return 0
    }

    private fun readCommands(file: File): List<ConductorCommand> {
        val reader = resolveCommandReader(file)

        return file.source().use {
            reader.readCommands(file.source())
        }
    }

    private fun resolveCommandReader(file: File): CommandReader {
        if (file.extension == "yaml") {
            return YamlCommandReader()
        }

        throw CommandLine.ParameterException(
            commandSpec.commandLine(),
            "Test file extension is not supported: ${file.extension}"
        )
    }
}
