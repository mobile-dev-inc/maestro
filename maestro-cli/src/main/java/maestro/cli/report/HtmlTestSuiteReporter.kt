package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import okio.Sink

import okio.buffer

class HtmlTestSuiteReporter : TestSuiteReporter {
    override fun report(summary: TestExecutionSummary, out: Sink) {
        val bufferedOut = out.buffer()
        val htmlContent = buildHtmlReport(summary)
        bufferedOut.writeUtf8(htmlContent)
        bufferedOut.close()
    }

    private fun buildHtmlReport(summary: TestExecutionSummary): String {
    val htmlBuilder = StringBuilder()
    
    htmlBuilder.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Maestro Test Report</title><link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\"></head><body>")
    
    htmlBuilder.append("<div class=\"container py-5\"><div class=\"card mb-4\"><br><center><h1>Test Execution Summary</h1></center>")
    htmlBuilder.append("<div class=\"card-body\">Device Name: ${summary.deviceName}<br>Test Result Summary: ${if (summary.passed) "PASSED" else "FAILED"}<br>Total Suite: ${summary.suites.size}<br>Total Test: ${summary.suites.sumOf { it.flows.size }}</div></div>")

    for (suite in summary.suites) {
        htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\"><center><b>Test Suite</b></center><br>Summary: ${if (suite.passed) "PASSED" else "FAILED"}<br>Duration: ${suite.duration}<br>Test Count: ${suite.flows.size}<br><br>")
        for (flow in suite.flows) {
            htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\">")
            htmlBuilder.append("<h5 class=\"card-title\">${flow.name}</h5>")
            htmlBuilder.append("<p class=\"card-text\">Status: ${flow.status}<br>Duration: ${flow.duration}<br>File Name: ${flow.fileName}</p>")
            if (flow.failure != null) {
                htmlBuilder.append("<p class=\"card-text text-danger\">${flow.failure.message}</p>")
                htmlBuilder.append("<a href=\"https://jenkins.evermosa2z.com/job/qa-ec2-node-playground\" class=\"btn btn-danger\" target=\"_blank\">More Info</a>")
            }
            htmlBuilder.append("</div></div>")
        }
    }
    htmlBuilder.append("</body></html>")
    return htmlBuilder.toString()
    }
}
