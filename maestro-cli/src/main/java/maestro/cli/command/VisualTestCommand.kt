package maestro.cli.command

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import maestro.cli.App
import maestro.cli.DisableAnsiMixin
import maestro.cli.session.MaestroSessionManager
import maestro.cli.view.yellow
import maestro.utils.Insights
import maestro.utils.chunkStringByWordCount
import picocli.CommandLine
import java.lang.StringBuilder
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "visual-test",
    description = [
        "Helps you surfacing regressions with truncated texts"
    ],
    hidden = false
)
class VisualTestCommand : Callable<Int>  {

    @CommandLine.Mixin
    var disableANSIMixin: DisableAnsiMixin? = null

    @CommandLine.ParentCommand
    private val parent: App? = null

    override fun call(): Int {
        MaestroSessionManager.newSession(parent?.host, parent?.port, parent?.deviceId) { session ->
            Insights.onInsightsUpdated {
                val message = StringBuilder()
                val level = it.level.toString().lowercase().replaceFirstChar(Char::uppercase)
                message.append(level.yellow() + ": ")
                it.message.chunkStringByWordCount(12).forEach { chunkedMessage ->
                    message.append("$chunkedMessage ")
                }
                println(message.toString())
            }
            val treeNode = session.maestro.viewHierarchy().root

        }
    }

    fun getTextFromImage() {

    }
}