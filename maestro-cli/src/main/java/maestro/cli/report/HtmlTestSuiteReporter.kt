package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import okio.buffer
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import maestro.cli.runner.CommandStatus
import maestro.orchestra.MaestroCommand
import okio.sink
import java.io.File
import java.net.URLEncoder

class HtmlTestSuiteReporter(override val fileExtension: String?) : TestSuiteReporter {
  override fun report(summary: TestExecutionSummary, out: File) {
    val bufferedOut = out.sink().buffer()
    val htmlContent = buildHtmlReport(summary, out.parentFile)
    bufferedOut.writeUtf8(htmlContent)
    bufferedOut.close()
  }

  private fun getFailedTest(summary: TestExecutionSummary): Array<String>{
    var failedTest = emptyArray<String>()
    for (suite in summary.suites) {
        for(flow in suite.flows){
            if(flow.status.toString() == "ERROR"){
                failedTest += flow.name
            }
        }
    }
    return failedTest
  }

  private fun buildHtmlReport(summary: TestExecutionSummary, reportFolder: File): String {
    val failedTest = getFailedTest(summary)
    return buildString {
      appendHTML().html {
        head {
          title { +"Maestro Test Report" }
          link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css") {}
        }
        body {
          summary.suites.forEach { suite ->
            div(classes = "card mb-4") {
              div(classes = "card-body") {
                h1(classes = "mt-5 text-center") { +"Flow Execution Summary" }
                br{}
                +"Test Result: ${if (suite.passed) "PASSED" else "FAILED"}"
                br{}
                +"Duration: ${suite.duration}"
                br{}
                br{}
                div(classes = "card-group mb-4") {
                  div(classes = "card") {
                    div(classes = "card-body") {
                      h5(classes = "card-title text-center") { +"Total number of Flows" }
                      h3(classes = "card-text text-center") { +"${suite.flows.size}" }
                    }
                  }
                  div(classes = "card text-white bg-danger") {
                    div(classes = "card-body") {
                      h5(classes = "card-title text-center") { +"Failed Flows" }
                      h3(classes = "card-text text-center") { +"${failedTest.size}" }
                    }
                  }
                  div(classes = "card text-white bg-success") {
                    div(classes = "card-body") {
                      h5(classes = "card-title text-center") { +"Successful Flows" }
                      h3(classes = "card-text text-center") { +"${suite.flows.size - failedTest.size}" }
                    }
                  }
                }
                if(failedTest.size != 0){
                  div(classes = "card border-danger mb-3") {
                    div(classes = "card-body text-danger") {
                      b { +"Failed Flow" }
                      br{}
                      p(classes = "card-text") {
                        failedTest.forEach { test ->
                          +test
                          br{}
                        }
                      }
                    }
                  }
                }
                suite.flows.forEach { flow ->
                  val buttonClass = if (flow.status.toString() == "ERROR") "btn btn-danger" else "btn btn-success"
                  div(classes = "card mb-4") {
                    div(classes = "card-header") {
                      h5(classes = "mb-0") {
                        button(classes = buttonClass) {
                          attributes["type"] = "button"
                          attributes["data-bs-toggle"] = "collapse"
                          attributes["data-bs-target"] = "#${flow.name}"
                          attributes["aria-expanded"] = "false"
                          attributes["aria-controls"] = flow.name
                          +"${flow.name} : ${flow.status}"
                        }
                      }
                    }
                    div(classes = "collapse") {
                      id = flow.name
                      div(classes = "card-body") {
                        p(classes = "card-text") {
                          +"Status: ${flow.status}"
                          br{}
                          +"Duration: ${flow.duration}"
                          br{}
                          +"File Name: ${flow.fileName}"
                        }
                        if(flow.failure != null) {
                          p(classes = "card-text text-danger"){
                            +flow.failure.message
                          }
                          failureDetailsFormatted(flow.failure, flow, reportFolder)
                        }
                      }
                    }
                  }
                }
              }
              script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js", content = "")
            }
          }
        }
      }
    }
  }

    private fun DIV.failureDetailsFormatted(
        failure: TestExecutionSummary.Failure,
        flow: TestExecutionSummary.FlowResult,
        reportFolder: File
    ) {
        br {}
        p(classes = "card-text") {
            table(classes = "table table-bordered") {
                thead {
                    tr {
                        td { +"Command Type" }
                        td { +"Description" }
                        td { +"Status" }
                        td { +"Error message" }
                        td { +"Duration" }
                        td { +"Stack Trace" }
                    }
                }
                tbody {
                    val sortedCommands = failure.commands?.toList()?.sortedBy { it.second.timestamp }

                    sortedCommands?.forEach {
                        val command: MaestroCommand = it.first
                        val metadata: CommandDebugMetadata = it.second
                        val status: CommandStatus? = metadata.status
                        val rowClass: String = if (status == null) {
                            ""
                        } else {
                            when (status) {
                                CommandStatus.COMPLETED -> "table-success"
                                CommandStatus.FAILED -> "table-danger"
                                CommandStatus.PENDING -> "table-light"
                                CommandStatus.RUNNING -> "table-info"
                                CommandStatus.SKIPPED -> "table-secondary"
                            }
                        }
                        tr(classes = "$rowClass") {
                            td { +command.asCommand()?.javaClass?.simpleName.toString().replace(commandRegex, "") }
                            td(classes = "strong") { +command.description() }
                            td { +"${status?.name}" }
                            td { +metadata.error?.message.orEmpty() }
                            td { +"${metadata.duration ?: ""}" }
                            td { pre { +metadata.error?.stackTraceToString().orEmpty() } }
                        }
                    }
                }
            }
        }
        p {
            flow.failure?.screenshots?.forEach {
                val sanitisedFlowName = URLEncoder.encode(flow.name.replace(" ", "_"), "utf-8")
                val screenshotCopyFileName = "${sanitisedFlowName}_${flow.status.name}_${it.timestamp}"
                val screenshotCopyFile = File(reportFolder, "${screenshotCopyFileName}${it.screenshot.extension}")
                it.screenshot.copyTo(screenshotCopyFile, overwrite = true)

                img {
                    src = screenshotCopyFile.name
                    alt = "Screenshot: ${screenshotCopyFileName}, ${flow.status.name}"
                    style = "display: block; width: 400px; height: auto;"
                }
            }
        }
    }

    companion object {
        private val commandRegex = Regex("Command$")
    }
}
