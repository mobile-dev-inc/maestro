package maestro.cli.command.network

import maestro.cli.App
import picocli.CommandLine

@CommandLine.Command(
    name = "network",
    subcommands = [
        ObserveNetworkCommand::class,
        RecordNetworkCommand::class,
        ReplayNetworkCommand::class,
        SetupForProxyCommand::class,
    ]
)
class NetworkCommand {

    @CommandLine.ParentCommand
    lateinit var app: App

}